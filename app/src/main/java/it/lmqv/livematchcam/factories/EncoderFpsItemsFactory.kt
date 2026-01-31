package it.lmqv.livematchcam.factories

import it.lmqv.livematchcam.utils.OptionItem

object EncoderItemsFactory {
    fun getFps() : List<OptionItem<Int>> {
        return listOf(
            OptionItem(20, "20fps"),
            OptionItem(25, "25fps"),
            OptionItem(30, "30fps"),
            OptionItem(60, "60fps")
        )
    }

    fun getBitrate() : List<OptionItem<Int>> {
        return listOf(
            OptionItem(4000 * 1000, "4000 kpps"),
            OptionItem(5000 * 1000, "5000 kpps"),
            OptionItem(6000 * 1000, "6000 kpps"),
            OptionItem(7000 * 1000, "7000 kpps"),
            OptionItem(8000 * 1000, "8000 kpps"),

        )
    }

}