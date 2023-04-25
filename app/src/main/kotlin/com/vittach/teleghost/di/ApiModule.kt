package com.vittach.teleghost.di

import com.vittach.teleghost.data.telegram.TelegramClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object ApiModule {

    fun create() = module {
        single { TelegramClient(androidContext()) }
    }
}