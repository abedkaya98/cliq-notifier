package com.example.cliqnotifier.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.cliqnotifier.utils.PrefsManager
import com.example.cliqnotifier.utils.WebhookSender

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PrefsManager(context)
        if (!prefs.isServiceEnabled) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            for (sms in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                val sender = sms.originatingAddress ?: ""
                val body = sms.messageBody ?: ""
                val filter = prefs.senderFilter.trim()

                if (filter.isEmpty() || sender.contains(filter, ignoreCase = true) || body.contains("CliQ", ignoreCase = true)) {
                    WebhookSender.sendSmsWebhook(prefs.webhookUrl, prefs.secretToken, sender, body, sms.timestampMillis) { _, _ -> }
                }
            }
        }
    }
}
