package it.lmqv.livematchcam.utils

interface IOptionItem<T> {
    val key: T
    val description: String
}

data class OptionItem<T>(override val key: T, override val description: String) : IOptionItem<T> {
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


/*fun <K, T> SpinnerAdapter.getItemPositionByKey(key: K): Int where T : IOptionItem<K> {
    val itemsList = List(this.count) { index ->
        this.getItem(index)!! as OptionItem<K>
    }
    return max(0, itemsList.indexOfFirst { it.key == key })
}*/
