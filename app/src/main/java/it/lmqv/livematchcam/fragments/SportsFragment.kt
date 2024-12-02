package it.lmqv.livematchcam.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSportsBinding
import it.lmqv.livematchcam.factories.Sports
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.repositories.StreamersSettingsRepository
import kotlinx.coroutines.launch

class SportsFragment : Fragment() {

    companion object {
        fun newInstance() = SportsFragment()
    }

    private lateinit var streamersSettingsRepository : StreamersSettingsRepository

    private val cardItems = listOf(
        CardItem(sport = Sports.SOCCER, description = "Soccer", icon = R.drawable.sport_soccer),
        CardItem(sport = Sports.VOLLEY, description = "Volley", icon = R.drawable.sport_volley)
    )

    private var sportsFactory = SportsFactory

    private var _binding: FragmentSportsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSportsBinding.inflate(inflater, container, false)
        streamersSettingsRepository = StreamersSettingsRepository(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            streamersSettingsRepository.getSport.collect {
                sportsFactory.set(it)
                cardItems.forEach { item -> item.isSelected = item.sport == it }

                val adapter = CardAdapter(cardItems) { selectedItem ->
                    lifecycleScope.launch {
                        streamersSettingsRepository.setSport(selectedItem.sport)
                    }
                }
                binding.sportsGrid.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

data class CardItem(
    val sport: Sports,
    val description: String,
    var isSelected: Boolean = false,
    @DrawableRes val icon: Int
)

class CardAdapter(
    private val items: List<CardItem>,
    private val onItemSelected: (CardItem) -> Unit
) : BaseAdapter() {

    private var selectedPosition: Int = -1

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = items[position].sport.ordinal.toLong()

    init {
        var selectedCardItem = items.firstOrNull() { x -> x.isSelected }
        selectedPosition = if (selectedCardItem != null) items.indexOf(selectedCardItem) else -1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent?.context).inflate(R.layout.card_sport_item, parent, false)

        val cardItem = items[position]
        val imageView = view.findViewById<ImageView>(R.id.sport_icon)
        val cardView = view.findViewById<CardView>(R.id.sport_card)

        imageView.setImageResource(cardItem.icon)
        view.isSelected = position == selectedPosition

        if (view.isSelected) {
            parent?.context?.let {
                cardView.setCardBackgroundColor(ContextCompat.getColor(it, R.color.secondary))
                imageView.setColorFilter(Color.WHITE)
            }
        } else {
            cardView.setCardBackgroundColor(Color.WHITE)
            imageView.clearColorFilter()
        }

        view.setOnClickListener { v ->
            if (selectedPosition != position) {
                if (selectedPosition >= 0) {
                    items[selectedPosition].isSelected = false
                }
                selectedPosition = position
                cardItem.isSelected = true
                notifyDataSetChanged()
                onItemSelected(cardItem)
            }
        }
        return view
    }
}
