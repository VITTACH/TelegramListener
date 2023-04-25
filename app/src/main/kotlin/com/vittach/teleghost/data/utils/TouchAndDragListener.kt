package com.vittach.teleghost.data.utils

import android.annotation.SuppressLint
import android.graphics.Point
import android.view.MotionEvent
import android.view.View
import com.vittach.teleghost.data.guesture.Click
import com.vittach.teleghost.data.guesture.TouchEvent
import com.vittach.teleghost.data.guesture.Swipe
import kotlin.math.abs

class TouchAndDragListener(
    private val performTouchEvent: (List<TouchEvent>) -> Unit
) : View.OnTouchListener {

    companion object {
        private const val TOUCH_TOLERANCE = 4f
        private const val PRESS_DURATION = 500
    }

    private var isDrag = false
    private var startPoint: Point? = null
    private var touchTime = 0L

    private val touchEvents = mutableListOf<TouchEvent>()

    private val isPressed: Boolean
        get() = System.currentTimeMillis() - touchTime > PRESS_DURATION

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val x = e.rawX.toInt()
        val y = e.rawY.toInt()
        when (e.action) {
            MotionEvent.ACTION_UP -> {
                val endPoint = Point(x, y)
                val event = if (!isDrag) {
                    Click(endPoint).apply { isLongPress = isPressed }
                } else {
                    Swipe(startPoint!!, endPoint)
                }.apply {
                    touchDownTime = touchTime
                }

                touchEvents.add(event)
                performTouchEvent(touchEvents)
            }

            MotionEvent.ACTION_DOWN -> {
                touchEvents.clear()
                startPoint = Point(x, y)
                touchTime = System.currentTimeMillis()
                isDrag = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - startPoint!!.x)
                val dy = abs(y - startPoint!!.y)
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    isDrag = true
                }
            }
        }

        return false
    }
}