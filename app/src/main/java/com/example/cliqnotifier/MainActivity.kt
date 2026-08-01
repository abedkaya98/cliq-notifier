package com.example.cliqnotifier

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.cliqnotifier.databinding.ActivityMainBinding
import com.example.cliqnotifier.utils.WebhookSender

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissions()
        loadSavedSettings()

        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnTestLastSms.setOnClickListener {
            testLastSms()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val listPermissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences("CliQSettings", Context.MODE_PRIVATE)
        binding.etWebhookUrl.setText(prefs.getString("webhook_url", ""))
        binding.etSecretToken.setText(prefs.getString("secret_token", ""))
        binding.etSenderFilter.setText(prefs.getString("sender_filter", "CAB,REFLECT"))
        binding.switchService.isChecked = prefs.getBoolean("service_enabled", false)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("CliQSettings", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("webhook_url", binding.etWebhookUrl.text.toString().trim())
        editor.putString("secret_token", binding.etSecretToken.text.toString().trim())
        editor.putString("sender_filter", binding.etSenderFilter.text.toString().trim())
        editor.putBoolean("service_enabled", binding.switchService.isChecked)
        editor.apply()

        Toast.makeText(this, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
    }

    private fun testLastSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "الرجاء منح إذن قراءة الرسائل أولاً", Toast.LENGTH_SHORT).show()
            checkPermissions()
            return
        }

        val filtersRaw = binding.etSenderFilter.text.toString().trim()
        if (filtersRaw.isEmpty()) {
            Toast.makeText(this, "يرجى كتابة اسم مرسل في الفلتر أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        val filters: List<String> = filtersRaw.split(",").map { it.trim().lowercase() }
        val webhookUrl = binding.etWebhookUrl.text.toString().trim()
        val secretToken = binding.etSecretToken.text.toString().trim()

        if (webhookUrl.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال رابط Webhook أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        val cursor: Cursor? = contentResolver.query(
            Uri.parse("content://sms/inbox"),
            null, null, null, "date DESC"
        )

        var foundMatch = false

        cursor?.use { smsCursor ->
            val addressIndex = smsCursor.getColumnIndex("address")
            val bodyIndex = smsCursor.getColumnIndex("body")
            val dateIndex = smsCursor.getColumnIndex("date")

            while (smsCursor.moveToNext()) {
                val address: String = if (addressIndex >= 0) smsCursor.getString(addressIndex) ?: "" else ""
                val body: String = if (bodyIndex >= 0) smsCursor.getString(bodyIndex) ?: "" else ""
                val date: Long = if (dateIndex >= 0) smsCursor.getLong(dateIndex) else System.currentTimeMillis()

                val isMatched = filters.any { filterItem: String ->
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
