package it.lmqv.livematchcam.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File

object PathUtils {
    @JvmStatic
    fun getRecordPath(): File {
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        return File(storageDir.absolutePath + "/RootEncoder")
    }

    @JvmStatic
    fun updateGallery(context: Context, path: String) {
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
        context.toast("Video saved at: $path")
    }
}
