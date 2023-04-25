package com.vittach.teleghost

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vittach.teleghost.data.utils.PermissionHelper
import com.vittach.teleghost.navigation.FragmentNavigator
import com.vittach.teleghost.navigation.SystemBackNavigator
import com.vittach.teleghost.navigation.login.LoginRouter
import com.vittach.teleghost.navigation.system.SystemRouter
import com.vittach.teleghost.ui.base.BaseScreen
import me.aartikov.sesame.navigation.NavigationMessage
import me.aartikov.sesame.navigation.NavigationMessageHandler
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

open class BaseActivity : AppCompatActivity(), NavigationMessageHandler {

    private val navigator by lazy {
        FragmentNavigator(R.id.screen_container, supportFragmentManager)
    }

    private val systemBackNavigator: SystemBackNavigator by inject()
    private val permissionHelper: PermissionHelper by inject()

    private val systemRouter by inject<SystemRouter> { parametersOf(navigator) }
    private val loginRouter by inject<LoginRouter> { parametersOf(navigator) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionHelper.attach(this)
        systemBackNavigator.attach(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        systemBackNavigator.detach()
        permissionHelper.detach()
    }

    override fun onBackPressed() {
        (navigator.currentScreen as? BaseScreen<*>)?.onBackPressed()
    }

    override fun handleNavigationMessage(message: NavigationMessage): Boolean {
        Timber.d("New navigationMessage = $message")

        return when {
            systemRouter.handleNavigationMessage(message) -> true
            loginRouter.handleNavigationMessage(message) -> true
            else -> {
                Timber.d("Unhandled navigation message %s", message)
                false
            }
        }
    }
}