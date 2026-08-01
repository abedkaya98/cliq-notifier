package com.example.cliqnotifier.utils

import java.util.regex.Pattern

object TemplateGenerator {

    /**
     * يستخرج القالب التلقائي من نص الرسالة الحقيقية
     */
    fun generateTemplateFromSms(smsBody: String): String {
        if (smsBody.isBlank()) return ""

        var generatedPattern = smsBody

        // 1. استبدال المبلغ بـ [amount]
        val amountRegex = Pattern.compile("(?i)(\\b[0-9]+(?:\\.[0-9]+)?\\b)(?=\\s*(?:JOD|Jod|د\\.أ|دينار))")
        val amountMatcher = amountRegex.matcher(smsBody)

        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)
            if (amountStr != null) {
                generatedPattern = generatedPattern.replace(amountStr, "[amount]")
            }
        }

        // 2. استبدال الاسم بـ [name]
        val nameRegex = Pattern.compile("(?i)(?:from|من)\\s+([A-Za-z\\s]+?)(?=\\s+(?:Using|using|بواسطة|عبر|Using CliQ|t))")
        val nameMatcher = nameRegex.matcher(smsBody)

        if (nameMatcher.find()) {
            val nameStr = nameMatcher.group(1)?.trim()
            if (!nameStr.isNullOrBlank()) {
                generatedPattern = generatedPattern.replace(nameStr, "[name]")
            }
        }

        return generatedPattern
    }
}
