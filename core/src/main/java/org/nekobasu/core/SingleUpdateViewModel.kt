package org.nekobasu.core

import android.os.Bundle
import androidx.lifecycle.*
import org.nekobasu.android.extension.MutableNonnullLiveData
import org.nekobasu.android.extension.NonnullLiveData
import org.nekobasu.android.extension.NonnullMediatorLiveData

abstract class SingleUpdateViewModel<T : Any> : ViewModelContract<T>, ViewModel() {

    abstract val initialViewUpdate : T

    private val viewUpdateLiveData : NonnullLiveData<T> by lazy {
        MutableNonnullLiveData(initialViewUpdate)
    }

    private val viewUpdtaeMediatorLiveData : NonnullMediatorLiveData<T> by lazy {
        NonnullMediatorLiveData(initialViewUpdate).apply {
            addSource(
                viewUpdateLiveData
            ) { viewUpdtaeMediatorLiveData.value = it }
        }
    }

    protected fun <S : Any?> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        viewUpdtaeMediatorLiveData.addSource(source, onChanged)
    }

    protected fun <S : Any> removeSource(toRemote: LiveData<S>) {
        viewUpdtaeMediatorLiveData.removeSource(toRemote)
    }

    protected fun setViewUpdate(viewUpdate : T) {
        (viewUpdateLiveData as MutableNonnullLiveData).value = viewUpdate
    }

    override fun observeViewUpdates(lifecycleOwner: LifecycleOwner?, observer: Observer<T>) {
        if (lifecycleOwner == null) viewUpdtaeMediatorLiveData.observeForever(observer) else viewUpdtaeMediatorLiveData.observe(lifecycleOwner, observer)
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