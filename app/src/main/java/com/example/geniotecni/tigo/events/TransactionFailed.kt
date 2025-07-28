package com.example.geniotecni.tigo.events

data class TransactionFailed(
    val serviceId: String,
    val serviceName: String,
    val transactionId: String,
    val errorMessage: String,
    val errorCode: String? = null
) : TransactionEvent
