package it.lmqv.livematchcam.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.repositories.KeyDescription

class KeyDescriptionAdapter(
        private val context: Context,
        private val items: List<KeyDescription>,
        @DrawableRes val icon: Int
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): KeyDescription = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_keydescription_item, parent, false)

            val iconView = view.findViewById<ImageView>(R.id.spinner_item_image)
            val titleView = view.findViewById<TextView>(R.id.spinner_item_title)
            val keyView = view.findViewById<TextView>(R.id.spinner_item_key)

            val item = items[position]

            iconView.setImageResource(icon)
            titleView.text = item.description
            keyView.text = item.key

            return view
        }
    }