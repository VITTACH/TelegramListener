package com.vittach.teleghost.ui.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.vittach.teleghost.di.ViewModelFactory
import me.aartikov.sesame.dialog.DialogObserver
import me.aartikov.sesame.form.view.ControlObserver
import me.aartikov.sesame.property.PropertyObserver

abstract class BaseScreen<VM : BaseViewModel>(
    @LayoutRes contentLayoutId: Int,
) : Fragment(contentLayoutId), PropertyObserver, DialogObserver, ControlObserver {

    override val propertyObserverLifecycleOwner: LifecycleOwner get() = viewLifecycleOwner
    override val dialogObserverLifecycleOwner: LifecycleOwner get() = viewLifecycleOwner

    protected val viewModelFactory = ViewModelFactory()
    protected abstract val vm: VM
    protected open val snackBarAnchor: View get() = requireView()
    open val themeSettings: ThemeSettings? = null
    protected open val useResumeForActivable: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.bindToScreen()
    }

    fun <T : BaseViewModel> T.bindToScreen() {
        BindingDelegate(
            fragment = this@BaseScreen,
            snackBarAnchor = snackBarAnchor,
            useResumeForActivable = useResumeForActivable
        ).bind(this)
    }

    fun onBackPressed() {
        vm.navigateBack()
    }
}