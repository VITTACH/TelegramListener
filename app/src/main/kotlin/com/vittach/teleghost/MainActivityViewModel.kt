package com.vittach.teleghost

import com.vittach.teleghost.navigation.login.OpenLoginScreen
import com.vittach.teleghost.ui.base.BaseViewModel

class MainActivityViewModel(
) : BaseViewModel() {

    override fun onActive() {
        super.onActive()
    }

    fun appLaunched() = safeLaunch {
        navigate(OpenLoginScreen)
    }
}