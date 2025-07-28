package com.example.geniotecni.tigo.ui.activities

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import com.example.geniotecni.tigo.R
import com.example.geniotecni.tigo.utils.BaseActivity
// import com.example.geniotecni.tigo.core.extensions.showToast
import com.example.geniotecni.tigo.managers.StatisticsManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.DecimalFormat
import java.util.*
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import com.example.geniotecni.tigo.ui.viewmodels.StatisticsViewModel

@AndroidEntryPoint
class StatisticsActivity : BaseActivity() {
    
    override val tag = "StatisticsActivity"

    private lateinit var statisticsManager: StatisticsManager
    
    // ViewModel with dependency injection
    private val viewModel: StatisticsViewModel by viewModels()

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

    // Charts - nullable since they might not exist in layout
    private var servicesChart: PieChart? = null
    private var monthlyChart: BarChart? = null

    // Cards for empty states - nullable since they might not exist in layout
    private var emptyStateCard: CardView? = null
    private var servicesChartCard: CardView? = null
    private var monthlyChartCard: CardView? = null

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

        initializeViews()
        setupTimeFilterButtons()
        setupCharts()
        loadStatistics()
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

        // Charts - try to find but don't fail if they don't exist
        servicesChart = findViewById(R.id.servicesChart)
        monthlyChart = findViewById(R.id.monthlyChart)

        // Cards - try to find but don't fail if they don't exist (these may not exist in current layout)
        emptyStateCard = null // findViewById(R.id.emptyStateCard)
        servicesChartCard = null // findViewById(R.id.servicesChartCard) 
        monthlyChartCard = null // findViewById(R.id.monthlyChartCard)

        // Setup toolbar
        supportActionBar?.apply {
            title = "Estadísticas"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupTimeFilterButtons() {
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
                setBackgroundColor(if (isSelected) getColor(R.color.md_theme_light_primary) else getColor(R.color.md_theme_light_surfaceVariant))
                setTextColor(if (isSelected) Color.WHITE else getColor(R.color.md_theme_light_onSurfaceVariant))
            }
        }
    }

    private fun setupCharts() {
        // Setup Pie Chart (Services)
        servicesChart?.apply {
            description.isEnabled = false
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
            centerText = "Servicios"
            setCenterTextSize(16f)
            setDrawCenterText(true)
            transparentCircleRadius = 58f
            holeRadius = 50f
            setHoleColor(Color.WHITE)
            animateY(1000)

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 5f
                textSize = 12f
            }
        }

        // Setup Bar Chart (Monthly)
        monthlyChart?.apply {
            description.isEnabled = false
            setDrawValueAboveBar(true)
            setDrawGridBackground(false)
            animateY(1000)
            setScaleEnabled(false)
            setPinchZoom(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textSize = 12f
                valueFormatter = object : ValueFormatter() {
                    private val months = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                    override fun getFormattedValue(value: Float): String {
                        return if (value.toInt() in 0..11) months[value.toInt()] else ""
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(true)
                granularity = 1f
                axisMinimum = 0f
                textSize = 12f
            }

            axisRight.isEnabled = false

            legend.apply {
                form = Legend.LegendForm.SQUARE
                textSize = 12f
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
        }
    }

    private fun loadStatistics() {
        val stats = statisticsManager.getStatistics(currentTimeFilter.days)

        // Check if there's data
        if (stats.totalTransactions == 0) {
            showEmptyState()
            return
        }

        hideEmptyState()

        // Update text views
        totalTransactionsText.text = stats.totalTransactions.toString()
        totalAmountText.text = formatAmount(stats.totalAmount.toDouble())
        averageAmountText.text = formatAmount(stats.averageAmount.toDouble())

        // Use the success rate from statistics
        successRateText.text = "${stats.successRate}%"

        mostUsedServiceText.text = stats.mostUsedService ?: "N/A"
        dailyAverageText.text = String.format("%.1f", stats.dailyAverage)
        totalCommissionText.text = formatAmount(stats.totalCommission.toDouble())
        peakHourText.text = stats.peakHour ?: "N/A"

        // Update charts with real data
        updateServicesChart(stats.serviceBreakdown)
        updateMonthlyChart(stats.monthlyData)
    }

    private fun showEmptyState() {
        emptyStateCard?.visibility = View.VISIBLE
        servicesChartCard?.visibility = View.GONE
        monthlyChartCard?.visibility = View.GONE

        // Set all stats to 0 or N/A
        totalTransactionsText.text = "0"
        totalAmountText.text = "Gs. 0"
        averageAmountText.text = "Gs. 0"
        successRateText.text = "0%"
        mostUsedServiceText.text = "N/A"
        dailyAverageText.text = "0"
        totalCommissionText.text = "Gs. 0"
        peakHourText.text = "N/A"
    }

    private fun hideEmptyState() {
        emptyStateCard?.visibility = View.GONE
        servicesChartCard?.visibility = View.VISIBLE
        monthlyChartCard?.visibility = View.VISIBLE
    }

    private fun updateServicesChart(serviceBreakdown: Map<String, Int>) {
        if (serviceBreakdown.isEmpty() || servicesChart == null) {
            servicesChart?.clear()
            servicesChart?.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        serviceBreakdown.forEach { (service, count) ->
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), service))
            }
        }

        // Use custom colors
        colors.addAll(listOf(
            getColor(R.color.chart_color_1),
            getColor(R.color.chart_color_2),
            getColor(R.color.chart_color_3),
            getColor(R.color.chart_color_4),
            getColor(R.color.chart_color_5)
        ))

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            selectionShift = 5f
            valueFormatter = PercentFormatter(servicesChart)
            valueTextSize = 11f
            valueTextColor = Color.WHITE
        }

        val data = PieData(dataSet)
        servicesChart?.data = data
        servicesChart?.invalidate()
    }

    private fun updateMonthlyChart(monthlyData: List<Pair<Int, Double>>) {
        if (monthlyData.isEmpty() || monthlyChart == null) {
            monthlyChart?.clear()
            monthlyChart?.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        monthlyData.forEach { (month, amount) ->
            entries.add(BarEntry(month.toFloat(), amount.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Monto mensual (Gs.)").apply {
            color = getColor(R.color.md_theme_light_primary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return formatShortAmount(value.toDouble())
                }
            }
        }

        val data = BarData(dataSet).apply {
            barWidth = 0.7f
        }

        monthlyChart?.data = data
        monthlyChart?.setFitBars(true)
        monthlyChart?.invalidate()
    }

    private fun formatAmount(amount: Double): String {
        val formatter = DecimalFormat("#,###")
        return "Gs. ${formatter.format(amount)}"
    }

    private fun formatShortAmount(amount: Double): String {
        return when {
            amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000)
            amount >= 1_000 -> String.format("%.1fK", amount / 1_000)
            else -> amount.toInt().toString()
        }
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