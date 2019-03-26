package org.nekobasu.core

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.nekobasu.android.extension.MutualNonnullLiveData
import org.nekobasu.android.extension.NonnullLiveData

abstract class SingleUpdateViewModel<T : Any> : ViewModelContract<T>, ViewModel() {

    abstract val initialViewUpdate : T

    private val viewUpdateLiveData : NonnullLiveData<T> by lazy {
        MutualNonnullLiveData(initialViewUpdate)
    }

    protected fun setViewUpdate(viewUpdate : T) {
        (viewUpdateLiveData as MutualNonnullLiveData).setValue(viewUpdate)
    }

    override fun observeViewUpdates(lifecycleOwner: LifecycleOwner?, observer: Observer<T>) {
        if (lifecycleOwner == null) viewUpdateLiveData.observeForever(observer) else viewUpdateLiveData.observe(lifecycleOwner, observer)
    }

    override fun removeViewUpdateObserver(observer: Observer<T>) {
        viewUpdateLiveData.removeObserver(observer)
    }

    override fun getViewUpdate(): T = viewUpdateLiveData.value

    override fun onCleared() {
        super.onCleared()
        clear()
    }

    override fun clear() {}

    override fun deliverResult(result: RequestedResult) {}

    override fun onSave(outBundle: Bundle) {}

    override fun onRestore(inBundle: Bundle) {}
}