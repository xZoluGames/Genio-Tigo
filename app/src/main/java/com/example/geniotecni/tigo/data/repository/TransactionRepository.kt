package com.example.geniotecni.tigo.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import com.example.geniotecni.tigo.data.dao.TransactionDao
import com.example.geniotecni.tigo.data.entities.DailyStatistics
import com.example.geniotecni.tigo.data.entities.ServiceUsage
import com.example.geniotecni.tigo.data.entities.TransactionEntity
import com.example.geniotecni.tigo.data.entities.TransactionStatistics
import com.example.geniotecni.tigo.data.entities.TransactionStatus
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val context: Context
) {

    companion object {
        private const val TAG = "TransactionRepository"
        private const val MIGRATION_COMPLETED_KEY = "migration_from_shared_prefs_completed"
    }

    // Basic CRUD operations
    suspend fun insert(transaction: TransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            transactionDao.insert(transaction)
        }
    }

    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long> {
        return withContext(Dispatchers.IO) {
            transactionDao.insertAll(transactions)
        }
    }

    suspend fun update(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            transactionDao.update(transaction)
        }
    }

    suspend fun delete(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            transactionDao.delete(transaction)
        }
    }

    // Bulk delete operations removed - not used in application flow

    // Query operations
    suspend fun getById(id: Long): TransactionEntity? {
        return withContext(Dispatchers.IO) {
            transactionDao.getById(id)
        }
    }

    fun getByIdLiveData(id: Long): LiveData<TransactionEntity?> {
        return transactionDao.getByIdLiveData(id)
    }

    fun getAllLiveData(): LiveData<List<TransactionEntity>> {
        return transactionDao.getAllLiveData()
    }

    fun getAllFlow(): Flow<List<TransactionEntity>> {
        return transactionDao.getAllFlow()
    }

    // Pagination removed - not used in current UI flow

    // Filter operations
    fun getByServiceId(serviceId: Int): Flow<List<TransactionEntity>> {
        return transactionDao.getByServiceId(serviceId)
    }

    fun getByStatus(status: TransactionStatus): Flow<List<TransactionEntity>> {
        return transactionDao.getByStatus(status)
    }

    fun getByPhone(phone: String): Flow<List<TransactionEntity>> {
        return transactionDao.getByPhone(phone)
    }

    fun getByCedula(cedula: String): Flow<List<TransactionEntity>> {
        return transactionDao.getByCedula(cedula)
    }

    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getByDateRange(startDate, endDate)
    }

    fun search(query: String): Flow<List<TransactionEntity>> {
        return transactionDao.search(query)
    }

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> {
        return transactionDao.getRecentTransactions(limit)
    }

    fun getPendingTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getPendingTransactions()
    }

    fun getFailedTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getFailedTransactions()
    }

    fun getPrintableTransactions(): Flow<List<TransactionEntity>> {
        return transactionDao.getPrintableTransactions()
    }

    // Statistics operations
    suspend fun getTransactionCount(): Int {
        return withContext(Dispatchers.IO) {
            transactionDao.getTransactionCount()
        }
    }

    suspend fun getCountByStatus(status: TransactionStatus): Int {
        return withContext(Dispatchers.IO) {
            transactionDao.getCountByStatus(status)
        }
    }

    suspend fun getTotalAmount(): Long {
        return withContext(Dispatchers.IO) {
            transactionDao.getTotalAmount() ?: 0L
        }
    }

    suspend fun getTotalCommission(): Long {
        return withContext(Dispatchers.IO) {
            transactionDao.getTotalCommission() ?: 0L
        }
    }

    suspend fun getAverageAmount(): Double {
        return withContext(Dispatchers.IO) {
            transactionDao.getAverageAmount() ?: 0.0
        }
    }

    suspend fun getMostUsedServiceName(): String? {
        return withContext(Dispatchers.IO) {
            transactionDao.getMostUsedServiceName()
        }
    }

    suspend fun getServiceUsageStats(): List<ServiceUsage> {
        return withContext(Dispatchers.IO) {
            val rawStats = transactionDao.getServiceUsageStats()
            rawStats.map { raw ->
                ServiceUsage(
                    serviceId = raw.service_id,
                    serviceName = raw.service_name,
                    count = raw.count,
                    totalAmount = raw.totalAmount,
                    percentage = 0.0f // Calculado después si es necesario
                )
            }
        }
    }

    suspend fun getDailyStatistics(startTimestamp: Long): List<DailyStatistics> {
        return withContext(Dispatchers.IO) {
            val rawStats = transactionDao.getDailyStatistics(startTimestamp)
            rawStats.map { raw ->
                DailyStatistics(
                    date = raw.date,
                    transactionCount = raw.transactionCount,
                    totalAmount = raw.totalAmount,
                    totalCommission = raw.totalCommission,
                    successfulCount = raw.successfulCount,
                    failedCount = raw.failedCount
                )
            }
        }
    }

    suspend fun getTransactionStatistics(): TransactionStatistics {
        val totalTransactions = getTransactionCount()
        val totalAmount = getTotalAmount()
        val totalCommission = getTotalCommission()
        val averageAmount = getAverageAmount().toLong()
        val successfulTransactions = getCountByStatus(TransactionStatus.COMPLETED)
        val failedTransactions = getCountByStatus(TransactionStatus.FAILED)
        val successRate = if (totalTransactions > 0) {
            (successfulTransactions.toFloat() / totalTransactions) * 100f
        } else 0f
        val mostUsedService = getMostUsedServiceName()

        return TransactionStatistics(
            totalTransactions = totalTransactions,
            totalAmount = totalAmount,
            totalCommission = totalCommission,
            averageAmount = averageAmount,
            successfulTransactions = successfulTransactions,
            failedTransactions = failedTransactions,
            successRate = successRate,
            mostUsedService = mostUsedService,
            topServices = getServiceUsageStats()
        )
    }

    // Update operations
    suspend fun updateStatus(id: Long, status: TransactionStatus) {
        withContext(Dispatchers.IO) {
            transactionDao.updateStatus(id, status)
        }
    }

    suspend fun updateStatusWithError(id: Long, status: TransactionStatus, errorMessage: String) {
        withContext(Dispatchers.IO) {
            transactionDao.updateStatusWithError(id, status, errorMessage)
        }
    }

    suspend fun markAsPrinted(id: Long, printTime: Long = System.currentTimeMillis()) {
        withContext(Dispatchers.IO) {
            transactionDao.markAsPrinted(id, printTime)
        }
    }

    suspend fun updatePrintData(id: Long, printData: String) {
        withContext(Dispatchers.IO) {
            transactionDao.updatePrintData(id, printData)
        }
    }

    suspend fun updateResponseData(id: Long, responseData: String) {
        withContext(Dispatchers.IO) {
            transactionDao.updateResponseData(id, responseData)
        }
    }

    // Migration from SharedPreferences
    suspend fun migrateFromSharedPreferences() {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            
            if (prefs.getBoolean(MIGRATION_COMPLETED_KEY, false)) {
                AppLogger.d(TAG, "Migración ya completada anteriormente")
                return@withContext
            }

            try {
                AppLogger.i(TAG, "Iniciando migración desde SharedPreferences")
                
                // Aquí iría la lógica de migración específica del proyecto
                // Por ejemplo, migrar datos de transacciones guardadas en SharedPreferences
                migrateTransactionData(prefs)
                
                // Marcar migración como completada
                prefs.edit().putBoolean(MIGRATION_COMPLETED_KEY, true).apply()
                AppLogger.i(TAG, "Migración completada exitosamente")
                
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error durante la migración", e)
                throw e
            }
        }
    }

    private suspend fun migrateTransactionData(prefs: SharedPreferences) {
        // Ejemplo de migración - ajustar según la estructura de datos existente
        val transactionsToMigrate = mutableListOf<TransactionEntity>()
        
        // Si había datos de transacciones en SharedPreferences
        val allKeys = prefs.all.keys.filter { it.startsWith("transaction_") }
        
        for (key in allKeys) {
            try {
                val transactionJson = prefs.getString(key, null)
                if (!transactionJson.isNullOrEmpty()) {
                    // Aquí se parsearia el JSON y se convertiría a TransactionEntity
                    // Por ahora creamos una transacción de ejemplo
                    val transaction = TransactionEntity.create(
                        serviceId = 1,
                        serviceName = "Migrated Transaction",
                        phone = "",
                        cedula = "",
                        amount = 0,
                        commission = 0
                    )
                    transactionsToMigrate.add(transaction)
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Error migrando transacción $key", e)
            }
        }
        
        if (transactionsToMigrate.isNotEmpty()) {
            insertAll(transactionsToMigrate)
            AppLogger.i(TAG, "Migradas ${transactionsToMigrate.size} transacciones")
        }
    }

    // Legacy support - conversion from PrintData
    suspend fun insertFromPrintData(printData: PrintData): Long {
        val transaction = printDataToEntity(printData)
        return insert(transaction)
    }

    private fun printDataToEntity(printData: PrintData): TransactionEntity {
        val amount = extractAmount(printData.message)
        val commission = extractCommission(printData.message)
        
        return TransactionEntity.create(
            serviceId = 1, // Default service ID
            serviceName = printData.service,
            phone = printData.referenceData.ref1.takeIf { it.contains(Regex("\\d{10}")) } ?: "",
            cedula = printData.referenceData.ref2,
            amount = (amount / 100).toInt(), // Convert from cents
            commission = (commission / 100).toInt()
        ).copy(
            status = TransactionStatus.COMPLETED,
            printData = printData.message,
            reference = generateReferenceNumber()
        )
    }

    private fun extractAmount(message: String): Long {
        val regex = Regex("Monto: ([\\d,]+) Gs\\.")
        val match = regex.find(message)
        val amountStr = match?.groupValues?.get(1)?.replace(",", "") ?: "0"
        return (amountStr.toLongOrNull() ?: 0L) * 100 // Convert to cents
    }

    private fun extractCommission(message: String): Long {
        val regex = Regex("Comisión: ([\\d,]+) Gs\\.")
        val match = regex.find(message)
        val commissionStr = match?.groupValues?.get(1)?.replace(",", "") ?: "0"
        return (commissionStr.toLongOrNull() ?: 0L) * 100 // Convert to cents
    }
    
    private fun generateReferenceNumber(): String {
        return "REF${System.currentTimeMillis()}"
    }

    // Automatic cleanup operations removed - manual maintenance preferred

    // Export and duplicate detection removed - handled by higher-level managers
}