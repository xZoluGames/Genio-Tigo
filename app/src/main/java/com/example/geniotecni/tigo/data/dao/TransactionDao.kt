package com.example.geniotecni.tigo.data.dao

import androidx.room.*
import androidx.lifecycle.LiveData
import com.example.geniotecni.tigo.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(transactions: List<TransactionEntity>): List<Long>
    
    @Update
    fun update(transaction: TransactionEntity)
    
    @Delete
    fun delete(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    fun deleteById(id: Long)
    
    @Query("DELETE FROM transactions")
    fun deleteAll()
    
    // Basic queries
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getById(id: Long): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getByIdLiveData(id: Long): LiveData<TransactionEntity?>
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllLiveData(): LiveData<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getPaginated(limit: Int, offset: Int): List<TransactionEntity>
    
    // Filter queries
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
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Int
    
    @Query("SELECT COUNT(*) FROM transactions WHERE status = :status")
    fun getCountByStatus(status: TransactionStatus): Int
    
    @Query("SELECT SUM(amount) FROM transactions WHERE status = 'COMPLETED'")
    fun getTotalAmount(): Long?
    
    @Query("SELECT SUM(commission) FROM transactions WHERE status = 'COMPLETED'")
    fun getTotalCommission(): Long?
    
    @Query("SELECT AVG(amount) FROM transactions WHERE status = 'COMPLETED'")
    fun getAverageAmount(): Double?
    
    @Query("SELECT service_name FROM transactions WHERE status = 'COMPLETED' GROUP BY service_name ORDER BY COUNT(*) DESC LIMIT 1")
    fun getMostUsedServiceName(): String?
    
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
    
    // Update operations
    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    fun updateStatus(id: Long, status: TransactionStatus)
    
    @Query("UPDATE transactions SET status = :status, error_message = :errorMessage WHERE id = :id")
    fun updateStatusWithError(id: Long, status: TransactionStatus, errorMessage: String)
    
    @Query("UPDATE transactions SET is_printed = 1, print_count = print_count + 1, last_print_time = :printTime WHERE id = :id")
    fun markAsPrinted(id: Long, printTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE transactions SET print_data = :printData WHERE id = :id")
    fun updatePrintData(id: Long, printData: String)
    
    @Query("UPDATE transactions SET response_data = :responseData WHERE id = :id")
    fun updateResponseData(id: Long, responseData: String)
    
    // Cleanup operations
    @Query("DELETE FROM transactions WHERE timestamp < :cutoffTimestamp")
    fun deleteOldTransactions(cutoffTimestamp: Long)
    
    @Query("DELETE FROM transactions WHERE status = 'CANCELLED' AND timestamp < :cutoffTimestamp")
    fun deleteCancelledTransactions(cutoffTimestamp: Long)
    
    // Bulk operations
    @Query("UPDATE transactions SET status = 'CANCELLED' WHERE status = 'PENDING' AND timestamp < :cutoffTimestamp")
    fun cancelOldPendingTransactions(cutoffTimestamp: Long)
    
    // Export query for backup
    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    fun getAllForExport(): List<TransactionEntity>
    
    // Validation queries
    @Query("SELECT COUNT(*) FROM transactions WHERE phone = :phone AND timestamp > :recentTimestamp")
    fun getRecentTransactionCountByPhone(phone: String, recentTimestamp: Long): Int
    
    @Query("SELECT * FROM transactions WHERE phone = :phone AND amount = :amount AND timestamp > :recentTimestamp LIMIT 1")
    fun findDuplicateTransaction(phone: String, amount: Long, recentTimestamp: Long): TransactionEntity?
    
    // Simplified statistics queries (removing complex ones that cause issues)
    @Query("SELECT service_id, service_name, COUNT(*) as count, SUM(amount) as totalAmount FROM transactions WHERE status = 'COMPLETED' GROUP BY service_id, service_name ORDER BY count DESC")
    fun getServiceUsageStats(): List<ServiceUsageRaw>
    
    @Query("SELECT DATE(created_date) as date, COUNT(*) as transactionCount, SUM(amount) as totalAmount, SUM(commission) as totalCommission, COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as successfulCount, COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failedCount FROM transactions WHERE timestamp >= :startTimestamp GROUP BY DATE(created_date) ORDER BY date DESC")
    fun getDailyStatistics(startTimestamp: Long): List<DailyStatisticsRaw>
}

// Raw data classes for complex queries


