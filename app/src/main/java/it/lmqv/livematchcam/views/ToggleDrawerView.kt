package it.lmqv.livematchcam.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.ViewToggleDrawerBinding
import kotlin.math.roundToInt
import android.os.Handler
import android.os.Looper
import kotlin.math.max
import kotlin.math.min

class ToggleDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var _binding: ViewToggleDrawerBinding? = null
    private val binding get() = _binding!!

    private var isDrawerOpen = false

    private var isDraggingSlider = false
    private val sliderHandler = Handler(Looper.getMainLooper())
    private var debounceRunnable: Runnable? = null

    var onToggleChanged: ((Boolean) -> Unit)? = null
    var onSliderValueChanged: ((Int) -> Unit)? = null
    var onEditRequested: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        _binding = ViewToggleDrawerBinding.inflate(LayoutInflater.from(context), this, true)

        context.theme
            .obtainStyledAttributes(attrs, R.styleable.ToggleDrawerView, 0, 0)
            .apply {
                try {
                    val iconRes = getResourceId(R.styleable.ToggleDrawerView_drawerIcon, 0)
                    if (iconRes != 0) {
                        binding.iconToggle.setImageResource(iconRes)
                    }
                } finally {
                    recycle()
                }
            }

        // Click sull'icona → toggle cassetto con animazione
        binding.iconToggle.setOnClickListener {
            isDrawerOpen = !isDrawerOpen
            val transition = AutoTransition().apply { duration = 200 }
            TransitionManager.beginDelayedTransition(this, transition)
            binding.panelDrawer.visibility = if (isDrawerOpen) View.VISIBLE else View.GONE
            
            if (isDrawerOpen) {
                binding.iconToggle.imageTintList = android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(context, R.color.primary_1)
                )
            } else {
                binding.iconToggle.imageTintList = null
            }
        }

        binding.iconToggle.setOnLongClickListener {
            if (isDrawerOpen) {
                val transition = AutoTransition().apply { duration = 200 }
                TransitionManager.beginDelayedTransition(this, transition)
                binding.panelDrawer.visibility = View.GONE
                binding.iconToggle.imageTintList = null
            }
            onEditRequested?.invoke()
            true
        }

        // Switch listener
        binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            onToggleChanged?.invoke(isChecked)
        }

        binding.increaseSize.setOnClickListener {
            binding.sliderValue.progress = min(100, binding.sliderValue.progress + 5)

            debounceRunnable?.let { sliderHandler.removeCallbacks(it) }
            debounceRunnable = Runnable {
                onSliderValueChanged?.invoke(binding.sliderValue.progress)
            }
            sliderHandler.postDelayed(debounceRunnable!!, 200)
        }

        binding.decreaseSize.setOnClickListener {
            binding.sliderValue.progress = max(5, binding.sliderValue.progress - 5)
            onSliderValueChanged?.invoke(binding.sliderValue.progress)

            debounceRunnable?.let { sliderHandler.removeCallbacks(it) }
            debounceRunnable = Runnable {
                onSliderValueChanged?.invoke(binding.sliderValue.progress)
            }
            sliderHandler.postDelayed(debounceRunnable!!, 200)
        }

        // Slider listener (step di 5)
        binding.sliderValue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val step = 5
                val steppedProgress = (progress / step.toFloat()).roundToInt() * step

                binding.textSliderValue.text = steppedProgress.toString()
                
                if (fromUser) {
                    if (progress != steppedProgress) {
                        seekBar?.progress = steppedProgress
                    }
                    
                    debounceRunnable?.let { sliderHandler.removeCallbacks(it) }
                    debounceRunnable = Runnable {
                        onSliderValueChanged?.invoke(steppedProgress)
                    }
                    sliderHandler.postDelayed(debounceRunnable!!, 200)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isDraggingSlider = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isDraggingSlider = false
            }
        })
    }

    // --- Metodi pubblici programmatici ---

    /** Imposta lo switch senza triggerare la callback */
    fun setToggleChecked(checked: Boolean) {
        binding.switchToggle.setOnCheckedChangeListener(null)
        binding.switchToggle.isChecked = checked
        binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
            onToggleChanged?.invoke(isChecked)
        }
    }

    /** Imposta lo slider senza triggerare la callback (arrotonda al 5) */
    fun setSliderValue(value: Int) {
        if (isDraggingSlider) return // Ignora aggiornamenti remoti mentre l'utente sta trascinando
        
        val step = 5
        val steppedValue = (value / step.toFloat()).roundToInt() * step
        binding.sliderValue.progress = steppedValue.coerceIn(0, 100)
        binding.textSliderValue.text = binding.sliderValue.progress.toString()
    }

    /** Cambia l'icona del drawer da codice */
    fun setDrawerIcon(resId: Int) {
        binding.iconToggle.setImageResource(resId)
    }

    /** Abilita o disabilita le interazioni se il contenuto (es. URL) manca */
    fun setEnabledState(enabled: Boolean) {
        // Lo switch segue lo stato abilitato
        binding.switchToggle.isEnabled = enabled
        
        // Per l'icona, non usiamo isEnabled = false altrimenti perdiamo il LongClick (Edit)
        // Gestiamo invece il click manualmente nel listener o cambiamo lo stato
        if (!enabled) {
            isDrawerOpen = false
            binding.panelDrawer.visibility = View.GONE
            binding.iconToggle.imageTintList = null
            binding.iconToggle.alpha = 0.5f
            binding.switchToggle.alpha = 0.5f
            
            // Rimuoviamo il click di apertura drawer ma lasciamo l'oggetto "enabled" per il long click
            binding.iconToggle.setOnClickListener(null)
        } else {
            binding.iconToggle.alpha = 1.0f
            binding.switchToggle.alpha = 1.0f
            
            // Ripristiniamo il click di apertura drawer
            binding.iconToggle.setOnClickListener {
                isDrawerOpen = !isDrawerOpen
                val transition = AutoTransition().apply { duration = 200 }
                TransitionManager.beginDelayedTransition(this, transition)
                binding.panelDrawer.visibility = if (isDrawerOpen) View.VISIBLE else View.GONE
                
                if (isDrawerOpen) {
                    binding.iconToggle.imageTintList = android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(context, R.color.primary_1)
                    )
                } else {
                    binding.iconToggle.imageTintList = null
                }
            }
        }
    }
}
