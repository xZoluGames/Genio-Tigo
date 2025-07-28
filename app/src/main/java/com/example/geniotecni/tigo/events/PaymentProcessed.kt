package com.example.geniotecni.tigo.events

data class PaymentProcessed(
    val transactionId: String,
    val amount: Long,
    val serviceProvider: String,
    val success: Boolean
) : TransactionEvent
