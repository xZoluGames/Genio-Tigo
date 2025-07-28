package com.example.geniotecni.tigo.data.database

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.geniotecni.tigo.data.entities.TransactionEntity

/**
 * Mock database simplificado para el build
 */
object MockDatabaseSimple {
    
    private val transactions = mutableListOf<TransactionEntity>()
    
    fun getAllTransactions(): List<TransactionEntity> {
        return transactions.toList()
    }
    
    fun insertTransaction(transaction: TransactionEntity): Long {
        transactions.add(transaction)
        return transaction.id
    }
    
    fun clear() {
        transactions.clear()
    }
}