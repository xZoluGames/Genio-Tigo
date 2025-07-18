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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.*

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

    // Charts
    private lateinit var servicesChart: PieChart
    private lateinit var monthlyChart: BarChart

    enum class TimeFilter(val displayName: String, val days: Int) {
        TODAY("Hoy", 1),
        WEEK("Esta semana", 7),
        MONTH("Este mes", 30),
        YEAR("Este año", 365),
        ALL_TIME("Todo el tiempo", -1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        statisticsManager = StatisticsManager(this)

        setupToolbar()
        initializeViews()
        setupTimeFilters()
        loadStatistics()
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Estadísticas"
        }
    }

    private fun initializeViews() {
        // Time filter buttons
        todayButton = findViewById(R.id.todayButton)
        weekButton = findViewById(R.id.weekButton)
        monthButton = findViewById(R.id.monthButton)
        yearButton = findViewById(R.id.yearButton)
        allTimeButton = findViewById(R.id.allTimeButton)

        // Statistics views
        totalTransactionsText = findViewById(R.id.totalTransactionsText)
        totalAmountText = findViewById(R.id.totalAmountText)
        averageAmountText = findViewById(R.id.averageAmountText)
        successRateText = findViewById(R.id.successRateText)
        mostUsedServiceText = findViewById(R.id.mostUsedServiceText)
        dailyAverageText = findViewById(R.id.dailyAverageText)
        totalCommissionText = findViewById(R.id.totalCommissionText)
        peakHourText = findViewById(R.id.peakHourText)

        // Charts
        servicesChart = findViewById(R.id.servicesChart)
        monthlyChart = findViewById(R.id.monthlyChart)

        setupCharts()
    }

    private fun setupCharts() {
        // Setup Pie Chart
        servicesChart.apply {
            description.isEnabled = false
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
            centerText = "Servicios"
            setCenterTextSize(16f)
            setDrawCenterText(true)
            legend.isEnabled = true
            setUsePercentValues(true)
        }

        // Setup Bar Chart
        monthlyChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    private val months = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                    override fun getFormattedValue(value: Float): String {
                        return if (value.toInt() in months.indices) {
                            months[value.toInt()]
                        } else ""
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun setupTimeFilters() {
        val filterButtons = mapOf(
            todayButton to TimeFilter.TODAY,
            weekButton to TimeFilter.WEEK,
            monthButton to TimeFilter.MONTH,
            yearButton to TimeFilter.YEAR,
            allTimeButton to TimeFilter.ALL_TIME
        )

        filterButtons.forEach { (button, filter) ->
            button.setOnClickListener {
                currentTimeFilter = filter
                updateFilterButtonStates()
                loadStatistics()
            }
        }

        // Set initial state
        updateFilterButtonStates()
    }

    private fun updateFilterButtonStates() {
        val buttons = listOf(todayButton, weekButton, monthButton, yearButton, allTimeButton)
        val filters = listOf(TimeFilter.TODAY, TimeFilter.WEEK, TimeFilter.MONTH, TimeFilter.YEAR, TimeFilter.ALL_TIME)

        buttons.forEachIndexed { index, button ->
            val isSelected = filters[index] == currentTimeFilter
            button.apply {
                setBackgroundColor(if (isSelected) getColor(R.color.primary_blue) else Color.GRAY)
                setTextColor(Color.WHITE)
            }
        }
    }

    private fun loadStatistics() {
        val stats = statisticsManager.getStatistics(currentTimeFilter.days)

        // Update text views
        totalTransactionsText.text = stats.totalTransactions.toString()
        totalAmountText.text = formatAmount(stats.totalAmount)
        averageAmountText.text = formatAmount(stats.averageAmount)
        successRateText.text = "${stats.successRate}%"
        mostUsedServiceText.text = stats.mostUsedService ?: "N/A"
        dailyAverageText.text = String.format("%.1f", stats.dailyAverage)
        totalCommissionText.text = formatAmount(stats.totalCommission)
        peakHourText.text = stats.peakHour ?: "N/A"

        // Update charts
        updateServicesChart(stats.serviceBreakdown)
        updateMonthlyChart(stats.monthlyData)
    }

    private fun updateServicesChart(serviceBreakdown: Map<String, Int>) {
        if (serviceBreakdown.isEmpty()) {
            servicesChart.clear()
            servicesChart.invalidate()
            return
        }

        val entries = serviceBreakdown.map { (service, count) ->
            PieEntry(count.toFloat(), service)
        }

        val dataSet = PieDataSet(entries, "Servicios").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            sliceSpace = 3f
            selectionShift = 5f
            valueTextSize = 10f
            valueTextColor = Color.WHITE
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}"
                }
            })
        }

        servicesChart.data = data
        servicesChart.animateY(1000)
        servicesChart.invalidate()
    }

    private fun updateMonthlyChart(monthlyData: Map<Int, Long>) {
        if (monthlyData.isEmpty()) {
            monthlyChart.clear()
            monthlyChart.invalidate()
            return
        }

        val entries = monthlyData.map { (month, amount) ->
            BarEntry(month.toFloat(), amount.toFloat())
        }

        val dataSet = BarDataSet(entries, "Monto mensual").apply {
            colors = listOf(Color.parseColor("#2196F3"))
            valueTextSize = 10f
        }

        val data = BarData(dataSet).apply {
            barWidth = 0.9f
            setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatAmount(value.toLong())
                }
            })
        }

        monthlyChart.data = data
        monthlyChart.animateY(1000)
        monthlyChart.invalidate()
    }

    private fun formatAmount(amount: Long): String {
        return String.format("%,d Gs.", amount)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}