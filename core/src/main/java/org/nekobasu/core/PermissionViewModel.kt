package org.nekobasu.core

import android.Manifest
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import org.nekobasu.android.extension.NonnullLiveData
import org.nekobasu.core.PermissionChannel.PermissionRequestState
import org.nekobasu.core.PermissionChannel.PermissionRequestState.COMPLETED
import org.nekobasu.core.PermissionChannel.PermissionRequestState.RUNNING
import org.nekobasu.core.PermissionChannel.PermissionState.*
import org.nekobasu.dialogs.InteractionIds

sealed class SystemRequest
object NoRequest : SystemRequest()
data class RequestPermissions(val permissions: Collection<String>, val requestCode: Int) : SystemRequest()
data class RequestDialog(val dialog: DialogUpdateContract) : SystemRequest()
data class OpenApplicationSettings(val callbackId: Int) : SystemRequest()

interface PermissionChecker {
    fun isPermissionGranted(permission: Permission): Boolean
}

class PermissionRequestLiveData(permissions: List<Permission>, permissionChecker: PermissionChecker) : NonnullLiveData<PermissionChannel.PermissionRequest>(PermissionChannel.PermissionRequest(
        permissionStates = createPermissionStates(permissions, permissionChecker),
        state = RUNNING
)) {

    fun completedPermission(permission: Permission, state: PermissionChannel.PermissionState): PermissionRequestState {
        val map = value.permissionStates.toMutableMap().apply {
            put(permission, state)
        }

        value = value.copy(permissionStates = map, state = computeState(map))
        return value.state
    }

    private fun computeState(permissionStates: Map<Permission, PermissionChannel.PermissionState>) =
            if (value.permissionStates.all { it.value != UNKNOWN }) COMPLETED else RUNNING

    init {
        val state = computeState(value.permissionStates)
        if (state != value.state) {
            value = value.copy(state = state)
        }
    }

}

fun createPermissionStates(permissions: List<Permission>, permissionChecker: PermissionChecker): Map<Permission, PermissionChannel.PermissionState> =
        permissions
                .map { it to if (permissionChecker.isPermissionGranted(it)) GRANTED else UNKNOWN }
                .toMap()

private typealias Callback = (Permission, PermissionChannel.PermissionState) -> PermissionRequestState


open class PermissionViewModel(private val permissionChecker: PermissionChecker) : SingleUpdateViewModel<SystemRequest>(), PermissionChannel, DialogCallbackTarget, LifecycleObserver {

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1000
    }

    override val initialViewUpdate: SystemRequest = NoRequest

    private data class PermissionRequest(val permissions: List<Permission>, val liveData:)

    private val requests = mutableListOf<PermissionRequest>()


    override fun needPermission(vararg permissions: Permission): NonnullLiveData<PermissionChannel.PermissionRequest> {
        val liveData = PermissionRequestLiveData(permissions.toList(), permissionChecker)
        if (liveData.value.state != COMPLETED) {
            requestPermission(permissions.toList()) { permission, state ->
                liveData.completedPermission(permission, state)
            }
        }
        return liveData
    }

    private fun requestPermission(permissions: List<Permission>, callback: Callback) {
        requests.add(PermissionRequest(permissions, callback))
        val allPermissions = requests.flatMap { it.permissions }.map { it.systemName }.toSet()
        setViewUpdate(RequestPermissions(allPermissions, PERMISSIONS_REQUEST_CODE))
    }

    // TODO
