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
import com.google.api.services.youtube.model.LiveBroadcast
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.extensions.formatDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

sealed class LiveBroadcastItem {
    data class EditBroadcast(
        private val liveBroadcast: LiveBroadcast,
        val format: String = "EEEE dd MMMM yyyy HH:mm"
    ) : LiveBroadcastItem() {
        val broadcastId: String get() = liveBroadcast.id
        val thumbnailUrl: String get() = liveBroadcast.snippet.thumbnails?.standard?.url.orEmpty()
        val title: String get() = liveBroadcast.snippet.title
        val dateFormat: String get() = formatDate(liveBroadcast.snippet.scheduledStartTime, format)
        val scheduledStartTime: ZonedDateTime get() = Instant.ofEpochMilli(liveBroadcast.snippet.scheduledStartTime.value).atZone(ZoneId.systemDefault())
        val boundStreamId: String get() = liveBroadcast.contentDetails.boundStreamId
    }

    class AddBroadcast : LiveBroadcastItem()
}

class BroadcastsAdapter(
    private val context: Context,
    private val broadcasts: List<LiveBroadcastItem>,
    @DimenRes private val imageSizeDp: Int = R.dimen.image_size_width_large
) : BaseAdapter() {

    override fun getCount(): Int = broadcasts.size

    override fun getItem(position: Int): LiveBroadcastItem = broadcasts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val item = getItem(position)
        val view = inflater.inflate(R.layout.spinner_broadcast_item, parent, false)

        val imageView = view.findViewById<ImageView>(R.id.spinner_item_image)
        val titleView = view.findViewById<TextView>(R.id.spinner_item_text)
        val dateView = view.findViewById<TextView>(R.id.spinner_item_date)

        val sizePx = imageView.context.resources.getDimensionPixelSize(imageSizeDp)
        imageView.layoutParams.width = sizePx
        imageView.layoutParams.height = (sizePx * 9f / 16f).toInt()
        imageView.requestLayout()

        when (item) {
            is LiveBroadcastItem.EditBroadcast -> {
                imageView.load(item.thumbnailUrl + "?ts=" + System.currentTimeMillis()) {
                    placeholder(R.drawable.preview_missing)
                    error(R.drawable.preview_missing)
                    crossfade(true)
                }

                titleView.text = item.title
                dateView.text = item.dateFormat
                view
            }

            is LiveBroadcastItem.AddBroadcast -> {
                imageView.setImageResource(R.drawable.ic_add)
                titleView.text = context.getString(R.string.add)
                dateView.text = context.getString(R.string.new_schedule)
            }
        }

        return view
    }
}
