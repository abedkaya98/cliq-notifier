package com.example.cliqnotifier.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cliq_prefs", Context.MODE_PRIVATE)

    var webhookUrl: String
        get() = prefs.getString("webhook_url", "") ?: ""
        set(value) = prefs.edit().putString("webhook_url", value).apply()

    var secretToken: String
        get() = prefs.getString("secret_token", "") ?: ""
        set(value) = prefs.edit().putString("secret_token", value).apply()

    var senderFilter: String
        get() = prefs.getString("sender_filter", "CAB") ?: "CAB"
        set(value) = prefs.edit().putString("sender_filter", value).apply()

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean("service_enabled", false)
        set(value) = prefs.edit().putBoolean("service_enabled", value).apply()
}
