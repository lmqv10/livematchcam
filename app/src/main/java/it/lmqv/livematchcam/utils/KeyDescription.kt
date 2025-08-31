package it.lmqv.livematchcam.utils

interface IKeyDescription<T> {
    val key: T
    val description: String
}

data class KeyDescription<T>(override val key: T, override val description: String) : IKeyDescription<T> {
    override fun toString(): String {
        return description
    }
}

/*interface IKeyValue<K, V> {
    val key: K
    val value: V
}*/

/*data class KeyValue<K, V>(override val key: K, override val value: V) : IKeyValue<K, V> {
    override fun toString(): String {
        return key.toString()
    }
}*/


/*fun <K, T> SpinnerAdapter.getItemPositionByKey(key: K): Int where T : IKeyDescription<K> {
    val itemsList = List(this.count) { index ->
        this.getItem(index)!! as KeyDescription<K>
    }
    return max(0, itemsList.indexOfFirst { it.key == key })
}*/