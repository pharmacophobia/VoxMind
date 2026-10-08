package com.voxmind.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object EmailHelper {

    fun createEmailIntent(recipientEmail: String, subject: String, bodyText: String): Intent {
        val uri = Uri.parse("mailto:${recipientEmail.trim()}")
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun openEmailClient(context: Context, recipientEmail: String, subject: String, bodyText: String): Boolean {
        return try {
            val intent = createEmailIntent(recipientEmail, subject, bodyText)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
