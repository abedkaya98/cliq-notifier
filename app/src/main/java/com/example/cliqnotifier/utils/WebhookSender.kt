package com.example.cliqnotifier.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object WebhookSender {
    private val client = OkHttpClient()

    fun sendSmsWebhook(
        url: String,
        token: String,
        sender: String,
        message: String,
        timestamp: Long,
        onResult: (Boolean, String) -> Unit
    ) {
        if (url.isEmpty()) return

        val json = JSONObject().apply {
            put("sender", sender)
            put("message", message)
            put("timestamp", timestamp)
            put("api_token", token)
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, e.localizedMessage ?: "فشل الاتصال")
            }
            override fun onResponse(call: Call, response: Response) {
                onResult(response.isSuccessful, "Code: ${response.code}")
            }
        })
    }
}
