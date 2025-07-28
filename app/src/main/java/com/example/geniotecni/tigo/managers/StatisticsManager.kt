package com.example.geniotecni.tigo.managers

import android.content.Context
import com.example.geniotecni.tigo.data.database.AppDatabase
import com.example.geniotecni.tigo.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class StatisticsManager(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val repository = TransactionRepository(database.transactionDao(), context)

    data class Statistics(
        val totalTransactions: Int = 0,
        val totalAmount: Long = 0,
        val averageAmount: Long = 0,
        val successRate: Int = 100,
        val mostUsedService: String? = null,
        val dailyAverage: Double = 0.0,
        val totalCommission: Long = 0,
        val peakHour: String? = null,
        val serviceBreakdown: Map<String, Int> = emptyMap(),
        val monthlyData: List<Pair<Int, Double>> = emptyList()
    )

    fun getStatistics(daysFilter: Int): Statistics = runBlocking {
        withContext(Dispatchers.IO) {
            val totalTransactions = repository.getTransactionCount()
            val totalAmount = repository.getTotalAmount()
            val averageAmount = repository.getAverageAmount().toLong()
            val mostUsedService = repository.getMostUsedServiceName()
            val totalCommission = repository.getTotalCommission()
            val peakHour: String? = null // TODO: Implement peak hour calculation

            val serviceBreakdown: Map<String, Int> = emptyMap() // TODO: Implement service breakdown

            // Calculate daily average
            val daysToCalculate = if (daysFilter == -1) 365 else daysFilter
            val dailyAverage = if (daysToCalculate > 0) {
                totalTransactions.toDouble() / daysToCalculate
            } else 0.0

            Statistics(
                totalTransactions = totalTransactions,
                totalAmount = totalAmount,
                averageAmount = averageAmount,
                successRate = 100,
                mostUsedService = mostUsedService,
                dailyAverage = dailyAverage,
                totalCommission = totalCommission,
                peakHour = peakHour,
                serviceBreakdown = serviceBreakdown,
                monthlyData = emptyList() // TODO: Implement monthly data query
            )
        }
    }
}