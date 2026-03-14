package it.lmqv.livematchcam.utils

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object Mp4Merger {

    /**
     * Merges multiple MP4 files into one.
     * @param inputFiles List of files to merge.
     * @param outputFile The resulting merged file.
     * @param normalizeToZero If true, the first sample's timestamp will be treated as 0.
     *                        If false, original timestamps are kept (but still made monotonic if needed).
     */
    fun merge(inputFiles: List<File>, outputFile: File, normalizeToZero: Boolean = false): Boolean {
        if (inputFiles.isEmpty()) return false
        
        Logd("Mp4Merger :: merging ${inputFiles.size} files into ${outputFile.name} (normalize=$normalizeToZero)")

        var muxer: MediaMuxer? = null
        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            //val trackMap = mutableMapOf<Int, Int>() // Maps input track index to muxer track index

            // 1. Single pass for format discovery - use the first valid file
            val firstFile = inputFiles.firstOrNull { it.exists() && it.length() > 0 }
            if (firstFile == null) {
                Loge("Mp4Merger :: No valid input files found")
                return false
            }

            val headExtractor = MediaExtractor()
            headExtractor.setDataSource(firstFile.absolutePath)
            for (i in 0 until headExtractor.trackCount) {
                val format = headExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = muxer.addTrack(format)
                } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = muxer.addTrack(format)
                }
            }
            headExtractor.release()

            Logd("Mp4Merger :: headExtractor done")

            if (videoTrackIndex == -1 && audioTrackIndex == -1) {
                Loge("Mp4Merger :: No valid tracks found in first file")
                return false
            }

            Logd("Mp4Merger :: muxer start")

            muxer.start()

            // 2. Process all files in a single pass
            val buffer = ByteBuffer.allocateDirect(2 * 1024 * 1024) // Direct buffer for faster native I/O
            val info = MediaCodec.BufferInfo()
            
            var globalStartPtsUs = -1L
            var lastVideoOutputPtsUs = -1L
            var lastAudioOutputPtsUs = -1L

            for (file in inputFiles) {
                if (!file.exists() || file.length() == 0L) continue

                val extractor = MediaExtractor()
                extractor.setDataSource(file.absolutePath)

                var fileVideoTrackIndex = -1
                var fileAudioTrackIndex = -1

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/")) {
                        fileVideoTrackIndex = i
                        extractor.selectTrack(i)
                    } else if (mime.startsWith("audio/")) {
                        fileAudioTrackIndex = i
                        extractor.selectTrack(i)
                    }
                }

                // Calculate base offsets for this file to ensure monotonicity
                var fileVideoOffsetUs = 0L
                var fileAudioOffsetUs = 0L
                var foundFirstVideoInFile = false
                var foundFirstAudioInFile = false

                while (true) {
                    val trackIndex = extractor.sampleTrackIndex
                    if (trackIndex < 0) break

                    val sampleTime = extractor.sampleTime
                    
                    // Determine which output track to use
                    val targetMuxerTrackIndex = if (trackIndex == fileVideoTrackIndex) videoTrackIndex 
                                               else if (trackIndex == fileAudioTrackIndex) audioTrackIndex 
                                               else -1

                    if (targetMuxerTrackIndex != -1) {
                        val baseTime = if (normalizeToZero) sampleTime - globalStartPtsUs else sampleTime

                        // Initialize global start PTS from the very first sample ever seen
                        if (normalizeToZero && globalStartPtsUs == -1L) {
                            globalStartPtsUs = sampleTime
                        }

                        // Calculate offset for this file's tracks independently
                        if (trackIndex == fileVideoTrackIndex) {
                            if (!foundFirstVideoInFile) {
                                if (lastVideoOutputPtsUs != -1L && baseTime <= lastVideoOutputPtsUs) {
                                    fileVideoOffsetUs = (lastVideoOutputPtsUs - baseTime) + 1000L
                                }
                                foundFirstVideoInFile = true
                            }
                        } else {
                            if (!foundFirstAudioInFile) {
                                if (lastAudioOutputPtsUs != -1L && baseTime <= lastAudioOutputPtsUs) {
                                    fileAudioOffsetUs = (lastAudioOutputPtsUs - baseTime) + 1000L
                                }
                                foundFirstAudioInFile = true
                            }
                        }

                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) break

                        info.offset = 0
                        info.size = sampleSize
                        @Suppress("WrongConstant")
                        info.flags = extractor.sampleFlags
                        
                        var outputPtsUs = baseTime + (if (trackIndex == fileVideoTrackIndex) fileVideoOffsetUs else fileAudioOffsetUs)
                        
                        if (trackIndex == fileVideoTrackIndex) {
                            lastVideoOutputPtsUs = outputPtsUs
                        } else {
                            lastAudioOutputPtsUs = outputPtsUs
                        }
                        
                        info.presentationTimeUs = outputPtsUs
                        muxer.writeSampleData(targetMuxerTrackIndex, buffer, info)
                    }

                    extractor.advance()
                }
                extractor.release()
            }

            muxer.stop()
            Logd("Mp4Merger :: muxer stop")

            muxer.release()
            Logd("Mp4Merger :: Merge completed successfully")
            return true

        } catch (e: Exception) {
            Loge("Mp4Merger :: Exception during merge: ${e.message}")
            e.printStackTrace()
            try { muxer?.release() } catch (ignored: Exception) {}
            if (outputFile.exists()) outputFile.delete()
            return false
        }
    }

    /**
     * Super-optimized merge that pre-loads all samples into RAM in parallel
     * before writing them to the muxer.
     */
    suspend fun mergeParallel(inputFiles: List<File>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        if (inputFiles.isEmpty()) return@withContext false
        
        Logd("Mp4Merger :: mergeParallel starting for ${inputFiles.size} files")
        val startTime = System.currentTimeMillis()

        // 1. Discovery (on the first file)
        val firstFile = inputFiles.firstOrNull { it.exists() && it.length() > 0 } ?: return@withContext false
        var videoFormat: MediaFormat? = null
        var audioFormat: MediaFormat? = null

        MediaExtractor().use { extractor ->
            extractor.setDataSource(firstFile.absolutePath)
            videoFormat = (0 until extractor.trackCount)
                .map { extractor.getTrackFormat(it) }
                .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
            audioFormat = (0 until extractor.trackCount)
                .map { extractor.getTrackFormat(it) }
                .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
        }

        if (videoFormat == null && audioFormat == null) return@withContext false

        // 2. Parallel Extraction into Memory
        val preloadedFiles = inputFiles.filter { it.exists() && it.length() > 0 }.map { file ->
            async {
                Logd("Mp4Merger :: extraction into memory for ${file.absolutePath}")
                val samples = mutableListOf<PreloadedSample>()
                MediaExtractor().use { extractor ->
                    extractor.setDataSource(file.absolutePath)
                    
                    val fileVideoIdx = (0 until extractor.trackCount).firstOrNull { 
                        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true 
                    }?.also { extractor.selectTrack(it) } ?: -1
                    
                    val fileAudioIdx = (0 until extractor.trackCount).firstOrNull { 
                        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true 
                    }?.also { extractor.selectTrack(it) } ?: -1

                    val buffer = ByteBuffer.allocateDirect(1024 * 1024)
                    while (true) {
                        val trackIndex = extractor.sampleTrackIndex
                        if (trackIndex < 0) break
                        
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) break
                        
                        val sampleData = ByteBuffer.allocateDirect(size)
                        buffer.limit(size)
                        buffer.position(0)
                        sampleData.put(buffer)
                        
                        val info = MediaCodec.BufferInfo().apply {
                            this.offset = 0
                            this.size = size
                            this.presentationTimeUs = extractor.sampleTime
                            @Suppress("WrongConstant")
                            this.flags = extractor.sampleFlags
                        }
                        
                        val isVideo = trackIndex == fileVideoIdx
                        samples.add(PreloadedSample(isVideo, info, sampleData))
                        
                        extractor.advance()
                    }
                }
                samples
            }
        }.awaitAll()

        Logd("Mp4Merger :: Parallel extraction done in ${System.currentTimeMillis() - startTime}ms")

        // 3. Sequential Muxing
        var muxer: MediaMuxer? = null
        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val videoTrackIdx = videoFormat?.let { muxer.addTrack(it) } ?: -1
            val audioTrackIdx = audioFormat?.let { muxer.addTrack(it) } ?: -1

            Logd("Mp4Merger :: muxer.start()")
            muxer.start()

            var globalStartPtsUs = -1L
            var lastVideoPtsUs = -1L
            var lastAudioPtsUs = -1L

            for (samples in preloadedFiles) {
                var videoOffset = 0L
                var audioOffset = 0L
                var foundFirstVideo = false
                var foundFirstAudio = false

                for (sample in samples) {
                    if (globalStartPtsUs == -1L) {
                        globalStartPtsUs = sample.info.presentationTimeUs
                    }

                    val baseTime = sample.info.presentationTimeUs - globalStartPtsUs
                    
                    if (sample.isVideo) {
                        if (!foundFirstVideo) {
                            if (lastVideoPtsUs != -1L && baseTime <= lastVideoPtsUs) {
                                videoOffset = (lastVideoPtsUs - baseTime) + 1000L
                            }
                            foundFirstVideo = true
                        }
                    } else {
                        if (!foundFirstAudio) {
                            if (lastAudioPtsUs != -1L && baseTime <= lastAudioPtsUs) {
                                audioOffset = (lastAudioPtsUs - baseTime) + 1000L
                            }
                            foundFirstAudio = true
                        }
                    }

                    val targetIdx = if (sample.isVideo) videoTrackIdx else audioTrackIdx
                    if (targetIdx != -1) {
                        var pts = baseTime + (if (sample.isVideo) videoOffset else audioOffset)
                        
                        sample.info.presentationTimeUs = pts
                        muxer.writeSampleData(targetIdx, sample.data, sample.info)
                        
                        if (sample.isVideo) lastVideoPtsUs = pts else lastAudioPtsUs = pts
                    }
                }
            }

            muxer.stop()
            Logd("Mp4Merger :: muxer.stop()")
            Logd("Mp4Merger :: mergeParallel total time: ${System.currentTimeMillis() - startTime}ms")
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("Mp4Merger :: mergeParallel error: ${e.message}")
            return@withContext false
        } finally {
            try { muxer?.release() } catch (ignored: Exception) {}
        }
    }

    private class PreloadedSample(val isVideo: Boolean, val info: MediaCodec.BufferInfo, val data: ByteBuffer)

    private inline fun <T : MediaExtractor?, R> T.use(block: (T) -> R): R {
        try {
            return block(this)
        } finally {
            this?.release()
        }
    }

    fun extract(inputFile: File, outputFile: File, startUs: Long, endUs: Long): Boolean {
        if (!inputFile.exists() || inputFile.length() == 0L) return false
        Logd("Mp4Merger :: extracting from ${inputFile.name} [${startUs/1000}ms - ${endUs/1000}ms] into ${outputFile.name}")

        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            val trackCount = extractor.trackCount
            val trackMap = mutableMapOf<Int, Int>()
            
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    videoTrackIndex = muxer.addTrack(format)
                    trackMap[i] = videoTrackIndex
                    extractor.selectTrack(i)
                } else if (mime.startsWith("audio/")) {
                    audioTrackIndex = muxer.addTrack(format)
                    trackMap[i] = audioTrackIndex
                    extractor.selectTrack(i)
                }
            }

            if (videoTrackIndex == -1 && audioTrackIndex == -1) {
                Loge("Mp4Merger :: extract :: No valid tracks found")
                extractor.release()
                return false
            }

            muxer.start()

            // Seek to the starting point. Using SEEK_TO_PREVIOUS_SYNC to ensure we start with an I-frame.
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            
            var actualStartUs = -1L

            val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (actualStartUs == -1L) {
                    actualStartUs = sampleTime
                }

                val targetTrackIndex = trackMap[trackIndex]
                if (targetTrackIndex != null) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    info.offset = 0
                    info.size = sampleSize
                    @Suppress("WrongConstant")
                    info.flags = extractor.sampleFlags
                    info.presentationTimeUs = sampleTime - actualStartUs // Normalize to 0

                    muxer.writeSampleData(targetTrackIndex, buffer, info)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            Logd("Mp4Merger :: Extraction completed successfully")
            return true

        } catch (e: Exception) {
            Loge("Mp4Merger :: Exception during extraction: ${e.message}")
            e.printStackTrace()
            outputFile.delete()
            return false
        }
    }
}
