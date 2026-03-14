package it.lmqv.livematchcam.fragments.replay

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import it.lmqv.livematchcam.StreamActivity
import it.lmqv.livematchcam.databinding.FragmentReplaySeekingBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.launchOnResumed
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.services.replay.ReplayMetadata
import it.lmqv.livematchcam.preferences.ReplayPreferencesManager
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class ReplaySeekingFragment : Fragment() {

    private var _binding: FragmentReplaySeekingBinding? = null
    private val binding get() = _binding!!

    private var previewMediaPlayer: MediaPlayer? = null
    private var previewSurface: Surface? = null
    private var currentMetadata: ReplayMetadata? = null
    private lateinit var replayPrefs: ReplayPreferencesManager

    private var minUsableDurationMs: Long = 5000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReplaySeekingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val streamActivity = requireActivity() as StreamActivity
        val streamService = streamActivity.streamService
        replayPrefs = ReplayPreferencesManager(requireContext())

        binding.replayThumbnail.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                previewSurface = Surface(surfaceTexture)
                previewMediaPlayer?.setSurface(previewSurface)
            }
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                previewSurface?.release()
                previewSurface = null
                return true
            }
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }

        binding.replayCancelButton.setOnClickListener {
            streamService.cancelReplay()
        }

        binding.replayPlayButton.setOnClickListener {
            val progress = binding.replaySeekbar.progress
            val offset = currentMetadata?.filePlaybackOffsetMs ?: 0L
            val durationMs = currentMetadata?.durationMs ?: 0L

            // Logic: T (last instant in buffer) - s (seconds from end selected by user)
            // T = offset + durationMs
            // s = durationMs - progress
            // Result = (offset + durationMs) - (durationMs - progress) = offset + progress
            val absTimeMs = offset + progress

            val secondsAgo = (durationMs - progress) / 1000.0
            Logd(String.format("Starting replay from -%.1fs", secondsAgo))
            toast(String.format("Starting replay from -%.1fs", secondsAgo))
            streamService.startReplay(absTimeMs)
        }

        binding.replaySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser || progress == 0) {
                    val binding = _binding ?: return
                    updateThumbnailPosition(binding, progress)

                    val offset = currentMetadata?.filePlaybackOffsetMs ?: 0L
                    val durationMs = currentMetadata?.durationMs ?: 0L
                    val absTimeMs = offset + progress

                    // Update time label with negative offset (e.g. -30.0s to -5.0s)
                    val sMs = durationMs - progress
                    binding.replayTimeCurrent.text = String.format("-%.1fs", sMs / 1000.0)

                    if (fromUser) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                previewMediaPlayer?.seekTo(absTimeMs, MediaPlayer.SEEK_CLOSEST)
                            } else {
                                previewMediaPlayer?.seekTo(absTimeMs.toInt())
                            }
                        } catch (e: Exception) {
                            Loge("ReplaySeekingFragment :: Error seeking preview: ${e.message}")
                        }
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        launchOnResumed {
            streamService.replayMetadata.collect { metadata ->
                if (metadata != null) {
                    currentMetadata = metadata
                    initPreviewMediaPlayer(metadata)

                    val minUsableDurationSeconds = minUsableDurationMs / 1000L
                    val durationMs = metadata.durationMs
                    val durationSeconds = durationMs / 1000.0
                    binding.replaySeekbar.max = max(0L, durationMs - 5000L).toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        binding.replaySeekbar.min = 0
                    }

                    val quickReplayDurationSeconds = replayPrefs.getQuickReplayDurationSeconds()
                    val initialOffsetMs = max(0L, durationMs - (quickReplayDurationSeconds * 1000L))
                    
                    binding.replaySeekbar.progress = min(binding.replaySeekbar.max.toLong(), initialOffsetMs).toInt()
                    binding.replayTimeCurrent.text = String.format(Locale.getDefault(), "-%.1fs", (durationMs - binding.replaySeekbar.progress) / 1000.0)
                    binding.replayTimeTotal.text = "-5.0s"
                    
                    // Trigger the thumb position update and thumbnail seek
                    binding.replaySeekbar.post {
                        val binding = _binding ?: return@post
                        val progress = binding.replaySeekbar.progress
                        val offset = currentMetadata?.filePlaybackOffsetMs ?: 0L
                        val absTimeMs = offset + progress
                        previewMediaPlayer?.seekTo(absTimeMs.toInt())
                        
                        // Force a refresh of the thumbnail position
                        updateThumbnailPosition(binding, progress)
                    }
                }
            }
        }
    }

    private fun updateThumbnailPosition(binding: FragmentReplaySeekingBinding, progress: Int) {
        val percent = if (binding.replaySeekbar.max > 0) progress.toFloat() / binding.replaySeekbar.max else 0f
        val seekbarWidth = binding.replaySeekbar.width - binding.replaySeekbar.paddingLeft - binding.replaySeekbar.paddingRight
        val pos = binding.replaySeekbar.left + binding.replaySeekbar.paddingLeft + percent * seekbarWidth

        binding.replayThumbnail.x =
            min((binding.root.width - binding.replayThumbnail.width).toFloat(),
                max(0f, pos - (binding.replayThumbnail.width / 2f)))

        binding.replayThumbnail.visibility = View.VISIBLE
    }

    private fun initPreviewMediaPlayer(metadata: ReplayMetadata) {
        try {
            stopPreviewMediaPlayer()
            previewMediaPlayer = MediaPlayer().apply {
                setDataSource(requireContext(), Uri.fromFile(File(metadata.filePath)))
                previewSurface?.let { setSurface(it) }
                isLooping = false
                prepare()
                seekTo(metadata.filePlaybackOffsetMs.toInt())
            }
        } catch (e: Exception) {
            Loge("ReplaySeekingFragment :: Error initializing preview MediaPlayer: ${e.message}")
        }
    }

    private fun stopPreviewMediaPlayer() {
        try {
            previewMediaPlayer?.stop()
            previewMediaPlayer?.release()
            previewMediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPreviewMediaPlayer()
        _binding = null
    }
}
