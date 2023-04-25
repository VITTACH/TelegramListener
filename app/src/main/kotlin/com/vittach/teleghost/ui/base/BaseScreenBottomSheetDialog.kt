package com.vittach.teleghost.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import me.aartikov.sesame.dialog.DialogObserver
import me.aartikov.sesame.property.PropertyObserver

abstract class BaseScreenBottomSheetDialog<VM : BaseViewModel> :
    BottomSheetDialogFragment(),
    PropertyObserver,
    DialogObserver {

    override val propertyObserverLifecycleOwner: LifecycleOwner get() = viewLifecycleOwner
    override val dialogObserverLifecycleOwner: LifecycleOwner get() = viewLifecycleOwner

    abstract val screenLayout: Int

    protected abstract val vm: VM
    protected open val snackBarAnchor: View get() = requireView()

    protected val bottomSheetBehavior: BottomSheetBehavior<View>
        get() = BottomSheetBehavior.from(requireView().parent as View)

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(screenLayout, container, false).also {
            (it as? ViewGroup)?.isClickable = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.bindToScreen()
        onInitView(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        view?.requestApplyInsets()
    }

    protected fun <T : BaseViewModel> T.bindToScreen() {
        BindingDelegate(
            fragment = this@BaseScreenBottomSheetDialog,
            snackBarAnchor = snackBarAnchor,
            useResumeForActivable = false
        ).bind(this)
    }

    open fun onInitView(savedInstanceState: Bundle?) {
        bottomSheetBehavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}