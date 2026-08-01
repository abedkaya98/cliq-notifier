package com.example.cliqnotifier.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object WebhookSender {

    private val client = OkHttpClient()

    fun sendNotification(
        context: Context,
        webhookUrl: String,
        secretToken: String,
        walletName: String,
        amount: String,
        customerName: String,
        rawMessage: String,
        timestamp: Long
    ) {
        val jsonPayload = JSONObject().apply {
            put("wallet_name", walletName)
            put("amount", amount)
            put("customer_name", customerName)
            put("raw_message", rawMessage)
            put("timestamp", timestamp)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(webhookUrl)
            .addHeader("X-Secret-Token", secretToken)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(context, "فشل إرسال الـ Webhook ❌")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    showToast(context, "تم تسجيل الدفعة بنجاح! 🚀")
                } else {
                    showToast(context, "خطأ سيرفر: ${response.code}")
                }
            }
        })
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
