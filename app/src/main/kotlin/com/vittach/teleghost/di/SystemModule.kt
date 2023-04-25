package com.vittach.teleghost.di

import com.vittach.teleghost.navigation.SystemBackNavigator
import com.vittach.teleghost.data.utils.PermissionHelper
import me.aartikov.sesame.navigation.NavigationMessageDispatcher
import org.koin.dsl.module

object SystemModule {

    fun create() = module {
        single { NavigationMessageDispatcher() }
        single { SystemBackNavigator() }
        single { PermissionHelper() }
    }

}