package it.lmqv.livematchcam.converters

import it.lmqv.livematchcam.adapters.ImageResourceItem
import it.lmqv.livematchcam.utils.OptionItem

fun List<OptionItem<String>>.toImageItems() : List<ImageResourceItem> {
    return this.map { x -> ImageResourceItem(x.key) }
}