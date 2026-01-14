package it.lmqv.livematchcam.extensions

import android.text.InputFilter

fun String.applyInputFilters(filters: Array<InputFilter>): String {
    val editable = android.text.SpannableStringBuilder(this)

    filters.forEach { filter ->
        when (filter) {
            is InputFilter.LengthFilter -> {
                if (editable.length > filter.max) {
                    editable.delete(filter.max, editable.length)
                }
            }
            is InputFilter.AllCaps -> {
                for (i in 0 until editable.length) {
                    val char = editable[i]
                    if (char.isLowerCase()) {
                        editable.replace(i, i + 1, char.uppercaseChar().toString())
                    }
                }
            }
        }
    }

    return editable.toString()
}