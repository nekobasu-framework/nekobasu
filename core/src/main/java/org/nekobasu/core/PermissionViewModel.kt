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
import java.lang.IllegalStateException

sealed class SystemRequest
object NoRequest : SystemRequest()
data class RequestPermissions(val permissions: Collection<String>, val requestCode: Int) : SystemRequest()
data class RequestDialog(val dialog: DialogUpdateContract) : SystemRequest()
data class OpenApplicationSettings(val callbackId: Int) : SystemRequest()

interface PermissionChecker {
    fun isPermissionGranted(permission: Permission): Boolean
}

class PermissionRequestLiveData(permissionStates : Map<Permission, PermissionChannel.PermissionState>) : NonnullLiveData<PermissionChannel.PermissionRequest>(PermissionChannel.PermissionRequest(
        permissionStates = permissionStates,
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
            if (permissionStates.all { it.value != UNKNOWN }) COMPLETED else RUNNING

    fun unknownPermissions(): List<Permission> = value.permissionStates.filter { it.value == UNKNOWN }.map { it.key }

    fun recheckPermissions(permissionChecker: PermissionChecker) {
        if (value.state == RUNNING) {
            val newStates = value.permissionStates.map {
                val isGranted = permissionChecker.isPermissionGranted(it.key)
                val permissionState = it.value
                when  {
                    isGranted -> it.key to GRANTED
                    !isGranted && permissionState == GRANTED -> it.key to REJECTED
                    else -> it.key to it.value
                }
            }.toMap()
            value = PermissionChannel.PermissionRequest(newStates, computeState(newStates))
        }
    }

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


open class PermissionViewModel(private val permissionChecker: PermissionChecker) : SingleUpdateViewModel<SystemRequest>(), PermissionChannel, LifecycleObserver {

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1000
    }

    override val initialViewUpdate: SystemRequest = NoRequest

    private data class PermissionRequest(val permissions: List<Permission>, val liveData: PermissionRequestLiveData)

    private val requests = mutableListOf<PermissionRequest>()


    override fun needPermission(vararg permissions: Permission): NonnullLiveData<PermissionChannel.PermissionRequest> {
        val liveData = PermissionRequestLiveData(createPermissionStates(permissions.toList(), permissionChecker))
        if (liveData.value.state != COMPLETED) {
            requestPermission(permissions.toList(), liveData)
        }
        return liveData
    }

    private fun requestPermission(permissions: List<Permission>, liveData: PermissionRequestLiveData) {
        requests.add(PermissionRequest(permissions, liveData))
        val allPermissions = requests.flatMap { it.liveData.unknownPermissions() }.map { it.systemName }.toSet()
        setViewUpdate(RequestPermissions(allPermissions, PERMISSIONS_REQUEST_CODE))
    }


    fun onPermissionRequestResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray, shouldShowRationalResults: List<Boolean>) {

        if (permissions.isEmpty() || requestCode != PERMISSIONS_REQUEST_CODE) {
            // No permission was answered so ignore this request
            return
        }

        permissions.forEachIndexed { index, permissionString ->
            val result = grantResults[index]
            val newState = when {
                result == PackageManager.PERMISSION_DENIED && !shouldShowRationalResults[index] -> PERMANENT_REJECTED
                result == PackageManager.PERMISSION_DENIED -> REJECTED
                result == PackageManager.PERMISSION_GRANTED -> GRANTED
                else -> throw IllegalStateException("Permission ($permissionString) request returned with unknown state: $result")
            }

            requests.forEach {
                val permission = Permission(permissionString)
                if (it.permissions.contains(permission)) {
                    it.liveData.completedPermission(permission, newState)
                }
            }
        }

        computeNewState()
    }

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

    private fun openSettings(callbackId: DialogId) {
        setViewUpdate(OpenApplicationSettings(callbackId.id))
    }

    private fun onReturningFromSettings() {
        requests.forEach { it.liveData.recheckPermissions(permissionChecker) }
        computeNewState()
    }

    private fun computeNewState() {
        val deleteRequests = requests.filter { it.liveData.value.state == COMPLETED }
        requests.removeAll(deleteRequests)

        val allPermissions = requests.flatMap { it.liveData.unknownPermissions() }.map { it.systemName }.toSet()
        if (allPermissions.isEmpty()) {
            setViewUpdate(NoRequest)
        } else {
            setViewUpdate(RequestPermissions(allPermissions, PERMISSIONS_REQUEST_CODE))
        }
    }

    private fun missingPermissions(permissions: List<Permission>): List<Permission> = permissions
            .filter { !permissionChecker.isPermissionGranted(it) }
}

data class Permission(val systemName: String) {
    companion object {
        // TODO complete this list
        val LOCATION = Permission(Manifest.permission.ACCESS_FINE_LOCATION)
        val READ_CONTACTS = Permission(Manifest.permission.READ_CONTACTS)
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
