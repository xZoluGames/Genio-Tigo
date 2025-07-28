package com.example.geniotecni.tigo.managers

import android.content.Context
import com.example.geniotecni.tigo.utils.BaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 💰 GESTOR DE USO DE MONTOS - Sistema Inteligente de Sugerencias
 * 
 * PROPÓSITO PRINCIPAL:
 * - Tracking inteligente de montos más utilizados por el usuario
 * - Generación automática de sugerencias de montos frecuentes
 * - Optimización de UX mediante chips de acceso rápido
 * - Analytics de patrones de uso para mejora continua
 * 
 * FUNCIONALIDADES AVANZADAS:
 * - Registro asíncrono de uso de montos con timestamps
 * - Algoritmo de ranking por frecuencia y recencia
 * - Limpieza automática de datos antiguos (>90 días)
 * - Estadísticas detalladas de comportamiento de usuario
 * - Limitación inteligente a los montos más relevantes
 * 
 * ARQUITECTURA ASÍNCRONA:
 * - Operaciones I/O en Dispatchers.IO para rendimiento
 * - Persistencia en JSON con manejo robusto de errores
 * - Herencia de BaseManager para SharedPreferences optimizado
 * - Coroutines para operaciones no bloqueantes
 * 
 * ALGORITMO DE SUGERENCIAS:
 * - Prioriza montos con 2+ usos para evitar sugerencias únicas
 * - Combina frecuencia de uso con recencia temporal
 * - Mantiene máximo 20 montos para rendimiento óptimo
 * - Ordenamiento dual: usageCount DESC, lastUsed DESC
 * 
 * CASOS DE USO PRINCIPALES:
 * - MainActivity: Chips de montos frecuentes para entrada rápida
 * - Analytics: Comprensión de patrones de transacciones
 * - UX Optimization: Reducción de tiempo de entrada de datos
 * 
 * CONEXIONES ARQUITECTÓNICAS:
 * - HEREDA DE: BaseManager para persistencia optimizada
 * - USADO POR: MainActivity para chips de montos rápidos
 * - UTILIZA: Kotlin Coroutines para operaciones asíncronas
 * - PERSISTE EN: SharedPreferences con formato JSON estructurado
 */
class AmountUsageManager(context: Context) : BaseManager(context, "AmountUsageManager") {

    companion object {
        private const val KEY_AMOUNT_USAGE = "amount_usage_data"
        private const val MAX_TRACKED_AMOUNTS = 20
    }

    data class AmountUsageData(
        val amount: Long,
        val usageCount: Int,
        val lastUsed: Long
    )

    /**
     * Registra el uso de un monto
     */
    suspend fun recordAmountUsage(amount: Long) = withContext(Dispatchers.IO) {
        if (amount <= 0) return@withContext

        val usageData = loadUsageData().toMutableMap()
        val currentData = usageData[amount]

        usageData[amount] = AmountUsageData(
            amount = amount,
            usageCount = (currentData?.usageCount ?: 0) + 1,
            lastUsed = System.currentTimeMillis()
        )

        saveUsageData(usageData)
    }

    /**
     * Obtiene los montos más utilizados
     * @param limit Número máximo de montos a retornar
     * @return Lista de montos ordenados por frecuencia de uso
     */
    suspend fun getTopUsedAmounts(limit: Int = 5): List<AmountUsageData> = withContext(Dispatchers.IO) {
        val usageData = loadUsageData()

        // Filtrar montos con al menos 2 usos para evitar mostrar montos únicos
        return@withContext usageData.values
            .filter { it.usageCount >= 2 }
            .sortedWith(compareByDescending<AmountUsageData> { it.usageCount }
                .thenByDescending { it.lastUsed })
            .take(limit)
    }

    /**
     * Obtiene los montos más recientes
     * @param limit Número máximo de montos a retornar
     * @return Lista de montos ordenados por uso reciente
     */
    suspend fun getRecentAmounts(limit: Int = 5): List<AmountUsageData> = withContext(Dispatchers.IO) {
        val usageData = loadUsageData()

        return@withContext usageData.values
            .sortedByDescending { it.lastUsed }
            .take(limit)
    }

    /**
     * Limpia montos antiguos (más de 90 días sin usar)
     */
    suspend fun cleanOldAmounts() = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L) // 90 días
        val usageData = loadUsageData()

        val filteredData = usageData.filterValues { it.lastUsed > cutoffTime }

        if (filteredData.size < usageData.size) {
            saveUsageData(filteredData)
        }
    }

    /**
     * Obtiene estadísticas de uso de montos
     */
    suspend fun getAmountStatistics(): AmountStatistics = withContext(Dispatchers.IO) {
        val usageData = loadUsageData()

        if (usageData.isEmpty()) {
            return@withContext AmountStatistics()
        }

        val amounts = usageData.keys.toList()
        val totalUsage = usageData.values.sumOf { it.usageCount }
        val mostUsed = usageData.values.maxByOrNull { it.usageCount }

        AmountStatistics(
            totalDifferentAmounts = amounts.size,
            totalUsageCount = totalUsage,
            averageAmount = amounts.average().toLong(),
            mostUsedAmount = mostUsed?.amount,
            mostUsedCount = mostUsed?.usageCount ?: 0,
            minAmount = amounts.minOrNull() ?: 0,
            maxAmount = amounts.maxOrNull() ?: 0
        )
    }

    /**
     * Carga los datos de uso desde SharedPreferences
     */
    private fun loadUsageData(): Map<Long, AmountUsageData> {
        val jsonString = getString(KEY_AMOUNT_USAGE)
        if (jsonString.isEmpty()) return emptyMap()

        return try {
            val jsonArray = JSONArray(jsonString)
            val dataMap = mutableMapOf<Long, AmountUsageData>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val amount = jsonObject.getLong("amount")
                val data = AmountUsageData(
                    amount = amount,
                    usageCount = jsonObject.getInt("usageCount"),
                    lastUsed = jsonObject.getLong("lastUsed")
                )
                dataMap[amount] = data
            }

            dataMap
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Guarda los datos de uso en SharedPreferences
     */
    private fun saveUsageData(data: Map<Long, AmountUsageData>) {
        val jsonArray = JSONArray()

        // Limitar a los MAX_TRACKED_AMOUNTS más relevantes
        val sortedData = data.values
            .sortedWith(compareByDescending<AmountUsageData> { it.usageCount }
                .thenByDescending { it.lastUsed })
            .take(MAX_TRACKED_AMOUNTS)

        sortedData.forEach { amountData ->
            val jsonObject = JSONObject().apply {
                put("amount", amountData.amount)
                put("usageCount", amountData.usageCount)
                put("lastUsed", amountData.lastUsed)
            }
            jsonArray.put(jsonObject)
        }

        savePreference(KEY_AMOUNT_USAGE, jsonArray.toString())
    }

    /**
     * Restablece todos los datos de uso
     */
    fun clearAllData() {
        prefs.edit().remove(KEY_AMOUNT_USAGE).apply()
    }

    /**
     * Clase para estadísticas de montos
     */
    data class AmountStatistics(
        val totalDifferentAmounts: Int = 0,
        val totalUsageCount: Int = 0,
        val averageAmount: Long = 0,
        val mostUsedAmount: Long? = null,
        val mostUsedCount: Int = 0,
        val minAmount: Long = 0,
        val maxAmount: Long = 0
    )
}