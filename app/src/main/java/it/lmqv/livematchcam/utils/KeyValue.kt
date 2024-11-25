package it.lmqv.livematchcam.utils

import android.widget.SpinnerAdapter
import kotlin.math.max

interface IKeyValue<T> {
    val key: T
    val description: String
}

data class KeyValue<T>(override val key: T, override val description: String) : IKeyValue<T> {
    override fun toString(): String {
        return description
    }
}

fun <K, T> SpinnerAdapter.getItemPositionByKey(key: K): Int where T : IKeyValue<K> {
    val itemsList = List(this.count) { index ->
        this.getItem(index)!! as KeyValue<K>
    }
    return max(0, itemsList.indexOfFirst { it.key == key })
}