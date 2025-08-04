package it.lmqv.livematchcam.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import it.lmqv.livematchcam.R

class ImageColorControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val imageView: ImageView
    private val buttonSetImage: Button
    private val colorBox1: View
    private val colorBox2: View
    private val buttonColor1: Button
    private val buttonColor2: Button

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_team_control, this, true)

        imageView = findViewById(R.id.imageView)
        buttonSetImage = findViewById(R.id.buttonSetImage)
        colorBox1 = findViewById(R.id.colorBox1)
        colorBox2 = findViewById(R.id.colorBox2)
        buttonColor1 = findViewById(R.id.buttonColor1)
        buttonColor2 = findViewById(R.id.buttonColor2)

        buttonSetImage.setOnClickListener {
            promptForImageUrl()
        }

        buttonColor1.setOnClickListener {
            pickColor { color ->
                colorBox1.setBackgroundColor(color)
            }
        }

        buttonColor2.setOnClickListener {
            pickColor { color ->
                colorBox2.setBackgroundColor(color)
            }
        }
    }

    private fun promptForImageUrl() {
        val editText = EditText(context)
        AlertDialog.Builder(context)
            .setTitle("Enter Image URL")
            .setView(editText)
            .setPositiveButton("Load") { _, _ ->
                val url = editText.text.toString()
                loadImageFromUrl(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadImageFromUrl(url: String) {
        // Usa Glide o altra libreria per caricare l'immagine
        /*Glide.with(context)
            .load(url)
            .placeholder(R.drawable.placeholder) // opzionale
            .error(R.drawable.error) // opzionale
            .into(imageView)*/
    }

    private fun pickColor(callback: (Int) -> Unit) {
        val colors = arrayOf("Red", "Green", "Blue", "Yellow")
        val colorValues = arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)

        AlertDialog.Builder(context)
            .setTitle("Choose color")
            .setItems(colors) { _, which ->
                callback(colorValues[which])
            }
            .show()
    }
}
