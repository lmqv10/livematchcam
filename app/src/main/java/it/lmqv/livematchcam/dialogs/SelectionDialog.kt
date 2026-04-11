package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.lmqv.livematchcam.databinding.DialogSelectionBinding

class SelectionDialog<T>(
    context: Context,
    @StringRes private val titleResId: Int,
    private val items: List<T>,
    private val labelProvider: (T) -> String,
    private val onSelected: (T) -> Unit,
    private val onCancel: () -> Unit = {}
) : Dialog(context) {

    private lateinit var binding: DialogSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.6).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.dialogSelectionTitle.setText(titleResId)

        binding.selectionList.layoutManager = LinearLayoutManager(context)
        binding.selectionList.adapter = SelectionAdapter(items, labelProvider) { item ->
            onSelected(item)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            onCancel()
            dismiss()
        }
    }

    private class SelectionAdapter<T>(
        private val items: List<T>,
        private val labelProvider: (T) -> String,
        private val onItemClick: (T) -> Unit
    ) : RecyclerView.Adapter<SelectionAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(it.lmqv.livematchcam.R.id.item_text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(it.lmqv.livematchcam.R.layout.item_selection, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = labelProvider(item)
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
