package io.miniapp.core.openplatform.miniapp.utils

import android.Manifest
import android.app.Activity
import android.webkit.PermissionRequest
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting

internal object WebViewPermissionUtils {

    private var permissionRequest: PermissionRequest? = null
    private var selectedPermissions = arrayOf<String>()
    fun promptForPermissions(
            title: String,
            message: String,
            request: PermissionRequest,
            activity: Activity,
            activityResultLauncher: ActivityResultLauncher<Array<String>>
    ) {
        onPermissionsSelected(request.resources, request, activity, activityResultLauncher, title, message)
    }

    private fun onPermissionsSelected(
            permissions: Array<String>,
            request: PermissionRequest,
            activity: Activity,
            activityResultLauncher: ActivityResultLauncher<Array<String>>,
            title: String,
            message: String? = null) {

        permissionRequest = request
        selectedPermissions = permissions

        val requiredAndroidPermissions = selectedPermissions.mapNotNull { permission ->
            webPermissionToAndroidPermission(permission)
        }.toMutableList()

        // When checkPermissions returns false, some of the required Android permissions will
        // have to be requested and the flow completes asynchronously via onPermissionResult
        PermissionsTools.checkPermissions(
            requiredAndroidPermissions.toTypedArray(),
            activity,
            activityResultLauncher,
            title = title,
            message = message
        ) { allGranted, _ ->

            if (allGranted) {
                request.grant(request.resources)
            } else {
                request.deny()
            }
            reset()
        }
    }

    fun onPermissionResult(result: Map<String, Boolean>): Boolean {
        if (permissionRequest == null) {
            PermissionsTools.onPermissionResult(result, PermissionsTools.currentLambda)
            return false
        }
        val grantedPermissions = filterPermissionsToBeGranted(selectedPermissions, result)
        if (grantedPermissions.isNotEmpty()) {
            permissionRequest?.grant(grantedPermissions.toTypedArray())
        } else {
            permissionRequest?.deny()
        }
        reset()
        return true
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun filterPermissionsToBeGranted(selectedWebPermissions: Array<String>, androidPermissionResult: Map<String, Boolean>): List<String> {
        return selectedWebPermissions.filter { webPermission ->
            val androidPermission = webPermissionToAndroidPermission(webPermission)
                    ?: return@filter true // No corresponding Android permission exists
            return@filter androidPermissionResult[androidPermission]
                    ?: return@filter true // Android permission already granted before
        }
    }

    fun reset() {
        permissionRequest = null
        selectedPermissions = arrayOf()
    }

    private fun webPermissionToAndroidPermission(permission: String): String? {
        return when {
            permission == PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
            permission == PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
            permission.startsWith("android.permission.") -> permission
            else -> null
        }
    }
}
