package it.lmqv.livematchcam.extensions

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.text.Editable
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import it.lmqv.livematchcam.R


fun Context.showColorPickerDialog(delegate: (color: Int) -> Unit) {
    val context = this
    val dialogView = LayoutInflater.from(context).inflate(R.layout.color_picker_dialog, null)
    val dialog = AlertDialog.Builder(context, R.style.AppDialogTheme)
        .setView(dialogView)
        .create()

    /*var logoUrlLabel = dialogView.findViewById<TextView>(R.id.logo_url_label)
    var logoUrl = dialogView.findViewById<EditText>(R.id.logo_url)

    if (isLogoAvailable) {
        logoUrlLabel.visibility = View.VISIBLE
        logoUrl.visibility = View.VISIBLE
    } else {
        logoUrlLabel.visibility = View.GONE
        logoUrl.visibility = View.GONE
    }*/

    dialogView.findViewById<View>(R.id.tShirtBlack).setOnClickListener {
        delegate(Color.BLACK)
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtWhite).setOnClickListener {
        delegate(Color.WHITE)
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtGreen).setOnClickListener {
        delegate(ContextCompat.getColor(context, R.color.GREEN))
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtRed).setOnClickListener {
        delegate(Color.RED)
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtGray).setOnClickListener {
        delegate(Color.GRAY)
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtOrange).setOnClickListener {
        delegate(ContextCompat.getColor(context, R.color.ORANGE))
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtCornFlowerBlue).setOnClickListener {
        delegate(ContextCompat.getColor(context, R.color.CORNFLOWERBLUE))
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtDarkBlue).setOnClickListener {
        delegate(ContextCompat.getColor(context, R.color.DARKBLUE))
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtYellow).setOnClickListener {
        delegate(ContextCompat.getColor(context, R.color.YELLOW))
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtPink).setOnClickListener {
        delegate(ContextCompat.getColor(context, R.color.PINK))
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtViolet).setOnClickListener {
        delegate(ContextCompat.getColor(context, R.color.VIOLET))
        dialog.dismiss()
    }
    dialogView.findViewById<View>(R.id.tShirtSlateBlue).setOnClickListener {
        delegate(ContextCompat.getColor(context, R.color.SLATEBLUE))
        dialog.dismiss()
    }

    dialog.show()
}

fun Context.showEditStringDialog(@StringRes
                                 resTitleId: Int,
                                 textValue: String,
                                 filters: Array<InputFilter> = arrayOf<InputFilter>(
                                     InputFilter.LengthFilter(3),
                                     InputFilter.AllCaps()  // Example: Make text uppercase
                                 ),
                                 delegate: (updatedTextValue: String) -> Unit) {
    var context = this
    val inflater = LayoutInflater.from(context)
    val dialogView = inflater.inflate(R.layout.dialog_edit_text, null)

    val tvTitle = dialogView.findViewById<TextView>(R.id.tv_title)
    tvTitle.text = getString(resTitleId)

    val etTextValue = dialogView.findViewById<EditText>(R.id.et_text_value)
    etTextValue.text = Editable.Factory.getInstance().newEditable(textValue)
    etTextValue.filters = filters

    etTextValue.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            etTextValue.post(Runnable { etTextValue.selectAll() })
        }
    }

    val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
        .setView(dialogView)
        .setPositiveButton("Confirm") { dialog, _ ->
            delegate(etTextValue.text.toString())
            dialog.dismiss()
        }
        .create()

    dialog.setOnShowListener {
        etTextValue.requestFocus()
    }
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    dialog.show()
}

fun Context.showQRCode(content: String) {
    var context = this
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_qrcode, null)
    val dialog = AlertDialog.Builder(context, R.style.AppDialogTheme)
        .setView(dialogView)
        .setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
        }
        .create()

    val qrCodeImageView = dialogView.findViewById<ImageView>(R.id.qr_code)
    try {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        qrCodeImageView.setImageBitmap(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    dialog.show()
}