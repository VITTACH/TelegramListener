package com.vittach.teleghost.di

import com.vittach.teleghost.navigation.FragmentNavigator
import com.vittach.teleghost.navigation.login.LoginRouter
import com.vittach.teleghost.navigation.system.SystemRouter
import org.koin.dsl.module

object RouterModule {

    fun create() = module {
        factory { (navigator: FragmentNavigator) -> SystemRouter(navigator, get()) }
        factory { (navigator: FragmentNavigator) -> LoginRouter(navigator) }
    }
}