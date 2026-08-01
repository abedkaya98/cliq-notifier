package com.example.cliqnotifier.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.cliqnotifier.utils.WebhookSender

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val prefs = context.getSharedPreferences("CliQSettings", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("service_enabled", false)
            if (!isEnabled) return

            val webhookUrl = prefs.getString("webhook_url", "") ?: ""
            val secretToken = prefs.getString("secret_token", "") ?: ""
            val rawFilter = prefs.getString("sender_filter", "") ?: ""

            if (webhookUrl.isEmpty() || rawFilter.isEmpty()) return

            val filters = rawFilter.split(",").map { it.trim().lowercase() }
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {
                val sender = sms.originatingAddress ?: ""
                val body = sms.messageBody ?: ""
                val timestamp = sms.timestampMillis

                val isMatched = filters.any { filter -> sender.lowercase().contains(filter) }

                if (isMatched) {
                    WebhookSender.sendSmsToWebhook(
                        context = context,
                        webhookUrl = webhookUrl,
                        secretToken = secretToken,
                        sender = sender,
                        messageBody = body,
                        timestamp = timestamp
                    )
                }
            }
        }
    }
}
