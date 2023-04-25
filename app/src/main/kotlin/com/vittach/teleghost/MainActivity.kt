package com.vittach.teleghost

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import by.kirich1409.viewbindingdelegate.viewBinding
import com.vittach.teleghost.BaseActivity
import com.vittach.teleghost.R
import com.vittach.teleghost.databinding.ActivityMainBinding
import com.vittach.teleghost.di.ViewModelFactory
import com.vittach.teleghost.ui.base.BindingDelegate
import com.vittach.teleghost.ui.utils.viewModel
import me.aartikov.sesame.navigation.NavigationMessageDispatcher
import me.aartikov.sesame.property.PropertyObserver
import org.koin.android.ext.android.getKoin

class MainActivity : BaseActivity(), PropertyObserver {

    override val propertyObserverLifecycleOwner: LifecycleOwner get() = this

    private val binding by viewBinding(ActivityMainBinding::bind)
    private val navigationMessageDispatcher: NavigationMessageDispatcher = getKoin().get()

    private val viewModelFactory = ViewModelFactory()

    private val vm by viewModel { viewModelFactory.createMainActivityViewModel() }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BindingDelegate(this, binding.root).bind(vm)

        if (savedInstanceState == null) {
            vm.appLaunched()
        }
    }

    override fun onPause() {
        navigationMessageDispatcher.pause()
        super.onPause()
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        navigationMessageDispatcher.resume()
    }
}