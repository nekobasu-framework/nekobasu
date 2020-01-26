package org.nekobasu.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner

abstract class CompositeUiModule<T : Any, V, P>(param: P) : LifecycleUiModule<T, V, P>(param)
        where V : ViewModelContract<T>, V : ViewModel {

    abstract val supportModules: Set<LifecycleUiModule<*, *, *>>

    private val supportModuleViews = mutableMapOf<LifecycleUiModule<*, *, *>, View?>()

    @CallSuper
    override fun attach(lifecycleOwner: LifecycleOwner, viewModelStoreOwner: ViewModelStoreOwner, context: Context) {
        supportModules.forEach { it.attach(lifecycleOwner, viewModelStoreOwner, context) }
        super.attach(lifecycleOwner, viewModelStoreOwner, context)
    }

    @CallSuper
    override fun onRestore(inBundle: Bundle) {
        supportModules.forEach { it.onRestore(inBundle) }
        super.onRestore(inBundle)
    }

    @CallSuper
    override fun onSave(outBundle: Bundle) {
        supportModules.forEach { it.onSave(outBundle) }
        super.onSave(outBundle)
    }

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        supportModules.forEach { supportModuleViews[it] = it.onCreateView(inflater, container, savedInstanceState) }
        return onCreateView(inflater, container, savedInstanceState, supportModuleViews)
    }

    abstract fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, moduleViews: Map<LifecycleUiModule<*, *, *>, View?>): View?

    @CallSuper
    override fun onInitView(view: View, savedInstanceState: Bundle?) {
        supportModuleViews.entries.forEach { (key, value) ->
            if (value != null) key.onInitView(value, savedInstanceState)
        }
    }

    @CallSuper
    override fun deliverResult(result: RequestedResult) : Boolean {
        if (viewModel.deliverResult(result)) {
            return true
        } else {
            supportModules.forEach {
                if (it.deliverResult(result)) return true
            }
            return false
        }
    }
}