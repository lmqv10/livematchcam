package it.lmqv.livematchcam

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import it.lmqv.livematchcam.databinding.SpinnerCameraSourceItemBinding
import it.lmqv.livematchcam.viewmodels.VideoSourceKind

class CameraSourceAdapter(
    private val context: Context,
    private val items: List<CameraSourceItem>
) : BaseAdapter() {

    fun getSelectedIndex(videoSourceKind: VideoSourceKind): Int {
        return items
            .map { it.videoSourceKind }
            .indexOf(videoSourceKind)
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): CameraSourceItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val binding: SpinnerCameraSourceItemBinding = if (convertView == null) {
            SpinnerCameraSourceItemBinding.inflate(LayoutInflater.from(context), parent, false)
        } else {
            SpinnerCameraSourceItemBinding.bind(convertView)
        }

        val cameraSourceItem = items[position]

        binding.tvCameraSourceItem.text = cameraSourceItem.videoSourceKind.label

        val drawable = ContextCompat.getDrawable(context, cameraSourceItem.icon)
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.icon_size_default)
        drawable?.setBounds(0, 0, iconSize, iconSize)
        binding.tvCameraSourceItem.setCompoundDrawablesRelative(drawable, null, null, null)

        return binding.root
    }
}