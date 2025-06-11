package io.miniapp.core.openplatform.miniapp.utils

import android.app.Activity
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.core.content.ContextCompat

internal object CheckWebViewPermissionsUseCase {

    /**
     * Checks if required WebView permissions are already granted system level.
     * @param activity the calling Activity that is requesting the permissions (or fragment parent)
     * @param request WebView permission request of onPermissionRequest function
     * @return true if WebView permissions are already granted, false otherwise
     */
    fun execute(activity: Activity, request: PermissionRequest): Boolean {
        return request.resources.all {
            when (it) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    PERMISSIONS_FOR_AUDIO_IP_CALL.all { permission ->
                        ContextCompat.checkSelfPermission(activity.applicationContext, permission) == PackageManager.PERMISSION_GRANTED
                    }
                }
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    PERMISSIONS_FOR_VIDEO_IP_CALL.all { permission ->
                        ContextCompat.checkSelfPermission(activity.applicationContext, permission) == PackageManager.PERMISSION_GRANTED
                    }
                }
                else -> {
                    false
                }
            }
        }
    }
}
