package com.example.cliqnotifier

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cliqnotifier.adapters.TemplateAdapter
import com.example.cliqnotifier.models.BankTemplate
import com.example.cliqnotifier.utils.TemplateParser
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 101

    private lateinit var etWebhookUrl: EditText
    private lateinit var etSecretToken: EditText
    private lateinit var switchService: MaterialSwitch
    private lateinit var btnSave: Button
    private lateinit var btnAddBankCard: Button
    private lateinit var rvBankTemplates: RecyclerView

    private lateinit var templateAdapter: TemplateAdapter
    private val bankTemplatesList = mutableListOf<BankTemplate>()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        checkPermissions()
        loadSavedSettings()

        btnSave.setOnClickListener { saveSettings() }
        btnAddBankCard.setOnClickListener { showAddTemplateDialog() }
    }

    private fun initViews() {
        etWebhookUrl = findViewById(R.id.etWebhookUrl)
        etSecretToken = findViewById(R.id.etSecretToken)
        switchService = findViewById(R.id.switchService)
        btnSave = findViewById(R.id.btnSave)
        btnAddBankCard = findViewById(R.id.btnAddBankCard)
        rvBankTemplates = findViewById(R.id.rvBankTemplates)
    }

    private fun setupRecyclerView() {
        templateAdapter = TemplateAdapter(
            templates = bankTemplatesList,
            onDeleteClick = { template -> deleteTemplate(template) },
            onTestClick = { template -> testSingleTemplate(template) }
        )
        rvBankTemplates.layoutManager = LinearLayoutManager(this)
        rvBankTemplates.adapter = templateAdapter
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
        etSecretToken.setText(prefs.getString("secret_token", "CliqSecret2026"))
        switchService.isChecked = prefs.getBoolean("service_enabled", false)

        val jsonTemplates = prefs.getString("bank_templates", "[]")
        val type = object : TypeToken<List<BankTemplate>>() {}.type
        val savedList: List<BankTemplate> = gson.fromJson(jsonTemplates, type) ?: emptyList()

        bankTemplatesList.clear()
        bankTemplatesList.addAll(savedList)
        templateAdapter.notifyDataSetChanged()
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("CliQSettings", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putString("webhook_url", etWebhookUrl.text.toString().trim())
        editor.putString("secret_token", etSecretToken.text.toString().trim())
        editor.putBoolean("service_enabled", switchService.isChecked)

        val jsonTemplates = gson.toJson(bankTemplatesList)
        editor.putString("bank_templates", jsonTemplates)

        editor.apply()
        Toast.makeText(this, "تم حفظ الإعدادات والقوالب بنجاح! 💾", Toast.LENGTH_SHORT).show()
    }

    private fun showAddTemplateDialog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "يرجى إعطاء صلاحية قراءة الرسائل أولاً", Toast.LENGTH_SHORT).show()
            checkPermissions()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_template, null)
        val spSenderList = dialogView.findViewById<Spinner>(R.id.spSenderList)
        val etTemplatePattern = dialogView.findViewById<EditText>(R.id.etTemplatePattern)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddTemplate)

        // جلب قائمة المرسلين المستخرجة من الهاتف
        val senders = getUniqueSmsSenders()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, senders)
        spSenderList.adapter = spinnerAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnAdd.setOnClickListener {
            val selectedSender = spSenderList.selectedItem?.toString() ?: ""
            val pattern = etTemplatePattern.text.toString().trim()

            if (selectedSender.isEmpty()) {
                Toast.makeText(this, "اختر اسم المرسل من القائمة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!pattern.contains("[amount]")) {
                Toast.makeText(this, "يجب أن يحتوي القالب على [amount] للتعرف على المبلغ", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val newTemplate = BankTemplate(
                bankName = selectedSender,
                templatePattern = pattern
            )

            bankTemplatesList.add(newTemplate)
            templateAdapter.notifyDataSetChanged()
            saveSettings()

            dialog.dismiss()
            Toast.makeText(this, "تمت إضافة بطاقة $selectedSender بنجاح!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun deleteTemplate(template: BankTemplate) {
        bankTemplatesList.remove(template)
        templateAdapter.notifyDataSetChanged()
        saveSettings()
        Toast.makeText(this, "تم حذف القالب", Toast.LENGTH_SHORT).show()
    }

    private fun testSingleTemplate(template: BankTemplate) {
        val smsUri = Uri.parse("content://sms/inbox")
        val cursor: Cursor? = contentResolver.query(smsUri, null, null, null, "date DESC")

        var matchedFound = false

        cursor?.use { c ->
            val addressIndex = c.getColumnIndex("address")
            val bodyIndex = c.getColumnIndex("body")
            var count = 0

            while (c.moveToNext() && count < 20) { // فحص آخر 20 رسالة لهذا المرسل
                val address = if (addressIndex >= 0) c.getString(addressIndex) ?: "" else ""
                val body = if (bodyIndex >= 0) c.getString(bodyIndex) ?: "" else ""

                if (address.equals(template.bankName, ignoreCase = true)) {
                    val parseResult = TemplateParser.parse(body, template.templatePattern)
                    if (parseResult.isMatched) {
                        matchedFound = true
                        AlertDialog.Builder(this)
                            .setTitle("نجح اختبار القالب! ✅")
                            .setMessage("تطابق مع رسالة من: ${template.bankName}\n\nالمبلغ المستخرج: ${parseResult.amount ?: "غير محدد"}\nاسم المحول المستخرج: ${parseResult.customerName ?: "غير محدد"}")
                            .setPositiveButton("موافق", null)
                            .show()
                        break
                    }
                }
                count++
            }
        }

        if (!matchedFound) {
            Toast.makeText(this, "لم نجد أي رسالة مطابقة للقالب في آخر رسائل ${template.bankName}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getUniqueSmsSenders(): List<String> {
        val sendersSet = mutableSetOf<String>()
        val smsUri = Uri.parse("content://sms/inbox")
        val cursor: Cursor? = contentResolver.query(smsUri, arrayOf("address"), null, null, "date DESC")

        cursor?.use { c ->
            val addressIndex = c.getColumnIndex("address")
            var count = 0
            while (c.moveToNext() && count < 100) { // أخذ أحدث 100 رسالة لاستخراج المرسلين
                val address = if (addressIndex >= 0) c.getString(addressIndex) else null
                if (!address.isNull_or_blank()) {
                    sendersSet.add(address)
                }
                count++
            }
        }
        return sendersSet.toList().ifEmpty { listOf("CAB", "REFLECT") }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}
