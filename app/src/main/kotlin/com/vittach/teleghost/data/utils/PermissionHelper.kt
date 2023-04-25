package com.vittach.teleghost.data.utils

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.appcompat.app.AppCompatActivity
import com.eazypermissions.common.model.PermissionResult
import com.eazypermissions.coroutinespermission.PermissionManager

class PermissionHelper {

    private var activity: AppCompatActivity? = null

    fun attach(activity: AppCompatActivity) {
        this.activity = activity
    }

    fun detach() {
        activity = null
    }

    suspend fun requestPermissions(vararg permissions: String): PermissionResult {
        if (activity == null) {
            throw IllegalStateException("PermissionHelper was not attached to any Activity")
        }

        var permissionResult: PermissionResult
        do {
            permissionResult = PermissionManager.requestPermissions(activity!!, 0, *permissions)
        } while (permissionResult is PermissionResult.ShowRational)
        return permissionResult
    }

    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        var result = true
        permissions.forEach {
            result = result && context.checkCallingOrSelfPermission(it) == PERMISSION_GRANTED
        }
        return result
    }
}