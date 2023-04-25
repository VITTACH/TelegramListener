package com.vittach.teleghost.di

import com.vittach.teleghost.domain.GifToBitmapsInteractor
import org.koin.dsl.module

object InteractorModule {

    fun create() = module {
        single { GifToBitmapsInteractor() }
    }
}