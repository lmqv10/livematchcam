package it.lmqv.livematchcam.fragments.sports.banners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentFiltersControlBinding
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.fragments.sports.BaseFiltersControlFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.firebase.FilterOverlayEvent
import it.lmqv.livematchcam.services.firebase.ScoreboardOverlay
import it.lmqv.livematchcam.views.ToggleDrawerView
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FiltersControlFragment : BaseFiltersControlFragment() {

    companion object {
        fun newInstance() = FiltersControlFragment()
    }

    private var _binding: FragmentFiltersControlBinding? = null
    private val binding get() = _binding!!

    private val drawerViews = mutableMapOf<FilterPosition, ToggleDrawerView>()
    private var scoreboardDrawerView: ToggleDrawerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFiltersControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScoreboardView()

        lifecycleScope.launch {
            MatchRepository.scoreboard.collect { scoreboard ->
                updateScoreboardView(scoreboard)
            }
        }

        lifecycleScope.launch {
            MatchRepository.filters.collect { filters ->
                updateFilterViews(filters)
            }
        }
    }

    private fun setupScoreboardView() {
        scoreboardDrawerView = ToggleDrawerView(requireContext()).apply {
            setDrawerIcon(R.drawable.ic_scoreboard)

            onToggleChanged = { isChecked ->
                val current = MatchRepository.scoreboard.value
                MatchRepository.updateScoreboard(current.copy(visible = isChecked))
            }
            onSliderValueChanged = { value ->
                val current = MatchRepository.scoreboard.value
                MatchRepository.updateScoreboard(current.copy(size = value))
            }
            onEditRequested = {
                showEditScoreboardDialog()
            }
        }
        binding.scoreboardContainer.addView(scoreboardDrawerView)
    }

    private fun updateScoreboardView(scoreboard: ScoreboardOverlay) {
        scoreboardDrawerView?.setToggleChecked(scoreboard.visible)
        scoreboardDrawerView?.setSliderValue(scoreboard.size)
    }

    private fun updateFilterViews(filters: List<FilterOverlayEvent>) {
        val currentPositions = filters.map { it.position }.toSet()
        val existingPositions = drawerViews.keys.toSet()

        (existingPositions - currentPositions).forEach { position ->
            drawerViews[position]?.let { binding.filtersContainer.removeView(it) }
            drawerViews.remove(position)
        }

        // Ordina i filtri in base all'ordine dichiarato nell'enum FilterPosition
        var viewIndex = 0
        FilterPosition.entries.forEach { position ->
            val filterEvent = filters.firstOrNull { it.position == position }
            val filter = filterEvent?.filter

            if (filterEvent != null) {
                val drawerView = drawerViews[position]
                if (drawerView != null) {
                    drawerView.setToggleChecked(filter?.visible ?: false)
                    drawerView.setSliderValue(filter?.size ?: 0)
                    
                    val hasUrl = filter?.urls?.any { it.isNotEmpty() } ?: false
                    drawerView.setEnabledState(hasUrl)
                    
                    // Assicura che la view sia all'indice corretto nel LinearLayout
                    if (binding.filtersContainer.getChildAt(viewIndex) != drawerView) {
                        binding.filtersContainer.removeView(drawerView)
                        binding.filtersContainer.addView(drawerView, viewIndex)
                    }
                } else {
                    val newView = ToggleDrawerView(requireContext()).apply {
                        setDrawerIcon(getIconForPosition(position))
                        setToggleChecked(filter?.visible ?: false)
                        setSliderValue(filter?.size ?: 0)

                        val hasUrl = filter?.urls?.any { it.isNotEmpty() } ?: false
                        setEnabledState(hasUrl)

                        onToggleChanged = { isChecked ->
                            onFilterToggleChanged(position, isChecked)
                        }
                        onSliderValueChanged = { value ->
                            onFilterSliderChanged(position, value)
                        }
                        onEditRequested = {
                            showEditFilterDialog(position)
                        }
                    }
                    binding.filtersContainer.addView(newView, viewIndex)
                    drawerViews[position] = newView
                }
                viewIndex++
            }
        }
    }

    private fun onFilterToggleChanged(position: FilterPosition, isChecked: Boolean) {
        val event = MatchRepository.filters.value.firstOrNull { it.position == position } ?: return
        val updated = event.copy(filter = event.filter?.copy(visible = isChecked))
        MatchRepository.updateFilter(updated)
    }

    private fun onFilterSliderChanged(position: FilterPosition, value: Int) {
        val event = MatchRepository.filters.value.firstOrNull { it.position == position } ?: return
        val updated = event.copy(filter = event.filter?.copy(size = value))
        MatchRepository.updateFilter(updated)
    }

    /**
     * Mappa ogni FilterPosition a un'icona drawable.
     */
    private fun getIconForPosition(position: FilterPosition): Int {
        return when (position) {
            FilterPosition.TOP_LEFT -> R.drawable.ic_spot_top_left
            FilterPosition.TOP -> R.drawable.ic_spot_top
            FilterPosition.TOP_RIGHT -> R.drawable.ic_spot_top_right
            FilterPosition.CENTER -> R.drawable.ic_spot_center
            FilterPosition.BOTTOM_LEFT -> R.drawable.ic_spot_bottom_left
            FilterPosition.BOTTOM -> R.drawable.ic_spot_bottom
            FilterPosition.BOTTOM_RIGHT -> R.drawable.ic_spot_bottom_right
        }
    }

    private fun showEditFilterDialog(initialPosition: FilterPosition) {
        val dialog = EditFilterDialogFragment.newInstance(initialPosition)
        dialog.show(childFragmentManager, "EditFilterDialog")
    }

    private fun showEditScoreboardDialog() {
        val dialog = EditScoreboardDialogFragment.newInstance()
        dialog.show(childFragmentManager, "EditScoreboardDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scoreboardDrawerView = null
        drawerViews.clear()
        _binding = null
    }
}
