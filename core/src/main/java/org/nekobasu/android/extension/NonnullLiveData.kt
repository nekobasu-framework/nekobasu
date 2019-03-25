package org.nekobasu.android.extension

import androidx.lifecycle.LiveData

abstract class NonnullLiveData<T : Any>(private val initialValue : T) : LiveData<T>() {
    override fun getValue(): T {
        return super.getValue() ?: initialValue
    }
}

class MutualNonnullLiveData<T: Any>(initialValue: T) : NonnullLiveData<T>(initialValue) {
    public override fun postValue(value: T) {
        super.postValue(value)
    }

    public override fun setValue(value: T) {
        super.setValue(value)
    }
}