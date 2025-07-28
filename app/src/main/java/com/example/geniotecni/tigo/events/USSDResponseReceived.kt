package com.example.geniotecni.tigo.events

data class USSDResponseReceived(
    val transactionId: String,
    val responseBody: String,
    val referenceData: com.example.geniotecni.tigo.models.ReferenceData?
) : TransactionEvent
