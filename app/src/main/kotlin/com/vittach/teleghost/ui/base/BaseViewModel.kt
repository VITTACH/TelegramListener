package com.vittach.teleghost.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vittach.teleghost.R
import com.vittach.teleghost.navigation.system.Back
import com.vittach.teleghost.ui.base.widget.SnackbarAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.aartikov.sesame.activable.Activable
import me.aartikov.sesame.dialog.DialogControl
import me.aartikov.sesame.localizedstring.LocalizedString
import me.aartikov.sesame.navigation.NavigationMessage
import me.aartikov.sesame.navigation.NavigationMessageQueue
import me.aartikov.sesame.property.PropertyHost
import me.aartikov.sesame.property.command
import me.aartikov.sesame.property.state
import org.koin.core.component.KoinComponent
import timber.log.Timber
import kotlin.reflect.KMutableProperty0

open class BaseViewModel : ViewModel(), PropertyHost, KoinComponent, Activable by Activable() {

    override val propertyHostScope get() = viewModelScope

    val navigationMessageQueue = NavigationMessageQueue()
    val showSnackbar = command<LocalizedString>()
    val showHidingSnackbar = command<LocalizedString>()
    val showActionSnackbar = command<SnackbarAction>()
    val fullscreenProgress = DialogControl<Unit, Unit>()
    var inFullscreenProgress by state(false)
        protected set

    private val childViewModelStore = ChildViewModelStore()

    open fun navigateBack() = navigate(Back)

    protected fun navigate(message: NavigationMessage) {
        navigationMessageQueue.send(message)
    }

    protected fun safeLaunch(block: suspend () -> Unit) {
        viewModelScope.safeLaunch(block)
    }

    protected fun CoroutineScope.safeLaunch(
        block: suspend () -> Unit
    ): Job {
        return launch {
            try {
                block()
            } catch (e: CancellationException) {
                // do nothing
            } catch (e: Exception) {
                handleException(e)
            }
        }
    }

    protected suspend fun withProgress(
        progressProperty: KMutableProperty0<Boolean>,
        block: suspend () -> Unit
    ) {
        try {
            progressProperty.set(true)
            block()
        } finally {
            progressProperty.set(false)
        }
    }

    protected fun <VM : BaseViewModel> childViewModel(createViewModel: () -> VM): VM {
        return childViewModelStore.create(createViewModel)
    }

    override fun onCleared() {
        super.onCleared()
        childViewModelStore.clear()
    }

    protected fun handleException(throwable: Throwable, action: () -> Unit = {}) {
        Timber.d("new throwable $throwable")
        when {
            else -> {
                val errorMessage = LocalizedString.resource(R.string.error_unexpected)
                showSnackbar(errorMessage)
            }
        }
    }
}
