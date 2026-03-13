package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import android.view.View
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.drawable.CheckerboardDrawable
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.preferences.RecentsOverlaysPreferences
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    private var imageAspectRatio: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        initialPosition = arguments?.getSerializable(ARG_POSITION) as? FilterPosition
            ?: FilterPosition.TOP_LEFT
    }

    override fun onResume() {
        super.onResume()
//        val window = dialog?.window ?: return
//        val displayMetrics = DisplayMetrics()

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val bounds = requireActivity().windowManager.currentWindowMetrics.bounds
//            val width = (bounds.width() * 0.6).toInt()
//            val height = (bounds.height() * 0.9).toInt()
//            window.setLayout(width, height)
//        } else {
//            @Suppress("DEPRECATION")
//            requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
//            val width = (displayMetrics.widthPixels * 0.6).toInt()
//            val height = (displayMetrics.heightPixels * 0.9).toInt()
//            window.setLayout(width, height)
//        }
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
        val increaseSize = dialogView.findViewById<ImageView>(R.id.filter_increase_size)!!
        val decreaseSize = dialogView.findViewById<ImageView>(R.id.filter_decrease_size)!!
        val sliderSize = dialogView.findViewById<SeekBar>(R.id.slider_size)!!
        val textSliderSize = dialogView.findViewById<TextView>(R.id.text_slider_size)!!
        val ivFilterUrlPreview = dialogView.findViewById<ImageView>(R.id.filter_url_preview)!!
        val ivFilterUrlEdit = dialogView.findViewById<ImageView>(R.id.filter_url_edit)!!

        val btnPosTopLeft = dialogView.findViewById<ImageButton>(R.id.btn_pos_top_left)!!
        val btnPosTop = dialogView.findViewById<ImageButton>(R.id.btn_pos_top)!!
        val btnPosTopRight = dialogView.findViewById<ImageButton>(R.id.btn_pos_top_right)!!
        val btnPosCenter = dialogView.findViewById<ImageButton>(R.id.btn_pos_center)!!
        val btnPosBottomLeft = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom_left)!!
        val btnPosBottom = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom)!!
        val btnPosBottomRight = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom_right)!!

        val btnCancel = dialogView.findViewById<ImageView>(R.id.cancelButton)!!
        val btnSave = dialogView.findViewById<ImageView>(R.id.confirmButton)!!

        val container = dialogView.findViewById<ConstraintLayout>(R.id.filter_preview_container)!!
        container.background = CheckerboardDrawable()

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

        fun updatePreviewLayout() {
            ivFilterUrlPreview.visibility = if (selectedVisible && filterUrls.isNotEmpty()) View.VISIBLE else View.INVISIBLE

            if (!selectedVisible || filterUrls.isEmpty()) return

            val set = ConstraintSet()
            // CLONE THE VIEWPORT container where the image is nested
            val viewport = dialogView.findViewById<ConstraintLayout>(R.id.filter_preview_viewport)!!
            set.clone(viewport)

            // CLEAR ALL constraints for the preview image to start fresh
            set.clear(R.id.filter_url_preview, ConstraintSet.TOP)
            set.clear(R.id.filter_url_preview, ConstraintSet.BOTTOM)
            set.clear(R.id.filter_url_preview, ConstraintSet.START)
            set.clear(R.id.filter_url_preview, ConstraintSet.END)

            // FORCE Robust handling - Both MATCH_CONSTRAINT for ratio to work
            set.constrainWidth(R.id.filter_url_preview, ConstraintSet.MATCH_CONSTRAINT)
            set.constrainHeight(R.id.filter_url_preview, ConstraintSet.MATCH_CONSTRAINT)
            set.setDimensionRatio(R.id.filter_url_preview, imageAspectRatio ?: "1:1")

            // 1. HORIZONTAL POSITIONING
            set.connect(R.id.filter_url_preview, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            set.connect(R.id.filter_url_preview, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            val hBias = when (selectedPosition) {
                FilterPosition.TOP_LEFT, FilterPosition.BOTTOM_LEFT -> 0.0f
                FilterPosition.TOP_RIGHT, FilterPosition.BOTTOM_RIGHT -> 1.0f
                else -> 0.5f // TOP, BOTTOM, CENTER
            }
            set.setHorizontalBias(R.id.filter_url_preview, hBias)

            // 2. VERTICAL POSITIONING
            when (selectedPosition) {
                FilterPosition.TOP_LEFT, FilterPosition.TOP, FilterPosition.TOP_RIGHT -> {
                    set.connect(R.id.filter_url_preview, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                }
                FilterPosition.BOTTOM_LEFT, FilterPosition.BOTTOM, FilterPosition.BOTTOM_RIGHT -> {
                    set.connect(R.id.filter_url_preview, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                }
                FilterPosition.CENTER -> {
                    // CENTER Logic using Guideline to allow overflow without squashing
                    set.connect(R.id.filter_url_preview, ConstraintSet.TOP, R.id.guideline_h_center, ConstraintSet.TOP)
                    set.connect(R.id.filter_url_preview, ConstraintSet.BOTTOM, R.id.guideline_h_center, ConstraintSet.BOTTOM)
                }
            }

            // 3. SIZE
            val percent = max(0.01f, selectedSize / 100f)
            set.constrainPercentWidth(R.id.filter_url_preview, percent)
            
            set.applyTo(viewport)
        }

        switchVisible.isChecked = selectedVisible
        handleSwitchDescription()
        sliderSize.progress = selectedSize
        textSliderSize.text = selectedSize.toString()

        switchVisible.setOnCheckedChangeListener { _, isChecked ->
            selectedVisible = isChecked
            handleSwitchDescription()
            updatePreviewLayout()
        }

        increaseSize.setOnClickListener {
            sliderSize.progress = min(100, sliderSize.progress + 5)
            textSliderSize.text = sliderSize.progress.toString()
        }

        decreaseSize.setOnClickListener {
            sliderSize.progress = max(5, sliderSize.progress - 5)
            textSliderSize.text = sliderSize.progress.toString()
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
                updatePreviewLayout()
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

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        val selectableBackgroundResId = typedValue.resourceId

        fun updateGridSelection() {
            positionButtons.forEach { (pos, btn) ->
                if (occupiedPositions.contains(pos)) {
                    btn.isEnabled = false
                    btn.alpha = 0.3f
                    btn.setBackgroundColor(Color.TRANSPARENT)
                } else {
                    btn.isEnabled = true
                    btn.alpha = 1.0f
                    if (pos == selectedPosition) {
                        btn.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary_dark))
                        btn.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
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
                    updatePreviewLayout()
                }
            }
        }

        updateGridSelection()

        fun loadPreview(url: String) {
            val placeholderRes = R.drawable.preview_missing
            if (url.isNotEmpty()) {
                // Load into main preview
                ivFilterUrlPreview.load(url) {
                    placeholder(placeholderRes)
                    error(placeholderRes)
                }

                ivFilterUrlEdit.load(url) {
                    placeholder(placeholderRes)
                    error(placeholderRes)
                    listener(
                        onError = { _, _ ->
                            switchVisible.isChecked = false
                            switchVisible.isEnabled = false
                            sliderSize.isEnabled = false
                            sliderSize.alpha = 0.3f
                            increaseSize.isEnabled = false
                            increaseSize.alpha = 0.3f
                            decreaseSize.isEnabled = false
                            decreaseSize.alpha = 0.3f

                        },
                        onSuccess = { _, result ->
                            switchVisible.isChecked = true
                            switchVisible.isEnabled = true
                            sliderSize.isEnabled = true
                            sliderSize.alpha = 1f
                            increaseSize.isEnabled = true
                            increaseSize.alpha = 1f
                            decreaseSize.isEnabled = true
                            decreaseSize.alpha = 1f
                            filterUrls = listOf(url)

                            // Capture aspect ratio from the loaded bitmap
                            val drawable = result.drawable
                            val width = drawable.intrinsicWidth
                            val height = drawable.intrinsicHeight
                            if (width > 0 && height > 0) {
                                imageAspectRatio = "$width:$height"
                            }
                            updatePreviewLayout()
                        }
                    )
                }
            } else {
                filterUrls = listOf()
                imageAspectRatio = "1:1"
                switchVisible.isChecked = false
                switchVisible.isEnabled = false
                sliderSize.isEnabled = false
                sliderSize.alpha = 0.3f
                increaseSize.isEnabled = false
                increaseSize.alpha = 0.3f
                decreaseSize.isEnabled = false
                decreaseSize.alpha = 0.3f
                ivFilterUrlPreview.load(placeholderRes)
                ivFilterUrlEdit.load(placeholderRes)

                updatePreviewLayout()
            }
        }

        if (filterUrls.isNotEmpty()) {
            loadPreview(filterUrls.first())
        } else {
            updatePreviewLayout()
        }

        ivFilterUrlEdit.setOnClickListener {
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

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
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