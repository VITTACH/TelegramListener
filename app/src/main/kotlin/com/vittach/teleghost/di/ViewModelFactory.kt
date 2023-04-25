package com.vittach.teleghost.di

import com.vittach.teleghost.MainActivityViewModel
import com.vittach.teleghost.ui.login.LoginViewModel
import org.koin.core.component.KoinComponent

class ViewModelFactory : KoinComponent {

    fun createMainActivityViewModel() = MainActivityViewModel()
    fun createLoginViewModel() = LoginViewModel()
}