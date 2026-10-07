package com.swapnochura.devicemanager

import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.util.UUID

class FirebaseRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun signInAnonymously(onReady: (Boolean, String?) -> Unit) {
        if (auth.currentUser != null) {
            onReady(true, auth.currentUser?.uid)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { onReady(true, it.user?.uid) }
            .addOnFailureListener { onReady(false, it.message) }
    }

    fun registerDevice(
        enrollmentId: String?,
        customerId: String?,
        deviceName: String?,
        callback: (Boolean, String?) -> Unit
    ) {
        val deviceId = getStableDeviceId()
        val data = hashMapOf<String, Any?>(
            "deviceId" to deviceId,
            "enrollmentId" to (enrollmentId ?: ""),
            "customerId" to (customerId ?: ""),
            "name" to (deviceName ?: "${Build.MANUFACTURER} ${Build.MODEL}"),
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "androidVersion" to Build.VERSION.RELEASE,
            "sdkInt" to Build.VERSION.SDK_INT,
            "status" to "ENROLLED",
            "managementStatus" to if (DeviceManager.isDeviceOwner(context)) "DEVICE_OWNER" else "NOT_DEVICE_OWNER",
            "lockStatus" to "UNKNOWN",
            "lastSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "authUid" to (auth.currentUser?.uid ?: "")
        )

        db.collection("devices").document(deviceId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { callback(true, deviceId) }
            .addOnFailureListener { callback(false, it.message) }
    }

    fun listenForCommands(deviceId: String, handler: (String, String) -> Unit): ListenerRegistration {
        return db.collection("lockCommands")
            .whereEqualTo("deviceId", deviceId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                for (doc in snapshot.documents) {
                    val command = doc.getString("command") ?: continue
                    handler(doc.id, command)
                }
            }
    }

    fun updateCommand(commandId: String, status: String, message: String? = null) {
        val data = hashMapOf<String, Any?>(
            "status" to status,
            "processedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "deviceMessage" to (message ?: "")
        )
        db.collection("lockCommands").document(commandId)
            .set(data, SetOptions.merge())
    }

    fun updateDeviceStatus(deviceId: String, lockStatus: String, message: String? = null) {
        val data = hashMapOf<String, Any?>(
            "lockStatus" to lockStatus,
            "lastSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "deviceMessage" to (message ?: "")
        )
        db.collection("devices").document(deviceId)
            .set(data, SetOptions.merge())
    }

    private fun getStableDeviceId(): String {
        val prefs = context.getSharedPreferences("swapnochura", Context.MODE_PRIVATE)
        val existing = prefs.getString("device_id", null)
        if (!existing.isNullOrBlank()) return existing
        val id = UUID.randomUUID().toString().replace("-", "")
        prefs.edit().putString("device_id", id).apply()
        return id
    }
}
