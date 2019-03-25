package org.nekobasu.core

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

// TODO external intent results and internal framework results
interface Result

interface ViewModelContract<T : Any> : InnerViewModelContract {

    // external methods
    fun observeViewUpdates(lifecycleOwner: LifecycleOwner? = null, observer : Observer<T>)
    fun removeViewUpdateObserver(observer : Observer<T>)
    fun getViewUpdate() : T
    fun clear()
    fun deliverResult(result : Result)


}

interface InnerViewModelContract {

    fun onSave(outBundle : Bundle)
    fun onRestore(inBundle : Bundle)
}

interface UiEvent
interface BackPressHandling : UiEvent {
    fun onBackPress() : Boolean
}