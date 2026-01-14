package it.lmqv.livematchcam.extensions

import android.view.View
import it.lmqv.livematchcam.R

fun View.wrapLayout() {
    this.measure(wrapContent(), wrapContent())

    this.layout(
        this.left, this.top,
        this.left + this.measuredWidth,
        this.top + this.measuredHeight)
}

fun View.sizeWidthLayout(measuredWidth: Int) {
    this.minimumWidth = this.context.resources.getDimension(R.dimen.volley_score_board_team_min_width).toInt()
    this.layoutParams.width = measuredWidth

    this.measure(setExactlyContent(measuredWidth), wrapContent())

    this.layout(this.left, this.top,
        this.left + measuredWidth,
        this.top + this.measuredHeight)
}

fun View.equalizeMaxWidthWith(otherView: View) {
    this.measure(wrapContent(), wrapContent())
    otherView.measure(wrapContent(), wrapContent())

    val maxMeasureWidth = maxOf(this.measuredWidth, otherView.measuredWidth)

    this.sizeWidthLayout(maxMeasureWidth)
    otherView.sizeWidthLayout(maxMeasureWidth)
}

private fun wrapContent() : Int {
    return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
}

private fun setExactlyContent(measuredWidth: Int) : Int {
    return View.MeasureSpec.makeMeasureSpec(measuredWidth, View.MeasureSpec.EXACTLY)
}
