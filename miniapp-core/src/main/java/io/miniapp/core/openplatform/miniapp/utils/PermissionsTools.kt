package io.miniapp.core.openplatform.miniapp.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.miniapp.core.R

// Permissions sets
val PERMISSIONS_EMPTY = emptyList<String>()
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
val PERMISSIONS_FOR_AUDIO_IP_CALL = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO)
val PERMISSIONS_FOR_VIDEO_IP_CALL = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
val PERMISSIONS_FOR_VOICE_MESSAGE = arrayOf(Manifest.permission.RECORD_AUDIO)
val PERMISSIONS_FOR_TAKING_PHOTO = arrayOf(Manifest.permission.CAMERA)
val PERMISSIONS_FOR_MEMBERS_SEARCH = arrayOf(Manifest.permission.READ_CONTACTS)
val PERMISSIONS_FOR_ROOM_AVATAR = arrayOf(Manifest.permission.CAMERA)
val PERMISSIONS_FOR_WRITING_FILES = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
val PERMISSIONS_FOR_PICKING_CONTACT = arrayOf(Manifest.permission.READ_CONTACTS)
val PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
val PERMISSIONS_FOR_VOICE_BROADCAST = arrayOf(Manifest.permission.RECORD_AUDIO)

internal object PermissionsTools {

    // This is not ideal to store the value like that, but it works
    private var permissionDialogDisplayed = false

    //
    var currentLambda: ((allGranted: Boolean, deniedPermanently: Boolean) -> Unit)? = null

    /**
     * First boolean is true if all permissions have been granted
     * Second boolean is true if the permission is denied forever AND the permission request has not been displayed.
     * So when the user does not grant the permission and check the box do not ask again, this boolean will be false.
     * Only useful if the first boolean is false
     */
    fun ComponentActivity.registerForPermissionsResult(lambda: (allGranted: Boolean, deniedPermanently: Boolean) -> Unit): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            onPermissionResult(result, lambda)
        }
    }

    fun Fragment.registerForPermissionsResult(lambda: (allGranted: Boolean, deniedPermanently: Boolean) -> Unit): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            onPermissionResult(result, lambda)
        }
    }

    fun onPermissionResult(result: Map<String, Boolean>, lambda: ((allGranted: Boolean, deniedPermanently: Boolean) -> Unit)? ) {
        if (result.keys.all { result[it] == true }) {
            lambda?.invoke(true, /* not used */ false)
        } else {
            if (permissionDialogDisplayed) {
                // A permission dialog has been displayed, so even if the user has checked the do not ask again button, we do
                // not tell the user to open the app settings
                lambda?.invoke(false, false)
            } else {
                // No dialog has been displayed, so tell the user to go to the system setting
                lambda?.invoke(false, true)
            }
        }
        // Reset
        permissionDialogDisplayed = false
    }

    /**
     * Check if the permissions provided in the list are granted.
     * This is an asynchronous method if permissions are requested, the final response
     * is provided in onRequestPermissionsResult(). In this case checkPermissions()
     * returns false.
     * <br></br>If checkPermissions() returns true, the permissions were already granted.
     * The permissions to be granted are given as bit map in permissionsToBeGrantedBitMap (ex: [.PERMISSIONS_FOR_TAKING_PHOTO]).
     * <br></br>permissionsToBeGrantedBitMap is passed as the request code in onRequestPermissionsResult().
     *
     *
     * If a permission was already denied by the user, a popup is displayed to
     * explain why vector needs the corresponding permission.
     *
     * @param permissionsToBeGranted the permissions to be granted
     * @param activity the calling Activity that is requesting the permissions (or fragment parent)
     * @param activityResultLauncher from the calling fragment/Activity that is requesting the permissions
     * @param message message to be displayed BEFORE requesting for the permission
     * @return true if the permissions are granted (synchronous flow), false otherwise (asynchronous flow)
     */
    fun checkPermissions(
        permissionsToBeGranted: Array<String>,
        activity: Activity,
        activityResultLauncher: ActivityResultLauncher<Array<String>>,
        title: String,
        message: String? = null,
        lambda: ((allGranted: Boolean, deniedPermanently: Boolean) -> Unit)? = null
    ): Boolean {

        currentLambda = lambda

        // retrieve the permissions to be granted according to the permission list
        val missingPermissions = permissionsToBeGranted.filter { permission ->
            ContextCompat.checkSelfPermission(activity.applicationContext, permission) == PackageManager.PERMISSION_DENIED
        }

        return if (missingPermissions.isNotEmpty()) {
            permissionDialogDisplayed = !permissionsDeniedPermanently(missingPermissions, activity)

            if (message != null && !permissionDialogDisplayed) {
                // display the dialog with the info text. Do not do it if no system dialog will
                // be displayed
                AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.action_allow) { _, _ ->
                        activityResultLauncher.launch(missingPermissions.toTypedArray())
                    }
                    .setNegativeButton(R.string.action_not_now) {  _, _ ->
                        lambda?.invoke(false, false)
                    }
                    .show()
            } else {
                // some permissions are not granted, ask permissions
                activityResultLauncher.launch(missingPermissions.toTypedArray())
            }
            false
        } else {
            // permissions were granted, start now.
            lambda?.invoke(true, false)
            true
        }
    }

    /**
     * To be call after the permission request.
     *
     * @param permissionsToBeGranted the permissions to be granted
     * @param activity the calling Activity that is requesting the permissions (or fragment parent)
     *
     * @return true if one of the permission has been denied and the user check the do not ask again checkbox
     */
    private fun permissionsDeniedPermanently(
        permissionsToBeGranted: List<String>,
        activity: Activity
    ): Boolean {
        return permissionsToBeGranted
            .filter { permission ->
                ContextCompat.checkSelfPermission(activity.applicationContext, permission) == PackageManager.PERMISSION_DENIED
            }
            .any { permission ->
                // If shouldShowRequestPermissionRationale() returns true, it means that the user as denied the permission, but not permanently.
                // If it return false, it mean that the user as denied permanently the permission
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission).not()
            }
    }

    fun Context.safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (activityNotFoundException: ActivityNotFoundException) {
            activityNotFoundException.printStackTrace()
        }
    }

    fun openAppSettingsPage(activity: Activity) {
        activity.safeStartActivity(
            Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.fromParts("package", activity.packageName, null)
            }
        )
    }
}