//    override fun cancelRequest(call: PermissionChannel.Callback) {
//        requests.removeAll { it.callback == call }
//        setViewUpdate(NoRequest)
//    }

    fun onPermissionRequestResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray, shouldShowRationalResults: List<Boolean>) {
        if (permissions.isEmpty() || requestCode != PERMISSIONS_REQUEST_CODE) {
            // No permission was answered so ignore this request
            return
        }

        val permanentDeniedPermissions = permissions.filterIndexed { index, _ ->
            grantResults[index] == PackageManager.PERMISSION_DENIED && !shouldShowRationalResults[index]
        }


        setViewUpdate(NoRequest)
        // Is any permission not accepted?
        if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            val permanentDeniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] == PackageManager.PERMISSION_DENIED && !shouldShowRationalResults[index]
            }
            // Did the user permanently denied any permission
            if (permanentDeniedPermissions.isEmpty()) {
                // We can still prompt the user without the settings dialog so just fail here
                requests.forEach {
                    it.callback.onFailure()
                }
                requests.clear()
            } else {
                // Tell the user that we need that permissions

                // Create a new request Id to keep track of this new UI interaction
                val overlayCallbackId = createRequestId()
                val permission = Permission.values().first {
                    it.systemName == permanentDeniedPermissions.first()
                }
                setViewUpdate(createPermissionDialog(overlayCallbackId, permission))

            }
        } else {
            requests.forEach {
                it.callback.onSuccess()
            }
            requests.clear()
        }
    }

    abstract fun createPermissionDialog(overlayCallbackId: Int, permission: Permission): RequestDialog

    override fun onDialogRemoved(dialogId: DialogId) {
        onCancelPermanentDenied()
        setViewUpdate(NoRequest)
    }

    override fun onDialogInteractionPerformed(dialogId: DialogId, interactionId: InteractionId) {
        setViewUpdate(NoRequest)
        if (interactionId == InteractionIds.NEGATIVE) {
            onCancelPermanentDenied()
        } else {
            openSettings(dialogId)
        }
    }

    override fun onDialogResult(dialogId: DialogId, dialogResult: DialogResult) {}

    // Coming back from the settings should call onStart
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        onReturningFromSettings()
    }

    // Recreated activity after the settings
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        onReturningFromSettings()
    }

    private fun createRequestId() = (Math.random() * 10000).toInt()

    private fun onCancelPermanentDenied() {
        requests.forEach {
            it.callback.onFailure()
        }
        requests.clear()
        setViewUpdate(NoRequest)
    }

    private fun openSettings(callbackId: DialogId) {
        setViewUpdate(OpenApplicationSettings(callbackId.id))
    }

    private fun onReturningFromSettings() {
        val viewDataValue = getViewUpdate()
        when (viewDataValue) {
            is OpenApplicationSettings -> {
                // We open the Settings lets finish the flow
                setViewUpdate(NoRequest)
                requests.forEach {
                    if (missingPermissions(it.permissions).isEmpty()) {
                        it.callback.onSuccess()
                    } else {
                        it.callback.onFailure()
                    }
                }
                requests.clear()
            }
            is RequestDialog -> {
                // We just went away from the dialog lets check again, bu do not fail in case it is not granted
                val iterator = requests.iterator()
                var hasCompleted = false
                while (iterator.hasNext()) {
                    val request = iterator.next()
                    if (missingPermissions(request.permissions).isEmpty()) {
                        iterator.remove()
                        request.callback.onSuccess()
                        hasCompleted = true
                    }
                }
                if (hasCompleted) setViewUpdate(NoRequest)
            }
        }
    }

    private fun missingPermissions(permissions: List<Permission>): List<Permission> = permissions
            .filter { !permissionChecker.isPermissionGranted(it) }
}

data class Permission(val systemName: String) {
    companion object {
        val LOCATION = Permission(Manifest.permission.ACCESS_FINE_LOCATION),
        val READ_CONTACTS = Permission(Manifest.permission.READ_CONTACTS),
        val WRITE_EXTERNAL_STORAGE = Permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}

interface PermissionChannel {

    data class PermissionRequest(val permissionStates: Map<Permission, PermissionChannel.PermissionState>, val state: PermissionRequestState)

    enum class PermissionRequestState {
        RUNNING, COMPLETED
    }

    enum class PermissionState {
        GRANTED, REJECTED, PERMANENT_REJECTED, UNKNOWN
    }

    fun needPermission(vararg permissions: Permission): NonnullLiveData<PermissionRequest>
}
