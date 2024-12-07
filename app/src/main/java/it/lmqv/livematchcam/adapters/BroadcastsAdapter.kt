package it.lmqv.livematchcam.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import coil.load
import it.lmqv.livematchcam.R

data class BroadcastItem(val imageURL: String,
                         val text: String,
                         val dateFormat: String,
                         val id: String,
                         val boundStreamId: String)

class BroadcastsAdapter(
        private val context: Context,
        private val items: List<BroadcastItem>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Any = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.broadcast_spinner_item, parent, false)

            val imageView = view.findViewById<ImageView>(R.id.spinner_item_image)
            val titleView = view.findViewById<TextView>(R.id.spinner_item_text)
            val dateView = view.findViewById<TextView>(R.id.spinner_item_date)

            val item = items[position]
            imageView.load(item.imageURL) {
                placeholder(R.drawable.youtube_logo)
                error(R.drawable.youtube_logo)
            }

            titleView.text = item.text
            dateView.text = item.dateFormat

            return view
        }
    }