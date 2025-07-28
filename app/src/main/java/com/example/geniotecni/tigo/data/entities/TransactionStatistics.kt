package com.example.geniotecni.tigo.data.entities

import com.example.geniotecni.tigo.utils.formatAsCurrency

// Data class for transaction statistics
data class TransactionStatistics(
    val totalTransactions: Int = 0,
    val totalAmount: Long = 0, // in cents
    val totalCommission: Long = 0, // in cents
    val averageAmount: Long = 0, // in cents
    val successfulTransactions: Int = 0,
    val failedTransactions: Int = 0,
    val successRate: Float = 0f,
    val mostUsedService: String? = null,
    val dailyAverage: Float = 0f,
    val peakHour: Int = 0,
    val topServices: List<ServiceUsage> = emptyList()
)
{
    fun getFormattedTotalAmount(): String = (totalAmount / 100.0).formatAsCurrency()
    fun getFormattedTotalCommission(): String = (totalCommission / 100.0).formatAsCurrency()
    fun getFormattedAverageAmount(): String = (averageAmount / 100.0).formatAsCurrency()
}