package it.lmqv.livematchcam.preferences

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.lmqv.livematchcam.utils.OptionItem
import androidx.core.content.edit

class RecentsPreferences(context: Context, private val key: String) {
    private val prefs = context.getSharedPreferences("recent_prefs", Context.MODE_PRIVATE)

    private val maxItems: Int = 30
    private val gson = Gson()

    fun getRecents(): List<OptionItem<String>> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<OptionItem<String>>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveRecent(item: OptionItem<String>) {
        val current = getRecents().toMutableList()
        current.removeAll { it.key == item.key }
        current.add(0, item)
        val limited = current.take(maxItems)
        val json = gson.toJson(limited)
        prefs.edit { putString(key, json) }
    }

    fun removeRecent(itemKey: String): List<OptionItem<String>> {
        val current = getRecents().toMutableList()
        current.removeAll { it.key == itemKey }
        val updated = current.take(maxItems)
        val json = gson.toJson(updated)
        prefs.edit { putString(key, json) }
        return getRecents()
    }
}

// Factory functions per retrocompatibilità e leggibilità
fun RecentsLogosPreferences(context: Context) = RecentsPreferences(context, "logos")
fun RecentsOverlaysPreferences(context: Context) = RecentsPreferences(context, "overlays")

/*val defaultJson = """
    [
        { "description": "ALB", "key": "https://content-s3.tuttocampo.it/Teams/200/1201525.png" },
        { "description": "ALC", "key": "https://content-s3.tuttocampo.it/Teams/200/1011300.png" },
        { "description": "BRA", "key": "https://content-s3.tuttocampo.it/Teams/200/1237423.png" },
        { "description": "GIA", "key": "https://content-s3.tuttocampo.it/Teams/200/1010440.png" },
        { "description": "LEC", "key": "https://content-s3.tuttocampo.it/Teams/200/1124525.png" },
        { "description": "NOV", "key": "https://content-s3.tuttocampo.it/Teams/200/1201528.png" },
        { "description": "PER", "key": "https://content-s3.tuttocampo.it/Teams/200/1098317.png" },
        { "description": "PPA", "key": "https://content-s3.tuttocampo.it/Teams/200/1098316.png" },
        { "description": "PVE", "key": "https://content-s3.tuttocampo.it/Teams/200/1124526.png" },
        { "description": "REN", "key": "https://content-s3.tuttocampo.it/Teams/200/1139782.png" },
        { "description": "TOR", "key": "https://content-s3.tuttocampo.it/Teams/200/1237324.png" },
        { "description": "UBR", "key": "https://content-s3.tuttocampo.it/Teams/200/1281730.png" },
        { "description": "COM", "key": "https://content-s3.tuttocampo.it/Teams/200/1124524.png" }
    ]
""".trimIndent()*/