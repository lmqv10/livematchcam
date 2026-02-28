package it.lmqv.livematchcam.fragments.sports.banners

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.firebase.ScoreboardOverlay
import kotlin.math.roundToInt

class EditScoreboardDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(): EditScoreboardDialogFragment {
            return EditScoreboardDialogFragment()
        }
    }

    private var selectedPosition: FilterPosition = FilterPosition.TOP_LEFT
    private var selectedSize: Int = 30
    private var selectedVisible: Boolean = true

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
        val sliderSize = dialogView.findViewById<SeekBar>(R.id.slider_size)!!
        val textSliderSize = dialogView.findViewById<TextView>(R.id.text_slider_size)!!
        val filterUrlContainer = dialogView.findViewById<LinearLayout>(R.id.filter_url_container)!!

        val btnPosTopLeft = dialogView.findViewById<ImageButton>(R.id.btn_pos_top_left)!!
        val btnPosTop = dialogView.findViewById<ImageButton>(R.id.btn_pos_top)!!
        val btnPosTopRight = dialogView.findViewById<ImageButton>(R.id.btn_pos_top_right)!!
        val btnPosCenter = dialogView.findViewById<ImageButton>(R.id.btn_pos_center)!!
        val btnPosBottomLeft = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom_left)!!
        val btnPosBottom = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom)!!
        val btnPosBottomRight = dialogView.findViewById<ImageButton>(R.id.btn_pos_bottom_right)!!
        
        val btnCancel = dialogView.findViewById<ImageView>(R.id.cancelButton)!!
        val btnSave = dialogView.findViewById<ImageView>(R.id.confirmButton)!!

        // hide url handling
        filterUrlContainer.visibility = View.GONE

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

        // Determina le posizioni occupate dai filtri attivi e visibili
        val occupiedPositions = currentFilters
            .filter { it.filter != null && it.filter.visible }
            .map { it.filter!!.position }
            .toSet()

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
