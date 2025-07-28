package com.example.geniotecni.tigo.events

data class TransactionStarted(
    val serviceId: String,
    val serviceName: String,
    val transactionId: String = generateTransactionId()
) : TransactionEvent
