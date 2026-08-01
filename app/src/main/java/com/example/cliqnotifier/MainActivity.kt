package com.example.cliqnotifier

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cliqnotifier.service.SmsForegroundService
import com.example.cliqnotifier.utils.PrefsManager

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // واجهة برمجية بسيطة مباشرة بدون ملفات تصميم XML معقدة
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        prefs = PrefsManager(this)
        checkPermissions()

        val etWebhook = EditText(this).apply { hint = "رابط الـ Webhook الخاص بمتجرك"; setText(prefs.webhookUrl) }
        val etToken = EditText(this).apply { hint = "مفتاح الأمان (Secret Token)"; setText(prefs.secretToken) }
        val etFilter = EditText(this).apply { hint = "فلتر المرسل (مثل: CAB)"; setText(prefs.senderFilter) }
        val switchService = Switch(this).apply { text = "تفعيل خدمة المراقبة 24/7"; isChecked = prefs.isServiceEnabled }
        val btnSave = Button(this).apply { text = "حفظ الإعدادات" }

        layout.addView(etWebhook)
        layout.addView(etToken)
        layout.addView(etFilter)
        layout.addView(switchService)
        layout.addView(btnSave)
        setContentView(layout)

        btnSave.setOnClickListener {
            prefs.webhookUrl = etWebhook.text.toString().trim()
            prefs.secretToken = etToken.text.toString().trim()
            prefs.senderFilter = etFilter.text.toString().trim()
            Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
        }

        switchService.setOnCheckedChangeListener { _, isChecked ->
            prefs.isServiceEnabled = isChecked
            val intent = Intent(this, SmsForegroundService::class.java)
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            } else {
                stopService(intent)
            }
        }
    }

    private fun checkPermissions() {
        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
    }
}
