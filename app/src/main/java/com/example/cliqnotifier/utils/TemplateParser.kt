package com.example.cliqnotifier.utils

import java.util.regex.Pattern

data class ParsedSmsResult(
    val isMatched: Boolean,
    val amount: String? = null,
    val customerName: String? = null
)

object TemplateParser {

    /**
     * مطابقة أي رسالة نصية مع قالب محدد واستخراج المبلغ واسم الزبون
     */
    fun parse(smsBody: String, templatePattern: String): ParsedSmsResult {
        if (templatePattern.isBlank() || smsBody.isBlank()) {
            return ParsedSmsResult(isMatched = false)
        }

        // 1. تحويل القالب إلى كود Regex مع حماية العلامات الخاصة
        var regexPattern = Pattern.quote(templatePattern)

        // استبدال [amount] بـ Regex يلتقط الأرقام والكسور (مثل 15.000 أو 15)
        regexPattern = regexPattern.replace("\\[amount\\]", "\\E([0-9]+(?:\\.[0-9]+)?)\\Q")

        // استبدال [name] بـ Regex يلتقط أي نص (مثل RANEEM JODEH أو رنيم جودة)
        regexPattern = regexPattern.replace("\\[name\\]", "\\E(.+?)\\Q")

        // تنظيف القوالب الفارغة الناتجة عن Quote
        regexPattern = regexPattern.replace("\\Q\\E", "")

        return try {
            val pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
            val matcher = pattern.matcher(smsBody)

            if (matcher.find()) {
                var extractedAmount: String? = null
                var extractedName: String? = null

                // تحديد أي الحقول تم استخراجها بناءً على موقعها في القالب
                val hasAmountFirst = templatePattern.indexOf("[amount]") < templatePattern.indexOf("[name]")

                if (templatePattern.contains("[amount]") && templatePattern.contains("[name]")) {
                    if (hasAmountFirst) {
                        extractedAmount = matcher.group(1)?.trim()
                        extractedName = matcher.group(2)?.trim()
                    } else {
                        extractedName = matcher.group(1)?.trim()
                        extractedAmount = matcher.group(2)?.trim()
                    }
                } else if (templatePattern.contains("[amount]")) {
                    extractedAmount = matcher.group(1)?.trim()
                } else if (templatePattern.contains("[name]")) {
                    extractedName = matcher.group(1)?.trim()
                }

                ParsedSmsResult(
                    isMatched = true,
                    amount = extractedAmount,
                    customerName = extractedName
                )
            } else {
                ParsedSmsResult(isMatched = false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ParsedSmsResult(isMatched = false)
        }
    }
}
