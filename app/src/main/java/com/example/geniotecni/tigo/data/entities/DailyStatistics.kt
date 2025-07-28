package com.example.geniotecni.tigo.data.entities

import com.example.geniotecni.tigo.utils.formatAsCurrency

// Data class for daily statistics
data class DailyStatistics(
    val date: String,
    val transactionCount: Int,
    val totalAmount: Long, // in cents
    val totalCommission: Long, // in cents
    val successfulCount: Int,
    val failedCount: Int
) {
    fun getFormattedAmount(): String = (totalAmount / 100.0).formatAsCurrency()
    fun getFormattedCommission(): String = (totalCommission / 100.0).formatAsCurrency()
    fun getSuccessRate(): Float = if (transactionCount > 0) (successfulCount.toFloat() / transactionCount) * 100f else 0f
}
