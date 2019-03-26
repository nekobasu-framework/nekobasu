package org.nekobasu.android.extension

import androidx.lifecycle.LiveData

abstract class NonnullLiveData<T : Any>(initialValue : T) : LiveData<T>() {
    init {
        value = initialValue
    }

    override fun getValue(): T {
        return super.getValue()!!
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