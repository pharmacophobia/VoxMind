package com.voxmind.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

object SmsHelper {

    private const val TAG = "VoxMind_SmsHelper"

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun sendSmsReminder(context: Context, recipientPhone: String, messageText: String): Boolean {
        val phone = recipientPhone.trim()
        if (phone.isBlank()) {
            Log.w(TAG, "Empty phone number provided for SMS")
            return false
        }

        if (!hasSmsPermission(context)) {
            Log.w(TAG, "SEND_SMS permission not granted. Cannot dispatch background SMS.")
            return false
        }

        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val fullText = "[VoxMind Reminder] $messageText"
            val parts = smsManager.divideMessage(fullText)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phone, null, fullText, null, null)
            }
            Log.i(TAG, "SMS reminder successfully sent to $phone")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phone: ${e.message}", e)
            false
        }
    }

    fun createSmsIntent(recipientPhone: String, messageText: String): Intent {
        val uri = Uri.parse("smsto:${recipientPhone.trim()}")
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", "[VoxMind Reminder] $messageText")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
