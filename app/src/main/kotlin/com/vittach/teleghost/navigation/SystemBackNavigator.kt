package com.vittach.teleghost.navigation

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.vittach.teleghost.R
import java.util.*

class SystemBackNavigator {

    companion object {
        private const val TRY_TO_QUIT_INTERVAL_MS = 2000L
    }

    private var isTriedToQuit = false

    private var activity: FragmentActivity? = null

    fun attach(activity: FragmentActivity) {
        this.activity = activity
    }

    fun detach() {
        this.activity = null
    }

    fun closeApp() {
        if (activity != null) {
            if (isTriedToQuit) {
                activity!!.finish()
            } else showFinishMessage(activity!!)
            startBackPressedTimer()
        } else {
            throw NullPointerException("SystemBackHelper require activity!")
        }
    }

    private fun showFinishMessage(context: Context) {
        isTriedToQuit = true
        Toast.makeText(
            context,
            context.getString(R.string.quit_toast_msg),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun startBackPressedTimer() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                isTriedToQuit = false
            }
        }, TRY_TO_QUIT_INTERVAL_MS)
    }
}