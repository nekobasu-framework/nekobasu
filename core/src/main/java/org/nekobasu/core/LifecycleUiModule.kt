package org.nekobasu.core

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.*

abstract class LifecycleUiModule<T : Any, V, P>(val param: P) :
        UiContract<T, V, P>, InnerViewModelContract,
        BackPressHandling
        where V : ViewModelContract<T>, V : ViewModel {

    var isAttached = false
        private set

    open fun attach(lifecycleOwner: LifecycleOwner, viewModelStoreOwner: ViewModelStoreOwner, context: Context) {
        this.context = context
        provider = ViewModelProvider(viewModelStoreOwner, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return onCreateViewModel(param) as T
            }
        })
        viewModel.observeViewUpdates(lifecycleOwner, Observer { viewUpdate ->
            onViewUpdate(viewUpdate)
        })
        isAttached = true
    }

    private lateinit var provider: ViewModelProvider
    override lateinit var context: Context

    final override val viewModel: V by lazy { provider.get(getViewModelClass(param)) }

    override fun onCreateViewModel(params: P): V {
        // Default implementation that expects a params only constructor
        val constructor = getViewModelClass(params).constructors.first()
        return if (constructor.parameterTypes.size == 1) constructor.newInstance(params) as V else constructor.newInstance() as V
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
    override fun deliverResult(result: RequestedResult) : Boolean {
        return viewModel.deliverResult(result)
    }
}

