package com.zaki.knotvideoeditor.utils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.ArrayList

/**
 * Developer : Mohammad Zaki
 * Created On : 26-07-2020
 */

object PermissionHelper {

    fun checkHasPermission(context: Activity?, permissions: Array<String>?): ArrayList<String> {
        val permissionList: ArrayList<String> = ArrayList()
        if (isMarshmallow && context != null && permissions != null) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(permission)
                }
            }
        }
        return permissionList
    }

    fun isPermissionBlocked(context: Activity?, permissions: ArrayList<String>?): Boolean {
        if (isMarshmallow && context != null && permissions != null) {
            for (permission in permissions) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                    return true
                }
            }
        }
        return false
    }

     fun callPermissionSettings(mActivity:Fragment) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", "com.zaki.knotvideoeditor", null)
        intent.data = uri
        mActivity.startActivityForResult(intent, 300)
    }

    private val isMarshmallow: Boolean
        get() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) or (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1)
}