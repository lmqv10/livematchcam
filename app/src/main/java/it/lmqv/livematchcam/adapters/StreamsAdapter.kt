package it.lmqv.livematchcam.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DimenRes
import coil.load
import it.lmqv.livematchcam.R

sealed class BaseStreamItem {
    data class MatchStream(
        val thumbnail: String,
        val title: String,
        val server: String,
        val key: String
    ) : BaseStreamItem()

    class AddStream : BaseStreamItem()
}

class StreamsAdapter(
    private val context: Context,
    private val broadcasts: List<BaseStreamItem>,
    @DimenRes private val imageSizeDp: Int = R.dimen.image_size_width_small
) : BaseAdapter() {

    override fun getCount(): Int = broadcasts.size

    override fun getItem(position: Int): BaseStreamItem = broadcasts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val item = getItem(position)
        val view = inflater.inflate(R.layout.spinner_stream_item, parent, false)

        val imageView = view.findViewById<ImageView>(R.id.stream_item_image)
        val titleView = view.findViewById<TextView>(R.id.stream_item_text)
        val serverView = view.findViewById<TextView>(R.id.stream_item_server)
        val keyView = view.findViewById<TextView>(R.id.stream_item_key)

        val sizePx = imageView.context.resources.getDimensionPixelSize(imageSizeDp)
        imageView.layoutParams.width = sizePx
        imageView.layoutParams.height = (sizePx * 9f / 16f).toInt()
        imageView.requestLayout()

        when (item) {
            is BaseStreamItem.MatchStream -> {
                imageView.load(item.thumbnail + "?ts=" + System.currentTimeMillis()) {
                    placeholder(R.drawable.preview_missing)
                    error(R.drawable.preview_missing)
                    crossfade(true)
                }

                titleView.text = item.title
                serverView.text = item.server
                keyView.text = item.key
                view
            }

            is BaseStreamItem.AddStream -> {
            /*    imageView.setImageResource(R.drawable.ic_add)
                titleView.text = context.getString(R.string.add)
                dateView.text = context.getString(R.string.new_schedule)*/
            }
        }

        return view
    }
}
