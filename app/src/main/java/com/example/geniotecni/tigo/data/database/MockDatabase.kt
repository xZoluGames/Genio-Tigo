package com.example.geniotecni.tigo.data.database

import android.content.Context
import com.example.geniotecni.tigo.data.dao.TransactionDao
import com.example.geniotecni.tigo.data.entities.TransactionEntity
import java.util.Date

// Mock implementation for compilation without Room
class MockTransactionDao : TransactionDao {
    private val transactions = mutableListOf<TransactionEntity>()
    
    override fun getAllTransactions(): List<TransactionEntity> = transactions.toList()
    
    override fun getTransactionsBetweenDates(startDate: Date, endDate: Date): List<TransactionEntity> =
        transactions.filter { it.date >= startDate && it.date <= endDate }
    
    override fun getTransactionsByService(service: String): List<TransactionEntity> =
        transactions.filter { it.service == service }
    
    override fun getTransactionById(id: Long): TransactionEntity? =
        transactions.find { it.id == id }
    
    override fun insertTransaction(transaction: TransactionEntity): Long {
        val newTransaction = transaction.copy(id = transactions.size.toLong() + 1)
        transactions.add(newTransaction)
        return newTransaction.id
    }
    
    override fun updateTransaction(transaction: TransactionEntity) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
        }
    }
    
    override fun deleteTransaction(transaction: TransactionEntity) {
        transactions.removeAll { it.id == transaction.id }
    }
    
    override fun deleteAllTransactions() {
        transactions.clear()
    }
    
    override fun getTransactionCount(): Int = transactions.size
    
    override fun getTotalAmount(): Long? = transactions.sumOf { it.amount }.takeIf { it > 0 }
    
    override fun getAverageAmount(): Double? = 
        if (transactions.isNotEmpty()) transactions.map { it.amount }.average() else null
    
    override fun getTotalCommission(): Long? = transactions.sumOf { it.commission }.takeIf { it > 0 }
    
    override fun getMostUsedServiceName(): String? = 
        transactions.groupBy { it.service }.maxByOrNull { it.value.size }?.key
    
    override fun getServiceCount(service: String): Int = 
        transactions.count { it.service == service }
}

class MockAppDatabase {
    private val transactionDao = MockTransactionDao()
    
    fun transactionDao(): TransactionDao = transactionDao
    
    companion object {
        @Volatile
        private var INSTANCE: MockAppDatabase? = null
        
        fun getDatabase(context: Context): MockAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = MockAppDatabase()
                INSTANCE = instance
                instance
            }
        }
    }
}