package com.swapnochura.devicemanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Build
import com.google.firebase.firestore.ListenerRegistration

class CommandService : Service() {

    private var listener: ListenerRegistration? = null
    private lateinit var repo: FirebaseRepository

    override fun onCreate() {
        super.onCreate()
        repo = FirebaseRepository(this)
        createNotificationChannel()
        startForeground(1001, notification("Swapnochura Device Manager চালু আছে"))

        repo.signInAnonymously { ok, _ ->
            if (!ok) return@signInAnonymously
            val deviceId = getSharedPreferences("swapnochura", MODE_PRIVATE)
                .getString("device_id", null) ?: return@signInAnonymously

            listener = repo.listenForCommands(deviceId) { commandId, command ->
                handleCommand(commandId, command)
            }
        }
    }

    private fun handleCommand(commandId: String, command: String) {
        val deviceId = getSharedPreferences("swapnochura", MODE_PRIVATE)
            .getString("device_id", null) ?: return

        when (command.uppercase()) {
            "LOCK" -> {
                val result = DeviceManager.lock(this)
                if (result.isSuccess) {
                    repo.updateCommand(commandId, "EXECUTED", "Device locked")
                    repo.updateDeviceStatus(deviceId, "LOCKED", "LOCK executed")
                } else {
                    repo.updateCommand(commandId, "FAILED", result.exceptionOrNull()?.message)
                    repo.updateDeviceStatus(deviceId, "ERROR", result.exceptionOrNull()?.message)
                }
            }

            "UNLOCK" -> {
                // Android's normal keyguard cannot be remotely bypassed by a generic DPC.
                repo.updateCommand(commandId, "FAILED", "Remote unlock is not implemented for security reasons")
                repo.updateDeviceStatus(deviceId, "LOCKED", "Remote unlock requires a supported OEM/enterprise policy")
            }

            "PING" -> {
                repo.updateCommand(commandId, "EXECUTED", "Device online")
                repo.updateDeviceStatus(deviceId, "ONLINE", "PING")
            }

            else -> repo.updateCommand(commandId, "FAILED", "Unknown command: $command")
        }
    }

    override fun onDestroy() {
        listener?.remove()
        listener = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    "swapnochura_device_manager",
                    "Swapnochura Device Manager",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun notification(text: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "swapnochura_device_manager")
                .setContentTitle("Swapnochura Device Manager")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Swapnochura Device Manager")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build()
        }
    }
}
