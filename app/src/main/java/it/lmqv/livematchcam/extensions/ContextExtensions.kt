package it.lmqv.livematchcam.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.preferences.ThumbnailAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

fun Context.createThumbnail(thumbnailAssets : ThumbnailAssets) : Bitmap? {
    var resultBitmap: Bitmap? = null

    if (thumbnailAssets.background != null) {
        val background = thumbnailAssets.background!!
        val width = background.width
        val height = background.height

        resultBitmap = createBitmap(width, height)
        val canvas = Canvas(resultBitmap)

        canvas.drawBitmap(background, 0f, 0f, null)

        val logoHeight = (height / 2.5f).toInt()
        val centerY = height / 2 - logoHeight / 2

        thumbnailAssets.logoHome?.let {
            val logoWidth = it.width * logoHeight / it.height
            val scaledLeft = it.scale(logoWidth, logoHeight)
            canvas.drawBitmap(
                scaledLeft,
                (width / 4 - logoWidth / 2).toFloat(),
                centerY.toFloat(),
                null
            )
        }

        thumbnailAssets.logoGuest?.let {
            val logoWidth = it.width * logoHeight / it.height
            val scaledRight = it.scale(logoWidth, logoHeight)
            canvas.drawBitmap(
                scaledRight,
                (3 * width / 4 - logoWidth / 2).toFloat(),
                centerY.toFloat(),
                null
            )
        }

        val typeface =
            ResourcesCompat.getFont(this, R.font.roboto_condensed_medium_italic)

        thumbnailAssets.calendar.let {
            val paint = Paint()
            paint.color = Color.WHITE
            paint.textSize = height / 12f
            paint.isAntiAlias = true
            paint.textAlign = Paint.Align.CENTER
            paint.setShadowLayer(10f, 2f, 2f, Color.BLACK)
            paint.typeface = typeface

            var title = formatDate(it)
            canvas.drawText(title, width / 2f, paint.textSize * 1.5f, paint)
        }
    }

    return resultBitmap
}

fun Context.saveBitmapToFile(bitmap: Bitmap, fileName: String): File {
    val file = File(this.cacheDir, fileName)
    val outputStream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    outputStream.flush()
    outputStream.close()
    return file
}

fun Context.convertBitmapToFIle(bitmap: Bitmap) : File {
    var filePath = this.saveBitmapToFile(bitmap, "thumbnail.jpg")
    return File(filePath.absolutePath)
}

//fun loadBitmapFromFile(imageFile: File) : Bitmap {
//    return BitmapFactory.decodeFile(imageFile.absolutePath)
//}

suspend fun Context.loadBitmapFromUrl(urlString: String): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            if (urlString.isNotEmpty()) {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                BitmapFactory.decodeStream(input)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
