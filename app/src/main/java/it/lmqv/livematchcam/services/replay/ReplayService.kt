package it.lmqv.livematchcam.services.replay

import android.media.MediaMetadataRetriever
import android.content.Context
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.utils.Mp4Merger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReplayService(
    private val context: Context,
    private val bufferDurationSeconds: Int = 30
) {
    private val _replayState = MutableStateFlow(ReplayState.IDLE)
    val replayState: StateFlow<ReplayState> = _replayState.asStateFlow()

    private val _replayMetadata = MutableStateFlow<ReplayMetadata?>(null)
    val replayMetadata: StateFlow<ReplayMetadata?> = _replayMetadata.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var rollingJob: Job? = null
    private val mutex = Mutex()

    // Directory where replay video segments are stored
    private val replayDir by lazy {
        File(context.cacheDir, "replays").apply {
            if (!exists()) mkdirs()
        }
    }

    private var currentFile: File? = null
    private var recordingStartTime: Long = 0

    // Dependencies to call start/stop record on the StreamService
    var startRecordAction: ((String) -> Unit)? = null
    var stopRecordAction: (() -> Unit)? = null

    private val stopSignalChannel = Channel<Unit>(Channel.CONFLATED)

    fun onRecordStopped() {
        Logd("ReplayService :: onRecordStopped signal received")
        stopSignalChannel.trySend(Unit)
    }

    private suspend fun waitForStopSignal() {
        withTimeoutOrNull(1000) {
            stopSignalChannel.receive()
        }
    }

    private val chunkDurationMs = 20000L // 20s chunks for better granularity and performance

    init {
        Logd("ReplayService :: initialize cleanup")
        cleanup()
    }

    fun startRollingRecording() {
        if (_replayState.value != ReplayState.IDLE) return

        Logd("ReplayService :: startRollingRecording")
        scope.launch { clearOldFiles() }
        _replayState.value = ReplayState.RECORDING

        // Start the first segment
        startNewSegment()

        // Loop to periodically restart the recording to create segments
        rollingJob = scope.launch {
            while (isActive) {
                val timeSinceStart = System.currentTimeMillis() - recordingStartTime
                val nextDelay = chunkDurationMs - timeSinceStart

                if (nextDelay > 50) {
                    delay(nextDelay)
                    continue
                }

                val state = _replayState.value
                if (state == ReplayState.RECORDING || state == ReplayState.SEEKING) {
                    mutex.withLock {
                        // Double check if we still need to rotate (recordingStartTime might have changed)
                        if (System.currentTimeMillis() - recordingStartTime >= chunkDurationMs - 100) {
                            Logd("ReplayService :: rollingJob rotating segment")
                            stopRecordAction?.invoke()
                            waitForStopSignal() // Dynamic wait instead of fixed delay
                            if (isActive && (_replayState.value == ReplayState.RECORDING || _replayState.value == ReplayState.SEEKING)) {
                                performStartNewSegment()
                                performClearOldFiles()
                            }
                        }
                    }
                } else {
                    delay(1000)
                }
            }
        }
    }

    fun stopRollingRecording() {
        Logd("ReplayService :: stopRollingRecording")
        rollingJob?.cancel()
        if (_replayState.value == ReplayState.RECORDING) {
            stopRecordAction?.invoke()
        }
        _replayState.value = ReplayState.IDLE
        _replayMetadata.value = null
        currentFile = null
    }

    private fun performStartNewSegment() {
        val fileName = "replay_chunk_${System.currentTimeMillis()}.mp4"
        currentFile = File(replayDir, fileName)
        recordingStartTime = System.currentTimeMillis()
        Logd("ReplayService :: startNewSegment ${currentFile?.absolutePath}")

        startRecordAction?.invoke(currentFile!!.absolutePath)
    }

    private fun startNewSegment() = scope.launch {
        mutex.withLock {
            performStartNewSegment()
        }
    }

    private fun performClearOldFiles() {
        // Skip cleanup while replay is being selected or playing to avoid deleting files in use
        if (_replayState.value == ReplayState.SEEKING || _replayState.value == ReplayState.REPLAYING) {
            return 
        }

        val chunksCount = (bufferDurationSeconds / (chunkDurationMs.toInt() / 1000)) + 1
        val files = replayDir.listFiles()?.filter { it.isFile && it.name.startsWith("replay_chunk_") }
            ?.sortedByDescending { it.lastModified() } ?: return

        // We keep 'chunksCount' files + the 'currentFile' being written
        if (files.size > chunksCount) {
            val filesToDelete = files.drop(chunksCount)
            for (file in filesToDelete) {
                if (file.absolutePath != currentFile?.absolutePath) {
                    Logd("ReplayService :: deleting old chunk ${file.name}")
                    file.delete()
                }
            }
        }
    }

    private suspend fun clearOldFiles() = mutex.withLock {
        performClearOldFiles()
    }

    suspend fun prepareReplay(): Boolean = withContext(Dispatchers.IO) {
        if (_replayState.value != ReplayState.RECORDING) return@withContext false
        Logd("ReplayService :: prepareReplay - merging including current chunk")

        mutex.withLock {
            // 1. Force close the current file to include it in the replay
            stopRecordAction?.invoke()
            waitForStopSignal() // Dynamic wait instead of fixed delay

            // 2. Start a new one immediately to keep the buffer rolling
            if (_replayState.value == ReplayState.RECORDING) {
                performStartNewSegment()
            }

            // 3. Collect only the LATEST chunks that fit the buffer duration
            val maxChunksToMerge = (bufferDurationSeconds / (chunkDurationMs.toInt() / 1000)) + 1
            val allRecordings = replayDir.listFiles()?.filter {
                it.isFile && it.name.startsWith("replay_chunk_") && it.absolutePath != currentFile?.absolutePath
            }?.sortedByDescending { it.lastModified() } ?: emptyList()

            val filesToMerge = allRecordings.take(maxChunksToMerge).reversed() // Re-sort chronologically

            if (filesToMerge.isEmpty()) {
                Logd("ReplayService :: replay preparation failed, no valid chunks")
                _replayState.value = ReplayState.IDLE
                return@withContext false
            }

            Logd("ReplayService :: merging ${filesToMerge.size} files for buffer")
            val mergedFile = File(replayDir, "replay_merged.mp4")
            if (mergedFile.exists()) mergedFile.delete()

            val success = Mp4Merger.mergeParallel(filesToMerge, mergedFile)

            if (success && mergedFile.exists()) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(mergedFile.absolutePath)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val totalDurationMs = durationStr?.toLongOrNull() ?: 0L

                    val reqBufferMs = bufferDurationSeconds * 1000L
                    val fileOffsetMs = if (totalDurationMs > reqBufferMs) (totalDurationMs - reqBufferMs) else 0L
                    val usableDurationMs = if (totalDurationMs > reqBufferMs) reqBufferMs else totalDurationMs

                    // Ensure at least 5s duration
                    if (usableDurationMs >= 5000L) {
                        Logd("ReplayService :: merged success, total: $totalDurationMs, offset: $fileOffsetMs, usable: $usableDurationMs")

                        _replayMetadata.value = ReplayMetadata(mergedFile.absolutePath, usableDurationMs, fileOffsetMs)
                        _replayState.value = ReplayState.SEEKING

                        return@withContext true
                    } else {
                        return@withContext false
                    }
                } catch (e: Exception) {
                    Loge("ReplayService :: fail to extract metadata: ${e.message}")
                } finally {
                    retriever.release()
                }
            }
        }

        Logd("ReplayService :: replay preparation failed during merge")
        _replayState.value = ReplayState.IDLE
        return@withContext false
    }

    fun startReplay() {
        if (_replayState.value != ReplayState.SEEKING) return
        Logd("ReplayService :: startReplay")
        _replayState.value = ReplayState.REPLAYING
        // Stop live recording from camera since the replay is about to start
        stopRecordAction?.invoke()
    }

