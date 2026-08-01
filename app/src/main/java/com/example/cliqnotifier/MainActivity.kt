package com.example.cliqnotifier

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.materialswitch.MaterialSwitch
import com.example.cliqnotifier.utils.WebhookSender
import com.example.cliqnotifier.R // <--- تم إضافة استيراد ملف الموارد لربط الـ Views

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101

    private lateinit var etWebhookUrl: EditText
    private lateinit var etSecretToken: EditText
    private lateinit var etSenderFilter: EditText
    private lateinit var switchService: MaterialSwitch
    private lateinit var btnSave: Button
    private lateinit var btnTestLastSms: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Binding UI Elements directly
        etWebhookUrl = findViewById(R.id.etWebhookUrl)
        etSecretToken = findViewById(R.id.etSecretToken)
        etSenderFilter = findViewById(R.id.etSenderFilter)
        switchService = findViewById(R.id.switchService)
        btnSave = findViewById(R.id.btnSave)
        btnTestLastSms = findViewById(R.id.btnTestLastSms)

        checkPermissions()
        loadSavedSettings()

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnTestLastSms.setOnClickListener {
            testLastSms()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val listPermissionsNeeded = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences("CliQSettings", Context.MODE_PRIVATE)
        etWebhookUrl.setText(prefs.getString("webhook_url", ""))
        etSecretToken.setText(prefs.getString("secret_token", ""))
        etSenderFilter.setText(prefs.getString("sender_filter", "CAB,REFLECT"))
        switchService.isChecked = prefs.getBoolean("service_enabled", false)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("CliQSettings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        val url = etWebhookUrl.text.toString().trim()
        val token = etSecretToken.text.toString().trim()
        val filter = etSenderFilter.text.toString().trim()
        val isEnabled = switchService.isChecked

        editor.putString("webhook_url", url)
        editor.putString("secret_token", token)
        editor.putString("sender_filter", filter)
        editor.putBoolean("service_enabled", isEnabled)
        editor.apply()

        Toast.makeText(this, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
    }

    private fun testLastSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "الرجاء منح إذن قراءة الرسائل أولاً", Toast.LENGTH_SHORT).show()
            checkPermissions()
            return
        }

        val filtersRaw = etSenderFilter.text.toString().trim()
        if (filtersRaw.isEmpty()) {
            Toast.makeText(this, "يرجى كتابة اسم مرسل في الفلتر أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        val filters: List<String> = filtersRaw.split(",").map { item -> item.trim().lowercase() }
        val webhookUrl = etWebhookUrl.text.toString().trim()
        val secretToken = etSecretToken.text.toString().trim()

        if (webhookUrl.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال رابط Webhook أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        val smsUri = Uri.parse("content://sms/inbox")
        val cursor: Cursor? = contentResolver.query(smsUri, null, null, null, "date DESC")

        var foundMatch = false

        cursor?.use { smsCursor ->
            val addressIndex = smsCursor.getColumnIndex("address")
            val bodyIndex = smsCursor.getColumnIndex("body")
            val dateIndex = smsCursor.getColumnIndex("date")

            while (smsCursor.moveToNext()) {
                val address: String = if (addressIndex >= 0) smsCursor.getString(addressIndex) ?: "" else ""
                val body: String = if (bodyIndex >= 0) smsCursor.getString(bodyIndex) ?: "" else ""
                val date: Long = if (dateIndex >= 0) smsCursor.getLong(dateIndex) else System.currentTimeMillis()

                val isMatched = filters.any { filterItem ->
                    address.lowercase().contains(filterItem)
                }

                if (isMatched) {
                    foundMatch = true
                    Toast.makeText(this, "تم العثور على رسالة من: $address .. جاري الإرسال", Toast.LENGTH_SHORT).show()
                    
                    WebhookSender.sendSmsToWebhook(
                        context = this,
                        webhookUrl = webhookUrl,
                        secretToken = secretToken,
                        sender = address,
                        messageBody = body,
                        timestamp = date
                    )
                    break
                }
            }
        }

        if (!foundMatch) {
            Toast.makeText(this, "لم يتم العثور على رسائل مطابقة لـ: $filtersRaw", Toast.LENGTH_LONG).show()
        }
    }
}
