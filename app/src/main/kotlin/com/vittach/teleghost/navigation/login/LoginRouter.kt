package com.vittach.teleghost.navigation.login

import com.vittach.teleghost.navigation.FragmentNavigator
import com.vittach.teleghost.ui.login.LoginScreen
import me.aartikov.sesame.navigation.NavigationMessage
import me.aartikov.sesame.navigation.NavigationMessageHandler

class LoginRouter(
    private val navigator: FragmentNavigator
) : NavigationMessageHandler {

    override fun handleNavigationMessage(message: NavigationMessage): Boolean {
        when (message) {

            is OpenLoginScreen -> navigator.goTo(LoginScreen())

            else -> return false
        }
        return true
    }
}
