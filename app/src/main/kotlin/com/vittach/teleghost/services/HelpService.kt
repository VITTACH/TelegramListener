package com.vittach.teleghost.services

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.vittach.teleghost.MainActivity
import com.vittach.teleghost.R
import com.vittach.teleghost.ui.utils.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.aartikov.sesame.property.PropertyObserver


class HelpService : LifecycleService(), PropertyObserver {

    override val propertyObserverLifecycleOwner: LifecycleOwner get() = this

    private lateinit var windowManager: WindowManager
    private lateinit var bottomParams: WindowManager.LayoutParams

    private lateinit var bottomView: LinearLayout

    private var screenHeight = 0
    private var screenWidth = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        runBackgroundTask()

        return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        initScreenUtils()
        initViews()
    }

    override fun onDestroy() {
        windowManager.removeView(bottomView)
        super.onDestroy()
    }

    private fun runBackgroundTask() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        GlobalScope.launch {
            do {
                val tasks = am.getRunningTasks(1)
                for (i in 0 until tasks.size) {
                    val cn = tasks[i].topActivity ?: continue
                    val isMainActivityForeground = cn.className == MainActivity::class.java.name
                    if (bottomView.isVisible != isMainActivityForeground) {
                        withContext(Dispatchers.Main) {
                            bottomView.isVisible = isMainActivityForeground
                        }
                    }
                }
            } while (true)
        }
    }

    private fun initViews() {
        bottomView =
            LayoutInflater.from(this).inflate(R.layout.service_help, null) as LinearLayout

        bottomParams = WindowManager.LayoutParams(
            screenWidth,
            50.dp(),
            TYPE_APPLICATION_OVERLAY,
            FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = 0
            y = 0
            gravity = Gravity.BOTTOM or Gravity.END
        }

        windowManager.addView(bottomView, bottomParams)
    }

    private fun initScreenUtils() {
        val display = windowManager.defaultDisplay
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        screenWidth = display.width
        screenHeight = display.height - statusBarHeight
    }
}