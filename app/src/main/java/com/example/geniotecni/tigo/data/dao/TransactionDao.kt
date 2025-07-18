package com.example.geniotecni.tigo.data.dao

import com.example.geniotecni.tigo.data.entities.TransactionEntity
import java.util.Date

// Simplified interface without Room annotations
interface TransactionDao {
    fun getAllTransactions(): List<TransactionEntity>
    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): List<TransactionEntity>
    fun getTransactionsByService(service: String): List<TransactionEntity>
    fun getTransactionById(id: Long): TransactionEntity?
    fun insertTransaction(transaction: TransactionEntity): Long
    fun updateTransaction(transaction: TransactionEntity)
    fun deleteTransaction(transaction: TransactionEntity)
    fun deleteAllTransactions()
    
    // Statistics queries
    fun getTransactionCount(): Int
    fun getTotalAmount(): Long?
    fun getAverageAmount(): Double?
    fun getTotalCommission(): Long?
    fun getMostUsedServiceName(): String?
    fun getServiceCount(service: String): Int
}