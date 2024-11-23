package it.lmqv.livematchcam.handlers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import it.lmqv.livematchcam.utils.KeyValue

class StreamKeysHandler(context: Context) {
//    private val KEYS =  "Keys"
//    private val CURRENT_KEY =  "CurrentKey"
//
//    private val sharedPreferences: SharedPreferences =
//        context.getSharedPreferences("StreamKeysSettings", Context.MODE_PRIVATE)
//
//    private val gson = Gson()
//
//    fun getKeys(): MutableList<String> {
//        val json = sharedPreferences.getString(KEYS, null)
//        return if (json != null) {
//            val type = object : TypeToken<MutableList<String>>() {}.type
//            gson.fromJson(json, type)
//        } else {
//            mutableListOf()
//        }
//    }
//
//    fun saveKeys(items: List<KeyValue<Int>>) {
//        val json = gson.toJson(items)
//        sharedPreferences.edit().putString(KEYS, json).apply()
//    }
//
//    fun getCurrentKey(): String {
//        return sharedPreferences.getString(CURRENT_KEY, "") ?: ""
//    }
//
//    fun saveCurrentKey(key: String) {
//        sharedPreferences.edit().putString(CURRENT_KEY, key).apply()
//    }
}
