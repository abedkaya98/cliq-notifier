package com.example.cliqnotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.cliqnotifier.models.BankTemplate
import com.example.cliqnotifier.utils.TemplateParser
import com.example.cliqnotifier.utils.WebhookSender
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences("CliQSettings", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("service_enabled", false)
        if (!isEnabled) return

        val webhookUrl = prefs.getString("webhook_url", "") ?: ""
        val secretToken = prefs.getString("secret_token", "") ?: ""
        val jsonTemplates = prefs.getString("bank_templates", "[]") ?: "[]"

        if (webhookUrl.isBlank()) return

        val gson = Gson()
        val type = object : TypeToken<List<BankTemplate>>() {}.type
        val templatesList: List<BankTemplate> = gson.fromJson(jsonTemplates, type) ?: emptyList()

        if (templatesList.isEmpty()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.originatingAddress ?: continue
            val body = sms.messageBody ?: continue
            val timestamp = sms.timestampMillis

            // البحث عن قالب مطابق لاسم المرسل
            for (template in templatesList) {
                if (sender.equals(template.bankName, ignoreCase = true)) {
                    val result = TemplateParser.parse(body, template.templatePattern)
                    if (result.isMatched) {
                        // إرسال البيانات المستخرجة
                        WebhookSender.sendNotification(
                            context = context,
                            webhookUrl = webhookUrl,
                            secretToken = secretToken,
                            walletName = template.bankName,
                            amount = result.amount ?: "0.000",
                            customerName = result.customerName ?: "Unknown",
                            rawMessage = body,
                            timestamp = timestamp
                        )
                        break
                    }
                }
            }
        }
    }
}
