package it.lmqv.livematchcam.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import coil.load
import it.lmqv.livematchcam.R

data class LogoItem(val imageURL: String)

class LogosAdapter(
        context: Context,
        private val items: List<LogoItem>
    ) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): LogoItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ViewHolder
        val view: View

        if (convertView == null) {
            view = inflater.inflate(R.layout.logo_recent_item, parent, false)
            viewHolder = ViewHolder(view.findViewById(R.id.logo_item_image))
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val item = getItem(position)
        viewHolder.imageView.load(item.imageURL) {
            placeholder(R.drawable.shield)
            error(R.drawable.preview_missing)
        }

        return view
    }

    private data class ViewHolder(val imageView: ImageView)
}