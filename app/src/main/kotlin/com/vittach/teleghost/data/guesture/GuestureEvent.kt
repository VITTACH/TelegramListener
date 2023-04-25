package com.vittach.teleghost.data.guesture

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point

abstract class TouchEvent {
    var touchDownTime = 0L
    var startTime = 10L
    var duration = 10L
    var isLongPress = false
    lateinit var path: Path

    fun onEvent(
        previousStroke: GestureDescription.StrokeDescription? = null
    ): GestureDescription.StrokeDescription {
        path = Path()
        movePath()
        return previousStroke?.let {
            it.continueStroke(path, startTime, duration, isLongPress)
        } ?: run {
            GestureDescription.StrokeDescription(path, startTime, duration, isLongPress)
        }
    }

    abstract fun movePath()
}

data class Click(val to: Point) : TouchEvent() {
    override fun movePath() {
        path.moveTo(to.x.toFloat(), to.y.toFloat())
    }
}

data class Swipe(val from: Point, val to: Point) : TouchEvent() {
    override fun movePath() {
        path.moveTo(from.x.toFloat(), from.y.toFloat())
        path.lineTo(to.x.toFloat(), to.y.toFloat())
    }
}