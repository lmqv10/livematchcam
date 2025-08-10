package it.lmqv.livematchcam.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import it.lmqv.livematchcam.R

data class LiveStreamItem(val title: String,
                                 val id: String,
                                 val address: String,
                                 val streamName: String)

class LiveStreamAdapter(
        private val context: Context,
        private val items: List<LiveStreamItem>
    ) : BaseAdapter() {

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): LiveStreamItem = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_livestream_item, parent, false)

            val titleView = view.findViewById<TextView>(R.id.spinner_item_title)
            val idView = view.findViewById<TextView>(R.id.spinner_item_id)
            val serverView = view.findViewById<TextView>(R.id.spinner_item_server)
            val keyView = view.findViewById<TextView>(R.id.spinner_item_key)

            val item = items[position]

            titleView.text = item.title
            idView.text = item.id
            serverView.text = item.address
            keyView.text = item.streamName

            return view
        }
    }