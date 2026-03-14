package it.lmqv.livematchcam.fragments.replay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import it.lmqv.livematchcam.StreamActivity
import it.lmqv.livematchcam.databinding.FragmentReplayPlayingBinding
import it.lmqv.livematchcam.preferences.ReplayPreferencesManager
import java.util.Locale

class ReplayPlayingFragment : Fragment() {

    private var _binding: FragmentReplayPlayingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReplayPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val minSpeed = 0.25f
    private val maxSpeed = 1.50f
    private val speedRange = maxSpeed - minSpeed

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val streamActivity = requireActivity() as StreamActivity
        val streamService = streamActivity.streamService

        // Initialize Speed UI
        val currentSpeed = ReplayPreferencesManager(requireContext()).getReplaySpeed()
        val initialProgress = speedToProgress(currentSpeed)
        binding.replaySpeedSeekbar.progress = initialProgress
        updateSpeedUI(currentSpeed)

        binding.replayStopButton.setOnClickListener {
            streamService.stopReplay()
        }

        binding.replaySpeedSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Snap to steps of 5
                    val snappedProgress = (progress / 5) * 5
                    if (snappedProgress != progress) {
                        seekBar.progress = snappedProgress
                        return
                    }
                    
                    val speed = progressToSpeed(snappedProgress)
                    streamService.setReplaySpeed(speed)
                    updateSpeedUI(speed)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Preset Buttons
        setupSpeedButton(binding.btnSpeed025, 0.25f)
        setupSpeedButton(binding.btnSpeed050, 0.50f)
        setupSpeedButton(binding.btnSpeed075, 0.75f)
        setupSpeedButton(binding.btnSpeed100, 1.00f)
        setupSpeedButton(binding.btnSpeed125, 1.25f)
        setupSpeedButton(binding.btnSpeed150, 1.50f)
    }

    private fun setupSpeedButton(button: android.widget.Button, speed: Float) {
        button.setOnClickListener {
            val streamActivity = requireActivity() as StreamActivity
            streamActivity.streamService.setReplaySpeed(speed)
            binding.replaySpeedSeekbar.progress = speedToProgress(speed)
            updateSpeedUI(speed)
        }
    }

    private fun updateSpeedUI(speed: Float) {
        binding.replaySpeedText.text = String.format(Locale.getDefault(), "%.2fx", speed)
        
        // Update button visual "toggle" effect
        val buttons = mapOf(
            0.25f to binding.btnSpeed025,
            0.50f to binding.btnSpeed050,
            0.75f to binding.btnSpeed075,
            1.00f to binding.btnSpeed100,
            1.25f to binding.btnSpeed125,
            1.50f to binding.btnSpeed150
        )

        buttons.forEach { (s, btn) ->
            val isActive = Math.abs(s - speed) < 0.01f
            btn.alpha = if (isActive) 1.0f else 0.5f
            // We can also change background tint if needed, but alpha is a clear "active/inactive" signal
        }
    }

    private fun progressToSpeed(progress: Int): Float {
        return minSpeed + (progress / 100f) * speedRange
    }

    private fun speedToProgress(speed: Float): Int {
        val p = ((speed - minSpeed) / speedRange * 100).toInt()
        return Math.max(0, Math.min(100, (p / 5) * 5))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
