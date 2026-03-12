package it.lmqv.livematchcam.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.load
import it.lmqv.livematchcam.R

data class ImageResourceItem(val url: String)

class ImageResourcesAdapter(
    context: Context,
    private var items: List<ImageResourceItem>,
    private val onClick: (ImageResourceItem) -> Unit,
    private val onLongClick: (ImageResourcesAdapter, ImageResourceItem) -> Unit,
    @DrawableRes private val placeholderResId: Int = R.drawable.shield,
    ) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): ImageResourceItem = items[position]

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
        viewHolder.imageView.load(item.url) {
            placeholder(placeholderResId)
            error(R.drawable.preview_missing)
        }

        viewHolder.imageView.setOnClickListener {
            onClick(item)
        }

        viewHolder.imageView.setOnLongClickListener {
            onLongClick(this, item)
            true
        }
        return view
    }

    fun updateItems(newItems: List<ImageResourceItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    private data class ViewHolder(val imageView: ImageView)
}