//    fun cancelReplay() {
//        Logd("ReplayService :: cancelReplay")
//        if (_replayState.value == ReplayState.SEEKING) {
//            _replayState.value = ReplayState.RECORDING
//            _replayMetadata.value = null
//            // No need to start new segment, rolling job is still active
//        }
//    }

    fun stopReplay() {
        Logd("ReplayService :: stopReplay")
        if (_replayState.value == ReplayState.REPLAYING) {
            _replayState.value = ReplayState.RECORDING
            _replayMetadata.value = null
            // Resume recording chunks with a delay to let audio/video sources stabilize and generate valid timestamps
            scope.launch {
                delay(500)
                if (_replayState.value == ReplayState.RECORDING) {
                    startNewSegment()
                }
            }
        } else if (_replayState.value == ReplayState.SEEKING) {
            _replayState.value = ReplayState.RECORDING
            _replayMetadata.value = null
        }
        
        // Always attempt a cleanup after stopping or cancelling a replay
        scope.launch { clearOldFiles() }
    }

    private fun cleanup() {
        val files = replayDir.listFiles() ?: return
        for (file in files) {
            if (file.isFile && (file.name.startsWith("replay_chunk_") || file.name == "replay_merged.mp4")) {
                file.delete()
            }
        }
    }
}
