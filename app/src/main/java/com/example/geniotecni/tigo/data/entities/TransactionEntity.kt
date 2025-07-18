package com.example.geniotecni.tigo.data.entities

import java.util.Date

// Simplified entity without Room annotations
data class TransactionEntity(
    val id: Long = 0,
    val service: String,
    val serviceType: Int,
    val amount: Long,
    val commission: Long,
    val phone: String?,
    val cedula: String?,
    val date: Date,
    val referenceNumber: String,
    val status: String,
    val message: String,
    val createdAt: Date = Date()
)