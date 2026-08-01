package com.example.cliqnotifier.models

import java.io.Serializable

data class BankTemplate(
    val id: String = System.currentTimeMillis().toString(),
    val bankName: String,      // اسم المرسل (مثال: CAB أو REFLECT)
    val templatePattern: String // القالب (مثال: Amount [amount] JOD... from [name])
) : Serializable
