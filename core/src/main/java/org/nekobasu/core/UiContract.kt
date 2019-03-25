package org.nekobasu.core

import androidx.lifecycle.ViewModel

interface UiContract<T : Any, V, P> where V : ViewModelContract<T>, V : ViewModel {

    val viewModel : V
    fun onViewUpdate(viewUpdate : T)
    fun onCreateViewModel(params : P) : V
    fun getViewModelClass(params: P) : Class<V>
}