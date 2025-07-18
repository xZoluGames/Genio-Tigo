package com.example.geniotecni.tigo.data.repository

import com.example.geniotecni.tigo.data.dao.TransactionDao
import com.example.geniotecni.tigo.data.entities.TransactionEntity
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.models.ReferenceData
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): List<TransactionEntity> = transactionDao.getAllTransactions()

    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): List<TransactionEntity> =
        transactionDao.getTransactionsBetweenDates(startDate, endDate)

    fun getTransactionsByService(service: String): List<TransactionEntity> =
        transactionDao.getTransactionsByService(service)

    fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    fun deleteAllTransactions() = transactionDao.deleteAllTransactions()

    fun getTransactionCount() = transactionDao.getTransactionCount()

    fun getTotalAmount() = transactionDao.getTotalAmount() ?: 0L

    fun getAverageAmount() = transactionDao.getAverageAmount() ?: 0.0

    fun getMostUsedServiceName() = transactionDao.getMostUsedServiceName()

    fun getTotalCommission() = transactionDao.getTotalCommission() ?: 0L

    fun getServiceCount(service: String) = transactionDao.getServiceCount(service)

    // Conversion methods
    fun insertFromPrintData(printData: PrintData): Long {
        val transaction = printDataToEntity(printData)
        return insertTransaction(transaction)
    }

    private fun printDataToEntity(printData: PrintData): TransactionEntity {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val date = try {
            dateFormat.parse(printData.date) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        return TransactionEntity(
            service = printData.service,
            serviceType = 1, // Default service type
            amount = extractAmount(printData.message),
            commission = extractCommission(printData.message),
            phone = printData.referenceData.ref1.takeIf { it.contains(Regex("\\d{10}")) },
            cedula = printData.referenceData.ref2.takeIf { it.isNotEmpty() },
            date = date,
            referenceNumber = generateReferenceNumber(),
            status = "SUCCESS",
            message = printData.message
        )
    }

    private fun extractAmount(message: String): Long {
        val regex = Regex("Monto: ([\\d,]+) Gs\\.")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0L
    }

    private fun extractCommission(message: String): Long {
        val regex = Regex("Comisión: ([\\d,]+) Gs\\.")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0L
    }
    
    private fun generateReferenceNumber(): String {
        return "REF${System.currentTimeMillis()}"
    }
}