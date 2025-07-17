package com.example.geniotecni.tigo.managers

import android.content.Context
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

class StatisticsManager(private val context: Context) {
    
    data class Statistics(
        val totalTransactions: Int,
        val totalAmount: Long,
        val averageAmount: Long,
        val mostUsedService: String,
        val transactionsByService: Map<String, Int>,
        val transactionsByMonth: Map<String, Int>,
        val transactionsByDay: Map<String, Int>,
        val dailyAverage: Double,
        val successRate: Double,
        val peakHour: Int,
        val totalCommission: Long,
        val commissionByService: Map<String, Long>
    )
    
    data class ServiceStats(
        val serviceName: String,
        val count: Int,
        val totalAmount: Long,
        val percentage: Double
    )
    
    private val printDataManager = PrintDataManager(context)
    
    fun calculateStatistics(fromDate: Date? = null): Statistics {
        val allHistory = printDataManager.getAllPrintData()
        
        // Filter by date if provided
        val history = if (fromDate != null) {
            val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
            allHistory.filter { data ->
                try {
                    val dataDate = dateFormat.parse(data.date)
                    dataDate != null && dataDate.after(fromDate)
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            allHistory
        }
        
        val totalTransactions = history.size
        var totalAmount = 0L
        var totalCommission = 0L
        val serviceCount = mutableMapOf<String, Int>()
        val serviceCommission = mutableMapOf<String, Long>()
        val monthCount = mutableMapOf<String, Int>()
        val dayCount = mutableMapOf<String, Int>()
        val hourCount = mutableMapOf<Int, Int>()
        var successfulTransactions = 0
        
        history.forEach { data ->
            // Extract amount
            val montoRegex = Regex("""Monto: (\d+) Gs\.""")
            val match = montoRegex.find(data.message)
            match?.let {
                val amount = it.groupValues[1].toLongOrNull() ?: 0L
                totalAmount += amount
            }
            
            // Extract or calculate commission
            var commission = 0L
            val commissionRegex = Regex("""Comision: (\d+) Gs\.""")
            val commissionMatch = commissionRegex.find(data.message)
            
            if (commissionMatch != null) {
                // Use existing commission from message
                commission = commissionMatch.groupValues[1].toLongOrNull() ?: 0L
            } else {
                // Calculate commission for specific services (1% rate)
                val amount = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                commission = calculateCommissionForService(data.service, amount)
            }
            
            totalCommission += commission
            serviceCommission[data.service] = serviceCommission.getOrDefault(data.service, 0L) + commission
            
            // Count by service
            serviceCount[data.service] = serviceCount.getOrDefault(data.service, 0) + 1
            
            // Count by month
            val month = data.date.substring(3, 10) // MM-yyyy
            monthCount[month] = monthCount.getOrDefault(month, 0) + 1
            
            // Count by day of week
            try {
                val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
                val date = dateFormat.parse(data.date)
                val calendar = Calendar.getInstance()
                calendar.time = date!!
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                dayCount[getDayName(dayOfWeek)] = dayCount.getOrDefault(getDayName(dayOfWeek), 0) + 1
            } catch (e: Exception) {
                // Handle parse error
            }
            
            // Count by hour
            try {
                val hour = data.time.substring(0, 2).toInt()
                hourCount[hour] = hourCount.getOrDefault(hour, 0) + 1
            } catch (e: Exception) {
                // Handle parse error
            }
            
            // Count successful transactions (has reference)
            if (data.referenceData.ref1.isNotEmpty() || data.referenceData.ref2.isNotEmpty()) {
                successfulTransactions++
            }
        }
        
        val averageAmount = if (totalTransactions > 0) totalAmount / totalTransactions else 0L
        val mostUsedService = serviceCount.maxByOrNull { it.value }?.key ?: "N/A"
        val peakHour = hourCount.maxByOrNull { it.value }?.key ?: 0
        val successRate = if (totalTransactions > 0) {
            (successfulTransactions.toDouble() / totalTransactions) * 100
        } else 0.0
        
        // Calculate daily average
        val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())
        val uniqueDays = history.map { data ->
            try {
                dateFormat.parse(data.date)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }.filter { it > 0 }.toSet().size
        
        val dailyAverage = if (uniqueDays > 0) totalTransactions.toDouble() / uniqueDays else 0.0
        
        return Statistics(
            totalTransactions = totalTransactions,
            totalAmount = totalAmount,
            averageAmount = averageAmount,
            mostUsedService = mostUsedService,
            transactionsByService = serviceCount,
            transactionsByMonth = monthCount,
            transactionsByDay = dayCount,
            dailyAverage = dailyAverage,
            successRate = successRate,
            peakHour = peakHour,
            totalCommission = totalCommission,
            commissionByService = serviceCommission
        )
    }
    
    fun getTopServices(limit: Int = 5): List<ServiceStats> {
        val history = printDataManager.getAllPrintData()
        val serviceData = mutableMapOf<String, Pair<Int, Long>>() // service -> (count, totalAmount)
        
        history.forEach { data ->
            val montoRegex = Regex("""Monto: (\d+) Gs\.""")
            val match = montoRegex.find(data.message)
            val amount = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            
            val current = serviceData.getOrDefault(data.service, Pair(0, 0L))
            serviceData[data.service] = Pair(current.first + 1, current.second + amount)
        }
        
        val total = history.size.toDouble()
        
        return serviceData.map { (service, data) ->
            ServiceStats(
                serviceName = service,
                count = data.first,
                totalAmount = data.second,
                percentage = if (total > 0) (data.first / total) * 100 else 0.0
            )
        }.sortedByDescending { it.count }.take(limit)
    }
    
    fun getMonthlyTrend(): Map<String, Int> {
        val history = printDataManager.getAllPrintData()
        val monthlyData = mutableMapOf<String, Int>()
        
        history.forEach { data ->
            val month = data.date.substring(3, 10) // MM-yyyy
            monthlyData[month] = monthlyData.getOrDefault(month, 0) + 1
        }
        
        return monthlyData.toSortedMap()
    }
    
    private fun calculateCommissionForService(serviceName: String, amount: Long): Long {
        // Define services that generate commission with 1% rate
        val commissionServices = setOf(
            "Giros Tigo",
            "ANDE",
            "Recarga Tigo",
            "Billetera Tigo Money",
            "Telefonia Tigo"
        )
        
        return if (commissionServices.contains(serviceName) && amount > 0) {
            (amount * 0.01).toLong() // 1% commission
        } else {
            0L
        }
    }
    
    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Domingo"
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            else -> "Desconocido"
        }
    }
}