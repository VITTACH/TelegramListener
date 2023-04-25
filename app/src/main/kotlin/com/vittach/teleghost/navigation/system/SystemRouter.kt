package com.vittach.teleghost.navigation.system

import com.vittach.teleghost.navigation.FragmentNavigator
import com.vittach.teleghost.navigation.SystemBackNavigator
import me.aartikov.sesame.navigation.NavigationMessage
import me.aartikov.sesame.navigation.NavigationMessageHandler

class SystemRouter(
    private val navigator: FragmentNavigator,
    private val systemBackNavigator: SystemBackNavigator
) : NavigationMessageHandler {

    override fun handleNavigationMessage(message: NavigationMessage): Boolean {
        when (message) {

            is Back -> {
                val success = navigator.back()
                if (!success) systemBackNavigator.closeApp()
            }

            else -> return false
        }
        return true
    }
}
