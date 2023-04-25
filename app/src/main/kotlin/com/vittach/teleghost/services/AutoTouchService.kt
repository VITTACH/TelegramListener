package com.vittach.teleghost.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.vittach.teleghost.data.guesture.TouchEvent
import timber.log.Timber.w

var autoTouchService: AutoTouchService? = null

class AutoTouchService : AccessibilityService() {

    private val touchEvents = mutableListOf<TouchEvent>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        w("onServiceConnected")
        autoTouchService = this
    }

    override fun onInterrupt() {
    }

    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
    }

    override fun onUnbind(intent: Intent?): Boolean {
        autoTouchService = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        autoTouchService = null
    }

    fun startEvents(newTouchEvents: List<TouchEvent>, onCompleted: () -> Unit = {}) {
        if (newTouchEvents.isEmpty()) return
        touchEvents.clear()
        touchEvents.addAll(newTouchEvents)
        var strokeDescription: GestureDescription.StrokeDescription? = null
        touchEvents.forEach {
            strokeDescription = it.onEvent(strokeDescription)
        }

        dispatchGesture(
            GestureDescription.Builder().addStroke(strokeDescription!!).build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    onCompleted()
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                }
            },
            null
        )
    }

    fun click(x: Float, y: Float) {
        w("click $x $y")
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 10, 10))
            .build()
        dispatchGesture(gesture, null, null)
    }
}