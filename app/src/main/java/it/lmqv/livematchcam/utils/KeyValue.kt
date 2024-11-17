package it.lmqv.livematchcam.utils

data class KeyValue<T>(val key: T, val description: String) {
    override fun toString(): String {
        return description
    }
}