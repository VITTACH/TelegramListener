package com.vittach.teleghost.ui.utils

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

inline fun <reified VM : ViewModel> AppCompatActivity.viewModel(
    noinline createViewModel: () -> VM
): Lazy<VM> = viewModels(
    factoryProducer = {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return createViewModel() as T
            }
        }
    }
)

inline fun <reified VM : ViewModel> Fragment.viewModel(
    noinline createViewModel: () -> VM
): Lazy<VM> = viewModels(
    factoryProducer = {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return createViewModel() as T
            }
        }
    }
)