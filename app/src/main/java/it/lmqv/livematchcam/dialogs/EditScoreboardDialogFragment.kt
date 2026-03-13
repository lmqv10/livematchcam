package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.drawable.CheckerboardDrawable
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.factories.FiltersFactory
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class EditScoreboardDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(): EditScoreboardDialogFragment {
            return EditScoreboardDialogFragment()
        }
    }

    private var selectedPosition: FilterPosition = FilterPosition.TOP_LEFT
    private var selectedSize: Int = 30
    private var overflowRatio: Float = 1f
    private var selectedVisible: Boolean = true
    private var imageAspectRatio: String? = null

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
        val currentScoreboard = MatchRepository.scoreboard.value
        val currentFilters = MatchRepository.filters.value

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_filter, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Trova le viste nel layout (riutilizzo lo stesso layout dei filtri)
        val switchVisible = dialogView.findViewById<SwitchCompat>(R.id.switch_visible)!!
        val switchDescription = dialogView.findViewById<TextView>(R.id.switch_description)!!
        val increaseSize = dialogView.findViewById<ImageView>(R.id.filter_increase_size)!!
        val decreaseSize = dialogView.findViewById<ImageView>(R.id.filter_decrease_size)!!
        val sliderSize = dialogView.findViewById<SeekBar>(R.id.slider_size)!!
        val textSliderSize = dialogView.findViewById<TextView>(R.id.text_slider_size)!!
        val filterUrlPreview = dialogView.findViewById<ImageView>(R.id.filter_url_preview)!!
        val filterUrlEditContainer = dialogView.findViewById<LinearLayout>(R.id.filter_url_edit_container)!!

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

        // Disable clipping programmatically to allow scoreboard overflow preview
        container.clipChildren = false
        container.clipToPadding = false
        val viewport = dialogView.findViewById<ConstraintLayout>(R.id.filter_preview_viewport)!!
        viewport.clipChildren = false
        viewport.clipToPadding = false

        // Inizializza stato locale
        selectedPosition = currentScoreboard.position
        selectedSize = currentScoreboard.size
        selectedVisible = currentScoreboard.visible

        fun handleSwitchDescription() {
            switchDescription.text = if (selectedVisible) {
                getString(R.string.show_scoreboard)
            }  else {
                getString(R.string.hide_scoreboard)
            }
        }

        fun updatePreviewLayout() {
            // Update visibility
            filterUrlPreview.visibility = if (selectedVisible) View.VISIBLE else View.INVISIBLE

            if (!selectedVisible) return

            val set = ConstraintSet()
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

            // 3. SIZE — always use absolute pixel width (constrainPercentWidth clamps at 1.0)
            val percent = max(0.01f, (selectedSize * overflowRatio) / 100f)
            val absWidth = (viewport.width * percent).toInt()
            set.constrainWidth(R.id.filter_url_preview, absWidth)
            set.constrainHeight(R.id.filter_url_preview, ConstraintSet.WRAP_CONTENT)
            set.setDimensionRatio(R.id.filter_url_preview, "")
            filterUrlPreview.adjustViewBounds = true
            
            set.applyTo(viewport)
        }


        // hide preview handling
        filterUrlEditContainer.visibility = View.GONE

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

        // Determina le posizioni occupate dai filtri attivi e visibili
        val occupiedPositions = currentFilters
            .filter { it.filter != null && it.filter.visible }
            .map { it.filter!!.position }
            .toSet()

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

        var sport = MatchRepository.sport.value
        var scoreBoardPlaecholederfilter = FiltersFactory
            .getScoreBoard(sport, requireContext())
        scoreBoardPlaecholederfilter.getBitmap()?.let { bitmap ->
            filterUrlPreview.setImageBitmap(bitmap)
            imageAspectRatio = "${bitmap.width}:${bitmap.height}"
            overflowRatio = scoreBoardPlaecholederfilter.getOverflowRatio()
            viewport.post { updatePreviewLayout() }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            val updatedScoreboard = currentScoreboard.copy(
                position = selectedPosition,
                size = selectedSize,
                visible = selectedVisible
            )
            MatchRepository.updateScoreboard(updatedScoreboard)
            dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }
}