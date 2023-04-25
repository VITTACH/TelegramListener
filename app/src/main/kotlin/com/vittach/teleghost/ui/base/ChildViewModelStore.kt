package com.vittach.teleghost.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import java.util.*

class ChildViewModelStore {

    private val store = ViewModelStore()

    fun <VM : BaseViewModel> create(createViewModel: () -> VM): VM {
        val key = UUID.randomUUID().toString()
        val vm = createViewModel()

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return vm as T
            }
        }
        return ViewModelProvider(store, factory).get(key, vm::class.java)
    }

    fun clear() {
        store.clear()
    }
}