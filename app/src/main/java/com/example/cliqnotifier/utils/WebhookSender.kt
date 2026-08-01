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

    fun sendSmsToWebhook(
        context: Context,
        webhookUrl: String,
        secretToken: String,
        sender: String,
        messageBody: String,
        timestamp: Long
    ) {
        val jsonPayload = JSONObject().apply {
            put("sender", sender)
            put("message", messageBody)
            put("timestamp", timestamp)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toString().toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(webhookUrl)
            .post(requestBody)

        if (secretToken.isNotEmpty()) {
            requestBuilder.addHeader("X-Secret-Token", secretToken)
            requestBuilder.addHeader("Authorization", "Bearer $secretToken")
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "فشل إرسال Webhook: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val isSuccess = response.isSuccessful
                response.close()
                
                Handler(Looper.getMainLooper()).post {
                    if (isSuccess) {
                        Toast.makeText(context, "تم إرسال الـ Webhook بنجاح! 🚀", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "رد السيرفر بخطأ: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
