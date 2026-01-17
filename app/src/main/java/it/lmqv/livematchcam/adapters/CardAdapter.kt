package it.lmqv.livematchcam.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.factories.sports.Sports

data class CardItem(
    val sport: Sports,
    @StringRes val description: Int,
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
        val selectedCardItem = items.firstOrNull { x -> x.isSelected }
        selectedPosition = if (selectedCardItem != null) items.indexOf(selectedCardItem) else -1
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent?.context).inflate(R.layout.card_sport_item, parent, false)

        val cardItem = items[position]
        val imageView = view.findViewById<ImageView>(R.id.sport_icon)
        val textView = view.findViewById<TextView>(R.id.sport_description)
        val cardView = view.findViewById<CardView>(R.id.sport_card)

        imageView.setImageResource(cardItem.icon)
        parent?.context?.let {
            textView.text = it.getString(cardItem.description)
        }

        view.isSelected = position == selectedPosition

        if (view.isSelected) {
            parent?.context?.let {
                cardView.setCardBackgroundColor(ContextCompat.getColor(it, R.color.WHITE))
                imageView.clearColorFilter()
                textView.setTextColor(ContextCompat.getColorStateList(it, R.color.appColorSecondary))
            }
        } else {
            parent?.context?.let {
                cardView.setCardBackgroundColor(ContextCompat.getColor(it, R.color.gray_400))
                imageView.setColorFilter(R.color.secondary_darker)
                textView.setTextColor(ContextCompat.getColorStateList(it, R.color.gray_600))
            }
        }

        view.setOnClickListener { _ ->
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