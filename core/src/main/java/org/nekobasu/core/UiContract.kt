package org.nekobasu.core

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.ViewModel

interface UiContract<T : Any, V, P> where V : ViewModelContract<T>, V : ViewModel {

    val viewModel : V
    val context : Context

    fun onViewUpdate(viewUpdate : T)
    fun onCreateViewModel(params : P) : V
    fun getViewModelClass(params: P) : Class<V>
}

data class RequestedResult(val requestId : Int, val result: Result)

interface InterUiContract {
    fun deliverResult(result : RequestedResult) : Boolean
    fun updateParams(param: Parcelable)
}