package com.vittach.teleghost.ui.login

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.vittach.teleghost.R
import com.vittach.teleghost.data.telegram.TelegramClient
import com.vittach.teleghost.databinding.LoginScreenBinding
import com.vittach.teleghost.services.HelpService
import com.vittach.teleghost.services.TeleGhostService
import com.vittach.teleghost.services.TeleGhostService.Companion.AVATAR_PATH
import com.vittach.teleghost.ui.base.BaseScreen
import com.vittach.teleghost.ui.utils.doOnClick
import com.vittach.teleghost.ui.utils.viewModel
import org.koin.android.ext.android.inject

/**
 * Created by VITTACH on 23.04.2022.
 */
class LoginScreen : BaseScreen<LoginViewModel>(R.layout.login_screen) {

    companion object {
        private val TELEGHOST_SERVICE_CLASS = TeleGhostService::class.java
        private val HELP_SERVICE_CLASS = HelpService::class.java
        private const val ACTION_MANAGE_OVERLAY_PERMISSION_CODE = 2000
    }

    override val vm by viewModel { viewModelFactory.createLoginViewModel() }

    private val binding by viewBinding(LoginScreenBinding::bind)

    private val client by inject<TelegramClient>()

    private val mActivity by lazy { requireActivity() }

    private var isFirstPermissionCheck = true

    private lateinit var teleGhostServiceIntent: Intent
    private lateinit var helpServiceIntent: Intent

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        teleGhostServiceIntent = Intent(mActivity, TELEGHOST_SERVICE_CLASS)
        helpServiceIntent = Intent(mActivity, HELP_SERVICE_CLASS)

        initViews()
    }

    override fun onResume() {
        super.onResume()

        if (isFirstPermissionCheck) {
            isFirstPermissionCheck = false
            checkOverlayPermission()
        } else {
            binding.root.postDelayed({ checkOverlayPermission() }, 1000)
        }
    }

    private fun initViews() = with(binding) {

        requestCodeBtn.doOnClick { client.setPhoneNumber(phoneInput.text.toString()) }
        sendCodeBtn.doOnClick { client.setAuthCode(codeInput.text.toString()) }
        logOutBtn.doOnClick { client.logout() }

        // TODO: () remove it
        apiIdInput.setText("19713848")
        apiHashInput.setText("60c8fa9a2362b6788e967d6da13739ac")

        startBtn.doOnClick {
            client.setApiIdAndHash(
                id = apiIdInput.text.toString().toInt(),
                hash = apiHashInput.text.toString()
            )
        }

        recordBtn.doOnClick {
            val intent = Intent(mActivity, TELEGHOST_SERVICE_CLASS)
                .setAction(TeleGhostService.ACTION_RECORD)
            mActivity.startService(intent)
        }
        avatarBtn.doOnClick {
            val intent = Intent(mActivity, TELEGHOST_SERVICE_CLASS)
                .setAction(TeleGhostService.ACTION_AVATAR)
                .putExtra(AVATAR_PATH, "file:///android_asset/sample.gif")
            mActivity.startService(intent)
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(mActivity)) {
            askOverlayPermission()
        } else {
            isFirstPermissionCheck = true
            if (!checkTouchPermission()) {
                tryToStartHelpService()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } else {
                tryToStartGhostService()
            }
        }
    }

    private fun tryToStartGhostService() {
        if (!isServiceRunning(TELEGHOST_SERVICE_CLASS)) {
            mActivity.startService(teleGhostServiceIntent)
            mActivity.stopService(helpServiceIntent)
        }
    }

    private fun tryToStartHelpService() {
        if (!isServiceRunning(HELP_SERVICE_CLASS)) {
            mActivity.startService(helpServiceIntent)
        }
    }

    // Custom method to determine whether a service is running
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager =
            mActivity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Loop through the running services
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun checkTouchPermission(): Boolean {
        val serviceId = getString(R.string.accessibility_service_id)
        val manager =
            mActivity.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list =
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (id in list) {
            if (serviceId == id.id) {
                return true
            }
        }
        return false
    }

    private fun askOverlayPermission() {
        startActivityForResult(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${mActivity.packageName}")
            ),
            ACTION_MANAGE_OVERLAY_PERMISSION_CODE
        )
    }
}