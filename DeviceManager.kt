package com.swapnochura.devicemanager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

object DeviceManager {

    fun adminComponent(context: Context): ComponentName =
        ComponentName(context, SwapnochuraDeviceAdminReceiver::class.java)

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun lock(context: Context): Result<Unit> {
        return runCatching {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            val admin = adminComponent(context)
            if (!dpm.isAdminActive(admin)) {
                throw SecurityException("Device Admin/DPC সক্রিয় নয়")
            }
            dpm.lockNow()
        }
    }
}
