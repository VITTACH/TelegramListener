package com.vittach.teleghost.ui.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import me.aartikov.sesame.activable.Activable

fun Activable.bindToResumeLifecycle(lifecycle: Lifecycle) {
    lifecycle.addObserver(ResumeActivableLifecycleObserver(this))
}

private class ResumeActivableLifecycleObserver(private val activable: Activable) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        activable.onActive()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        activable.onInactive()
    }
}