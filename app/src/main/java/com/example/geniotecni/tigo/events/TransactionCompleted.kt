package com.example.geniotecni.tigo.events

data class TransactionCompleted(
    val serviceId: String,
    val serviceName: String,
    val transactionId: String,
    val amount: Long,
    val referenceData: com.example.geniotecni.tigo.models.ReferenceData?,
    val printData: com.example.geniotecni.tigo.models.PrintData
) : TransactionEvent

