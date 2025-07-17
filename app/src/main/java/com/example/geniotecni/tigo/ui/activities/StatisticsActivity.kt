package com.example.geniotecni.tigo.ui.activities

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.managers.StatisticsManager
import com.example.geniotecni.tigo.utils.showToast
import java.util.*
// Charts imports are commented out for now to prevent crashes
// import com.github.mikephil.charting.charts.BarChart
// import com.github.mikephil.charting.charts.PieChart
// import com.github.mikephil.charting.components.XAxis
// import com.github.mikephil.charting.data.*
// import com.github.mikephil.charting.formatter.ValueFormatter
// import com.github.mikephil.charting.utils.ColorTemplate

class StatisticsActivity : AppCompatActivity() {
    
    private lateinit var statisticsManager: StatisticsManager
    
    // Time filter buttons
    private lateinit var todayButton: Button
    private lateinit var weekButton: Button
    private lateinit var monthButton: Button
    private lateinit var yearButton: Button
    private lateinit var allTimeButton: Button
    private var currentTimeFilter = TimeFilter.ALL_TIME
    
    // Views
    private lateinit var totalTransactionsText: TextView
    private lateinit var totalAmountText: TextView
    private lateinit var averageAmountText: TextView
    private lateinit var successRateText: TextView
    private lateinit var mostUsedServiceText: TextView
    private lateinit var dailyAverageText: TextView
    private lateinit var totalCommissionText: TextView
    private lateinit var peakHourText: TextView
    
    enum class TimeFilter(val displayName: String, val days: Int) {
        TODAY("Hoy", 1),
        WEEK("Esta semana", 7),
        MONTH("Este mes", 30),
        YEAR("Este año", 365),
        ALL_TIME("Todo el tiempo", -1)
    }
    
    // Charts are commented out for now to prevent crashes
    // private lateinit var servicesChart: PieChart
    // private lateinit var monthlyChart: BarChart
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        
        supportActionBar?.apply {
            title = "Estadísticas"
            setDisplayHomeAsUpEnabled(true)
        }
        
        statisticsManager = StatisticsManager(this)
        
        initializeViews()
        setupTimeFilters()
        loadStatistics()
    }
    
    private fun initializeViews() {
        // Time filter buttons
        todayButton = findViewById(R.id.todayButton)
        weekButton = findViewById(R.id.weekButton)
        monthButton = findViewById(R.id.monthButton)
        yearButton = findViewById(R.id.yearButton)
        allTimeButton = findViewById(R.id.allTimeButton)
        
        // Summary cards
        totalTransactionsText = findViewById(R.id.totalTransactionsText)
        totalAmountText = findViewById(R.id.totalAmountText)
        averageAmountText = findViewById(R.id.averageAmountText)
        successRateText = findViewById(R.id.successRateText)
        mostUsedServiceText = findViewById(R.id.mostUsedServiceText)
        dailyAverageText = findViewById(R.id.dailyAverageText)
        totalCommissionText = findViewById(R.id.totalCommissionText)
        peakHourText = findViewById(R.id.peakHourText)
        
        // Charts are commented out for now to prevent crashes
        // servicesChart = findViewById(R.id.servicesChart)
        // monthlyChart = findViewById(R.id.monthlyChart)
        
        // setupCharts()
    }
    
    private fun setupTimeFilters() {
        val buttons = listOf(
            todayButton to TimeFilter.TODAY,
            weekButton to TimeFilter.WEEK,
            monthButton to TimeFilter.MONTH,
            yearButton to TimeFilter.YEAR,
            allTimeButton to TimeFilter.ALL_TIME
        )
        
        buttons.forEach { (button, filter) ->
            button.setOnClickListener {
                // Update button states
                buttons.forEach { (btn, _) -> btn.isSelected = false }
                button.isSelected = true
                
                currentTimeFilter = filter
                loadStatistics()
            }
        }
        
        // Set default selection
        allTimeButton.isSelected = true
    }
    
    // Charts functionality temporarily disabled
    // private fun setupCharts() {
    //     // Will be implemented when chart library is properly configured
    // }
    
    private fun loadStatistics() {
        try {
            // Calculate date range based on current filter
            val fromDate = if (currentTimeFilter.days == -1) {
                null // All time
            } else {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -currentTimeFilter.days)
                calendar.time
            }
            
            val stats = statisticsManager.calculateStatistics(fromDate)
            
            // Update summary cards
            totalTransactionsText.text = stats.totalTransactions.toString()
            totalAmountText.text = formatAmount(stats.totalAmount)
            averageAmountText.text = formatAmount(stats.averageAmount)
            successRateText.text = "${String.format("%.1f", stats.successRate)}%"
            mostUsedServiceText.text = stats.mostUsedService
            dailyAverageText.text = String.format("%.1f", stats.dailyAverage)
            totalCommissionText.text = formatAmount(stats.totalCommission)
            peakHourText.text = "${stats.peakHour}:00"
            
            // Update charts (commented out for now)
            // updateServicesChart(stats.transactionsByService)
            // updateMonthlyChart(stats.transactionsByMonth)
            
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error al cargar estadísticas")
        }
    }
    
    // Charts functionality temporarily disabled
    // private fun updateServicesChart(data: Map<String, Int>) {
    //     // Will be implemented when chart library is properly configured
    // }
    
    // Charts functionality temporarily disabled
    // private fun updateMonthlyChart(data: Map<String, Int>) {
    //     // Will be implemented when chart library is properly configured
    // }
    
    private fun formatAmount(amount: Long): String {
        return "${String.format("%,d", amount)} Gs."
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}