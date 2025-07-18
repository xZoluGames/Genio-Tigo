package com.example.geniotecni.tigo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
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