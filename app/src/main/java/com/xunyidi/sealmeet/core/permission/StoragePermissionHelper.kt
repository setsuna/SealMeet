package com.xunyidi.sealmeet.core.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

object StoragePermissionHelper {
    
    /**
     * 检查是否有完整的存储访问权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 检查 MANAGE_EXTERNAL_STORAGE
            Environment.isExternalStorageManager()
        } else {
            // Android 10 及以下检查常规权限
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * 存储权限请求器
 * Android 11+ 需要跳转到设置页面授权
 */
class PermissionRequester(
    private val activity: ComponentActivity,
    private val onResult: (Boolean) -> Unit
) {
    
    // Android 11+ 的设置页面返回处理
    private val settingsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val granted = StoragePermissionHelper.hasStoragePermission(activity)
        onResult(granted)
    }
    
    // Android 10 及以下的运行时权限处理
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        onResult(allGranted)
    }
    
    /**
     * 请求存储权限
     */
    fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 跳转到设置页面
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            settingsLauncher.launch(intent)
        } else {
            // Android 10 及以下请求运行时权限
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            permissionLauncher.launch(permissions)
        }
    }
}
