package org.nekobasu.core

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.android.parcel.Parcelize

class OnActivityResult(val requestCode: Int, val resultCode: Int, oldIntent: Intent?) : Intent(oldIntent), Result

interface Result: Parcelable

@Parcelize
object NoResult : Result

interface ViewModelContract<T : Any> : InnerViewModelContract {

    // external methods
    fun observeViewUpdates(lifecycleOwner: LifecycleOwner? = null, observer : Observer<T>)
    fun removeViewUpdateObserver(observer : Observer<T>)
    fun getViewUpdate() : T
    fun clearViewModel()
}

interface InnerViewModelContract {
    /**
     * Return true if consumed
     */
    fun deliverResult(result : RequestedResult) : Boolean
    fun onSave(outBundle : Bundle)
    fun onRestore(inBundle : Bundle)
}

interface UiEvent
interface BackPressHandling : UiEvent {
    fun onBackPress() : Boolean
}