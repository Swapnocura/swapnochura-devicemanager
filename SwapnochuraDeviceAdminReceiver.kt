package com.swapnochura.devicemanager

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class SwapnochuraDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Swapnochura Device Manager সক্রিয় হয়েছে", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Device Manager নিষ্ক্রিয় হয়েছে", Toast.LENGTH_SHORT).show()
    }
}
