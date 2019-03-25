package org.nekobasu.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.*

abstract class LifecycleUiModule<T : Any, V, P>(val params: P) :
        UiContract<T, V, P>, InnerViewModelContract, BackPressHandling
        where V : ViewModelContract<T>, V : ViewModel {

    fun attach(lifecycleOwner: LifecycleOwner, viewModelStoreOwner: ViewModelStoreOwner) {
        provider = ViewModelProvider(viewModelStoreOwner, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return onCreateViewModel(params) as T
            }
        })

        viewModel.observeViewUpdates(lifecycleOwner, Observer { viewUpdate ->
            onViewUpdate(viewUpdate)
        })
    }

    private lateinit var provider: ViewModelProvider

    final override val viewModel: V
        get() {
            return provider.get(getViewModelClass(params))
        }

    override fun onCreateViewModel(params: P): V {
        // Default implementation that expects a params only constructor
        val constructor = getViewModelClass(params).constructors.first()
        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(params) as V
    }

    override fun onRestore(inBundle: Bundle) {
        viewModel.onRestore(inBundle)
    }

    override fun onSave(outBundle: Bundle) {
        viewModel.onSave(outBundle)
    }

    override fun onBackPress(): Boolean {
        return if (viewModel is BackPressHandling) (viewModel as BackPressHandling).onBackPress() else false
    }

    abstract fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    abstract fun onInitView(view: View, savedInstanceState: Bundle?)
}

