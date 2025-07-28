package com.example.geniotecni.tigo.data.dao

data class DailyStatisticsRaw(
    val date: String,
    val transactionCount: Int,
    val totalAmount: Long,
    val totalCommission: Long,
    val successfulCount: Int,
    val failedCount: Int
)
