package org.nekobasu.android.extension

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

abstract class NonnullLiveData<T : Any>(initialValue : T) : LiveData<T>() {
    init {
        value = initialValue
    }

    override fun getValue(): T {
        return super.getValue()!!
    }
}

class MutableNonnullLiveData<T: Any>(initialValue: T) : NonnullLiveData<T>(initialValue) {
    public override fun postValue(value: T) {
        super.postValue(value)
    }

    public override fun setValue(value: T) {
        super.setValue(value)
    }
}

class NonnullMediatorLiveData<T>(initialValue: T) : MediatorLiveData<T>() {

    private val sources = mutableListOf<LiveData<*>>()

    init {
        value = initialValue
    }

    override fun getValue(): T {
        return super.getValue()!!
    }

    override fun <S : Any?> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        super.addSource(source, onChanged)
        sources.add(source)
    }

    override fun <S : Any> removeSource(toRemote: LiveData<S>) {
        super.removeSource(toRemote)
        sources.remove(toRemote)
    }

    fun removeAll() {
        sources.toList().forEach {
            removeSource(it)
        }
    }
}