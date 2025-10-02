package it.lmqv.livematchcam.converters

import it.lmqv.livematchcam.adapters.LogoItem
import it.lmqv.livematchcam.utils.KeyDescription

fun List<KeyDescription<String>>.toLogoItems() : List<LogoItem> {
    return this.map { x -> LogoItem(x.key) }
}