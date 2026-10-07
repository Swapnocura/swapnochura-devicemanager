package com.swapnochura.devicemanager

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var repo: FirebaseRepository
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repo = FirebaseRepository(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 50, 40, 40)
        }

        val title = TextView(this).apply {
            text = "স্বপ্নচূড়া Device Manager"
            textSize = 25f
        }

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }

        val start = Button(this).apply {
            text = "Device Service চালু করুন"
            setOnClickListener { startDeviceService() }
        }

        root.addView(title)
        root.addView(status)
        root.addView(start)
        setContentView(root)

        requestNotificationPermission()
        showStatus()
        registerFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        registerFromIntent(intent)
    }

    private fun registerFromIntent(intent: Intent?) {
        val enrollmentId = intent?.getStringExtra("enrollmentId")
        val customerId = intent?.getStringExtra("customerId")
        val deviceName = intent?.getStringExtra("deviceName")

        repo.signInAnonymously { ok, message ->
            if (!ok) {
                status.text = "Firebase Auth ব্যর্থ: $message"
                return@signInAnonymously
            }

            repo.registerDevice(enrollmentId, customerId, deviceName) { success, value ->
                status.text = if (success) {
                    "Device Firebase-এ নিবন্ধিত।\nDevice ID: $value\nDevice Owner: ${DeviceManager.isDeviceOwner(this)}"
                } else {
                    "Device registration ব্যর্থ: $value"
                }
                if (success) startDeviceService()
            }
        }
    }

    private fun startDeviceService() {
        val intent = Intent(this, CommandService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        status.text = "Command service চালু হয়েছে।"
    }

    private fun showStatus() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        status.text = buildString {
            append("Device Owner: ${dpm.isDeviceOwnerApp(packageName)}\n")
            append("Manufacturer: ${Build.MANUFACTURER}\n")
            append("Model: ${Build.MODEL}\n")
            append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }
}
