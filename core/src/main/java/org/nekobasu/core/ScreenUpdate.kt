package org.nekobasu.core

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

const val NO_RESULT_REQUESTED = -1
const val STANDALONE_TASK = "STANDALONE_TASK"
const val NO_ID = "NO_ID"

@Parcelize
data class ScreenUpdate(
        var params: Param,
        var paramsUpdatedByRequest: Boolean = false,
        var lastRequestForResultId: Int = NO_RESULT_REQUESTED,
        var screenId: String = NO_ID,
        var taskId: String = STANDALONE_TASK,
        var result: Result = NoResult
) : Parcelable {
    fun tag() = "$screenId#$taskId"
    fun isPartOfATask() = taskId != STANDALONE_TASK
}
