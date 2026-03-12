package it.lmqv.livematchcam.fragments.sports.banners

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.dialogs.RecentsDialog
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.preferences.RecentsOverlaysPreferences
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlin.math.roundToInt
import kotlin.text.isNotEmpty

class EditFilterDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_POSITION = "arg_position"

        fun newInstance(position: FilterPosition): EditFilterDialogFragment {
            return EditFilterDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_POSITION, position)
                }
            }
        }
    }

    private lateinit var initialPosition: FilterPosition
    private var selectedPosition: FilterPosition = FilterPosition.TOP_LEFT
    private var selectedSize: Int = 20
    private var selectedVisible: Boolean = false
    private var filterUrls: List<String> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        initialPosition = arguments?.getSerializable(ARG_POSITION) as? FilterPosition ?: FilterPosition.TOP_LEFT
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val currentFilters = MatchRepository.filters.value
        val event = currentFilters.firstOrNull { it.position == initialPosition }
            ?: return super.onCreateDialog(savedInstanceState)
        val currentFilter = event.filter ?: return super.onCreateDialog(savedInstanceState)

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_filter, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Trova le viste nel layout
        val switchVisible = dialogView.findViewById<SwitchCompat>(R.id.switch_visible)!!
        val switchDescription = dialogView.findViewById<TextView>(R.id.switch_description)!!
        val sliderSize = dialogView.findViewById<SeekBar>(R.id.slider_size)!!
        val textSliderSize = dialogView.findViewById<TextView>(R.id.text_slider_size)!!
        val ivFilterUrlPreview = dialogView.findViewById<ImageView>(R.id.filter_url_preview)!!
        val ivFilterUrlTrash = dialogView.findViewById<ImageView>(R.id.filter_url_trash)!!

        val btnPosTopLeft = dialogView.findViewById<ImageButton>(R.id.btn_pos_top_left)!!
        val btnPosTop = dialogView.findViewById<ImageButton>(R.id.btn_pos_top)!!
        val btnPosTopRight = dialogView.findViewById<ImageButton>(R.id.btn_pos_top_right)!!
        val btnPosCenter = dialogView.findViewById<ImageButton>(R.id.btn_pos_center)!!
        val btnPosBottomLeft = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom_left)!!
        val btnPosBottom = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom)!!
        val btnPosBottomRight = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom_right)!!
        
        val btnCancel = dialogView.findViewById<ImageView>(R.id.cancelButton)!!
        val btnSave = dialogView.findViewById<ImageView>(R.id.confirmButton)!!

        // Inizializza stato locale
        selectedPosition = initialPosition
        selectedSize = currentFilter.size
        selectedVisible = currentFilter.visible
        filterUrls = currentFilter.urls

        fun handleSwitchDescription() {
            switchDescription.text = if (selectedVisible) {
                getString(R.string.show_overlay)
            }  else {
                getString(R.string.hide_overlay)
            }
        }

        switchVisible.isChecked = selectedVisible
        handleSwitchDescription()
        sliderSize.progress = selectedSize
        textSliderSize.text = selectedSize.toString()

        switchVisible.setOnCheckedChangeListener { _, isChecked ->
            selectedVisible = isChecked
            handleSwitchDescription()
        }

        sliderSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val step = 5
                val steppedProgress = (progress / step.toFloat()).roundToInt() * step
                selectedSize = steppedProgress
                textSliderSize.text = steppedProgress.toString()
                if (fromUser && progress != steppedProgress) {
                    seekBar?.progress = steppedProgress
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configura i pulsanti della griglia
        val positionButtons = mapOf(
            FilterPosition.TOP_LEFT to btnPosTopLeft,
            FilterPosition.TOP to btnPosTop,
            FilterPosition.TOP_RIGHT to btnPosTopRight,
            FilterPosition.CENTER to btnPosCenter,
            FilterPosition.BOTTOM_LEFT to btnPosBottomLeft,
            FilterPosition.BOTTOM to btnPosBottom,
            FilterPosition.BOTTOM_RIGHT to btnPosBottomRight
        )

        // Determina le posizioni occupate dagli altri filtri attivi e visibili (incluso lo scoreboard)
        val scoreboard = MatchRepository.scoreboard.value
        val occupiedPositions = currentFilters
            .filter { it.position != initialPosition && it.filter != null && it.filter.visible }
            .map { it.filter!!.position }
            .toMutableSet()
        
        if (scoreboard.visible) {
            occupiedPositions.add(scoreboard.position)
        }

        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        val selectableBackgroundResId = typedValue.resourceId

        fun updateGridSelection() {
            positionButtons.forEach { (pos, btn) ->
                if (occupiedPositions.contains(pos)) {
                    btn.isEnabled = false
                    btn.alpha = 0.3f
                    btn.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                } else {
                    btn.isEnabled = true
                    btn.alpha = 1.0f
                    if (pos == selectedPosition) {
                        btn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_1))
                        btn.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
                    } else {
                        btn.setBackgroundResource(selectableBackgroundResId)
                        btn.imageTintList = null
                    }
                }
            }
        }

        positionButtons.forEach { (pos, btn) ->
            btn.setOnClickListener {
                if (!occupiedPositions.contains(pos)) {
                    selectedPosition = pos
                    updateGridSelection()
                }
            }
        }

        updateGridSelection()

        fun loadPreview(url: String) {
            if (url.isNotEmpty()) {
                ivFilterUrlPreview.load(url) {
                    placeholder(R.drawable.preview_missing)
                    error(R.drawable.preview_missing)
                    allowHardware(false)
                    listener(
                        onError = { _, error ->
                            ivFilterUrlTrash.visibility = View.GONE
                            switchVisible.isChecked = false
                            switchVisible.isEnabled = false
                        },
                        onSuccess = { _, result ->
                            ivFilterUrlTrash.visibility = View.VISIBLE
                            switchVisible.isChecked = true
                            switchVisible.isEnabled = true
                            filterUrls = listOf(url)
                        }
                    )
                }
            } else {
                ivFilterUrlTrash.visibility = View.GONE
                switchVisible.isChecked = false
                switchVisible.isEnabled = false
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.preview_missing)
                ivFilterUrlPreview.setImageDrawable(drawable)
            }
        }

        if (filterUrls.isNotEmpty()) {
            loadPreview(filterUrls.first())
        }

        ivFilterUrlPreview.setOnClickListener {
            val currentUrl = filterUrls.firstOrNull() ?: ""
            val dialog = RecentsDialog(
                requireContext(),
                currentUrl,
                RecentsOverlaysPreferences(requireContext()),
                titleResId = R.string.choose_overlay,
                hintResId = R.string.overlay_url_placeholder,
                placeholderResId = R.drawable.preview_missing
            ) { selectedUrl ->
                loadPreview(selectedUrl)
            }
            dialog.show()
        }

        ivFilterUrlTrash.setOnClickListener {
            filterUrls = listOf()
            loadPreview("")
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            // Aggiorniamo semplicemente le proprietà all'interno del filtro attuale.
            // Il contenitore FilterOverlayEvent manterrà la sua "position" originale (che funge da ID),
            // ma il "FilterOverlay" interno avrà la nuova position aggiornata per i vari component/listener.
            val updatedFilter = currentFilter.copy(
                position = selectedPosition, 
                size = selectedSize, 
                visible = selectedVisible,
                urls = filterUrls
            )
            val updatedEvent = event.copy(filter = updatedFilter)
            
            MatchRepository.updateFilter(updatedEvent)
            dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

}
