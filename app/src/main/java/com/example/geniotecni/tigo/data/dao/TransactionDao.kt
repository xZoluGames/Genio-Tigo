package com.example.geniotecni.tigo.data.dao

import androidx.room.*
import com.example.geniotecni.tigo.data.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE service = :service ORDER BY createdAt DESC")
    fun getTransactionsByService(service: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // Statistics queries
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("SELECT SUM(amount) FROM transactions")
    suspend fun getTotalAmount(): Long?

    @Query("SELECT AVG(amount) FROM transactions")
    suspend fun getAverageAmount(): Double?

    @Query("SELECT service, COUNT(*) as count FROM transactions GROUP BY service ORDER BY count DESC LIMIT 1")
    suspend fun getMostUsedService(): ServiceCount?

    @Query("SELECT SUM(commission) FROM transactions")
    suspend fun getTotalCommission(): Long?

    @Query("SELECT service, COUNT(*) as count FROM transactions GROUP BY service")
    suspend fun getServiceBreakdown(): List<ServiceCount>

    @Query("SELECT strftime('%H', datetime(createdAt/1000, 'unixepoch')) as hour, COUNT(*) as count FROM transactions GROUP BY hour ORDER BY count DESC LIMIT 1")
    suspend fun getPeakHour(): HourCount?
}

// Data classes for complex queries
data class ServiceCount(val service: String, val count: Int)
data class HourCount(val hour: String, val count: Int)