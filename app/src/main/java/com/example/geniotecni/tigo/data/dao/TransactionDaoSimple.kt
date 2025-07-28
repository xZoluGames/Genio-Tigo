package com.example.geniotecni.tigo.data.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.geniotecni.tigo.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDaoSimple {
    
    // Basic CRUD operations - Working versions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>
    
    @Update
    suspend fun update(transaction: TransactionEntity): Int
    
    @Delete
    suspend fun delete(transaction: TransactionEntity): Int
    
    // Basic queries - LiveData and Flow versions work well
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getByIdLiveData(id: Long): LiveData<TransactionEntity?>
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllLiveData(): LiveData<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>
    
    // Filter queries - Using LiveData/Flow
    @Query("SELECT * FROM transactions WHERE service_id = :serviceId ORDER BY timestamp DESC")
    fun getByServiceId(serviceId: Int): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestamp DESC")
    fun getByStatus(status: TransactionStatus): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE phone = :phone ORDER BY timestamp DESC")
    fun getByPhone(phone: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE cedula = :cedula ORDER BY timestamp DESC")
    fun getByCedula(cedula: String): Flow<List<TransactionEntity>>
    
    // Date range queries
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE timestamp >= :timestampStart ORDER BY timestamp DESC")
    fun getFromTimestamp(timestampStart: Long): Flow<List<TransactionEntity>>
    
    // Search queries  
    @Query("""
        SELECT * FROM transactions 
        WHERE service_name LIKE '%' || :query || '%' 
        OR phone LIKE '%' || :query || '%' 
        OR cedula LIKE '%' || :query || '%' 
        OR reference LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun search(query: String): Flow<List<TransactionEntity>>
    
    // Statistics queries - Non-suspend versions
    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): LiveData<Int>
    
    @Query("SELECT COUNT(*) FROM transactions WHERE status = :status")
    fun getCountByStatus(status: TransactionStatus): LiveData<Int>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE status = 'COMPLETED'")
    fun getTotalAmount(): LiveData<Long?>
    
    @Query("SELECT SUM(commission) FROM transactions WHERE status = 'COMPLETED'")
    fun getTotalCommission(): LiveData<Long?>
    
    @Query("SELECT AVG(amount) FROM transactions WHERE status = 'COMPLETED'")
    fun getAverageAmount(): LiveData<Double?>
    
    // Recent transactions for quick access
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>
    
    // Pending transactions
    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingTransactions(): Flow<List<TransactionEntity>>
    
    // Failed transactions for retry
    @Query("SELECT * FROM transactions WHERE status = 'FAILED' ORDER BY timestamp DESC")
    fun getFailedTransactions(): Flow<List<TransactionEntity>>
    
    // Print history queries
    @Query("SELECT * FROM transactions WHERE is_printed = 1 ORDER BY last_print_time DESC")
    fun getPrintedTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE print_data != '' AND status = 'COMPLETED' ORDER BY timestamp DESC")
    fun getPrintableTransactions(): Flow<List<TransactionEntity>>
}