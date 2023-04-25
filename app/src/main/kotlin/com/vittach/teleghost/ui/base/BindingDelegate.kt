package com.vittach.teleghost.ui.base

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.vittach.teleghost.R
import com.vittach.teleghost.ui.utils.bindToResumeLifecycle
import com.google.android.material.snackbar.Snackbar
import me.aartikov.sesame.activable.bindToLifecycle
import me.aartikov.sesame.dialog.DialogObserver
import me.aartikov.sesame.dialog.show
import me.aartikov.sesame.localizedstring.LocalizedString
import me.aartikov.sesame.navigation.NavigationMessageDispatcher
import me.aartikov.sesame.navigation.bind
import me.aartikov.sesame.property.PropertyObserver
import org.koin.core.component.KoinComponent

class BindingDelegate(
    override val propertyObserverLifecycleOwner: LifecycleOwner,
    override val dialogObserverLifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val dispatcherNode: Any,
    private val snackBarAnchor: View,
    private val useResumeForActivable: Boolean,
) : PropertyObserver, DialogObserver, KoinComponent {

    companion object {
        private const val SNACKBAR_LENGTH = 4000
    }

    constructor(activity: AppCompatActivity, snackBarAnchor: View) : this(
        activity,
        activity,
        activity,
        activity,
        snackBarAnchor,
        useResumeForActivable = false
    )

    constructor(fragment: Fragment, snackBarAnchor: View, useResumeForActivable: Boolean) : this(
        fragment.viewLifecycleOwner,
        fragment.viewLifecycleOwner,
        fragment.requireContext(),
        fragment,
        snackBarAnchor,
        useResumeForActivable
    )

    private val navigationMessageDispatcher: NavigationMessageDispatcher = getKoin().get()

    fun bind(viewModel: BaseViewModel) {
        viewModel.showSnackbar bind { showSimpleSnackbar(snackBarAnchor, it) }
        viewModel.showActionSnackbar bind {
            showActionSnackbar(
                snackBarAnchor,
                it.message,
                it.actionTitle,
                it.action
            )
        }
        viewModel.showHidingSnackbar bind { showActionSnackbar(snackBarAnchor, it) }
        viewModel.navigationMessageQueue.bind(
            navigationMessageDispatcher,
            dispatcherNode,
            propertyObserverLifecycleOwner
        )

        viewModel::inFullscreenProgress bind {
            if (it) viewModel.fullscreenProgress.show()
            else viewModel.fullscreenProgress.dismiss()
        }
        if (useResumeForActivable) {
            viewModel.bindToResumeLifecycle(propertyObserverLifecycleOwner.lifecycle)
        } else {
            viewModel.bindToLifecycle(propertyObserverLifecycleOwner.lifecycle)
        }
    }

    private fun showSimpleSnackbar(snackBarAnchor: View, text: LocalizedString) {
        Snackbar.make(snackBarAnchor, text.resolve(context), Snackbar.LENGTH_LONG)
            .apply {
                view.background = ContextCompat.getDrawable(context, R.drawable.bg_snackbar)
            }
            .show()
    }

    private fun showActionSnackbar(
        snackBarAnchor: View,
        text: LocalizedString,
        actionText: LocalizedString = LocalizedString.resource(R.string.hide),
        action: (() -> Unit)? = null
    ) {

        Snackbar.make(snackBarAnchor, text.resolve(context), SNACKBAR_LENGTH)
            .apply {
                view.background = ContextCompat.getDrawable(context, R.drawable.bg_snackbar)
                setAction(actionText.resolve(context)) {
                    if (action != null) {
                        action.invoke()
                    } else {
                        this.dismiss()
                    }
                }
            }
            .show()
    }
}