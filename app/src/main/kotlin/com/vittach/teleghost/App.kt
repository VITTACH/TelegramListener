package com.vittach.teleghost

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.vittach.teleghost.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        initKoin()
        initLogger()
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@App)
            modules(getAllModules())
        }
    }

    private fun initLogger() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun getAllModules(): List<Module> = listOf(
        SystemModule.create(),
        ApiModule.create(),
        GatewayModule.create(),
        InteractorModule.create(),
        StorageModule.create(),
        RouterModule.create()
    )
}