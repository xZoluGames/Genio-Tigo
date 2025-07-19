package com.example.geniotecni.tigo.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.geniotecni.tigo.utils.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class AmountUsageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AmountUsageManager"
        private const val PREFS_NAME = "amount_usage_prefs"
        private const val KEY_AMOUNTS_DATA = "amounts_data"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val MAX_TOP_AMOUNTS = 5
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    data class AmountUsage(
        val amount: String,
        val count: Int,
        val lastUsed: Long
    )
    
    /**
     * Registra el uso de un monto específico
     */
    fun recordAmountUsage(amount: String) {
        try {
            // Limpiar el monto (remover comas, espacios, etc.)
            val cleanAmount = amount.replace(",", "").replace(".", "").trim()
            
            // Validar que sea un número válido
            val amountValue = cleanAmount.toLongOrNull()
            if (amountValue == null || amountValue <= 0) {
                Log.w(TAG, "Monto inválido ignorado: $amount")
                return
            }
            
            val currentData = getAmountsData()
            val existingAmount = currentData.find { it.amount == cleanAmount }
            
            val updatedData = if (existingAmount != null) {
                // Incrementar contador del monto existente
                currentData.map { 
                    if (it.amount == cleanAmount) {
                        it.copy(count = it.count + 1, lastUsed = System.currentTimeMillis())
                    } else {
                        it
                    }
                }
            } else {
                // Agregar nuevo monto
                currentData + AmountUsage(cleanAmount, 1, System.currentTimeMillis())
            }
            
            saveAmountsData(updatedData)
            AppLogger.d(TAG, "Uso de monto registrado: $cleanAmount")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando uso de monto: $amount", e)
        }
    }
    
    /**
     * Obtiene los top 5 montos más utilizados
     */
    fun getTopUsedAmounts(): List<String> {
        return try {
            val data = getAmountsData()
            
            // Ordenar SOLO por frecuencia de uso total (descendente)
            // NO por recencia - queremos los más utilizados de todo el tiempo
            data.sortedByDescending { it.count }
                .take(MAX_TOP_AMOUNTS)
                .map { formatAmount(it.amount) }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo montos más usados", e)
            getDefaultAmounts()
        }
    }
    
    /**
     * Obtiene estadísticas de uso de montos
     */
    fun getAmountUsageStats(): Map<String, Int> {
        return try {
            getAmountsData().associate { it.amount to it.count }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo estadísticas de montos", e)
            emptyMap()
        }
    }
    
    /**
     * Limpia datos antiguos (más de 30 días sin uso)
     */
    fun cleanOldData() {
        try {
            val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // 30 días
            val currentData = getAmountsData()
            val filteredData = currentData.filter { it.lastUsed > cutoffTime }
            
            if (filteredData.size != currentData.size) {
                saveAmountsData(filteredData)
                AppLogger.d(TAG, "Limpieza completada: ${currentData.size - filteredData.size} registros antiguos eliminados")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en limpieza de datos antiguos", e)
        }
    }
    
    /**
     * Resetea todos los datos de uso
     */
    fun resetAllData() {
        prefs.edit().clear().apply()
        AppLogger.d(TAG, "Todos los datos de uso han sido reseteados")
    }
    
    private fun getAmountsData(): List<AmountUsage> {
        return try {
            val jsonString = prefs.getString(KEY_AMOUNTS_DATA, "[]") ?: "[]"
            val jsonArray = JSONArray(jsonString)
            
            (0 until jsonArray.length()).map { i ->
                val jsonObject = jsonArray.getJSONObject(i)
                AmountUsage(
                    amount = jsonObject.getString("amount"),
                    count = jsonObject.getInt("count"),
                    lastUsed = jsonObject.getLong("lastUsed")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo datos de montos", e)
            emptyList()
        }
    }
    
    private fun saveAmountsData(data: List<AmountUsage>) {
        try {
            val jsonArray = JSONArray()
            data.forEach { usage ->
                val jsonObject = JSONObject().apply {
                    put("amount", usage.amount)
                    put("count", usage.count)
                    put("lastUsed", usage.lastUsed)
                }
                jsonArray.put(jsonObject)
            }
            
            prefs.edit()
                .putString(KEY_AMOUNTS_DATA, jsonArray.toString())
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando datos de montos", e)
        }
    }
    
    private fun formatAmount(amount: String): String {
        return try {
            val number = amount.toLong()
            java.text.DecimalFormat("#,###").format(number)
        } catch (e: Exception) {
            amount
        }
    }
    
    private fun getDefaultAmounts(): List<String> {
        return listOf("10,000", "20,000", "50,000", "100,000", "200,000")
    }
}