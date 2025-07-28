package com.example.geniotecni.tigo.models

data class TransactionData(
    val phone: String = "",
    val cedula: String = "",
    val amount: String = "",
    val date: String = "",
    val additionalData: Map<String, String> = emptyMap()
)