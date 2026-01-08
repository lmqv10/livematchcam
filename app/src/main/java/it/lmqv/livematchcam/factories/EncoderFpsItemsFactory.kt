package it.lmqv.livematchcam.factories

import it.lmqv.livematchcam.utils.OptionItem

object EncoderFpsItemsFactory {
    fun get() : List<OptionItem<Int>> {
        return listOf(
            OptionItem(20, "20fps"),
            OptionItem(25, "25fps"),
            OptionItem(30, "30fps"),
            OptionItem(60, "60fps")
        )
    }

}