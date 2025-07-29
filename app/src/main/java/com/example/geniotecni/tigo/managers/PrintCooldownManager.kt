package com.example.geniotecni.tigo.managers

import android.content.Context
import com.example.geniotecni.tigo.utils.BaseManager
import com.example.geniotecni.tigo.utils.AppConfig
import com.example.geniotecni.tigo.utils.showToast
import java.util.*

class PrintCooldownManager(context: Context) : BaseManager(context, "PrintCooldownManager") {
    
    companion object {
        private const val PRINT_COUNT_KEY = "print_count"
        private const val LAST_PRINT_TIME_KEY = "last_print_time"
        private const val COOLDOWN_MINUTES = 5L
        private const val MAX_PRINTS_PER_PERIOD = 10
    }
    
    data class PrintStatus(
        val canPrint: Boolean,
        val remainingCooldown: Long = 0L, // in milliseconds
        val printCount: Int = 0,
        val message: String = ""
    )
    
    fun canPrint(): PrintStatus {
        val currentTime = System.currentTimeMillis()
        val lastPrintTime = getLong(LAST_PRINT_TIME_KEY, 0L)
        val printCount = getInt(PRINT_COUNT_KEY, 0)
        
        // If it's been more than cooldown period, reset counter
        val cooldownMillis = COOLDOWN_MINUTES * 60L * 1000L
        if (currentTime - lastPrintTime > cooldownMillis) {
            resetPrintCount()
            return PrintStatus(
                canPrint = true,
                printCount = 0,
                message = "Puedes imprimir"
            )
        }
        
        // Check if user has exceeded maximum prints
        if (printCount >= MAX_PRINTS_PER_PERIOD) {
            val remainingCooldown = cooldownMillis - (currentTime - lastPrintTime)
            val minutesRemaining = (remainingCooldown / (60L * 1000L)).toInt()
            val secondsRemaining = ((remainingCooldown % (60L * 1000L)) / 1000L).toInt()
            
            return PrintStatus(
                canPrint = false,
                remainingCooldown = remainingCooldown,
                printCount = printCount,
                message = "Límite de impresiones alcanzado. Espera ${minutesRemaining}m ${secondsRemaining}s"
            )
        }
        
        return PrintStatus(
            canPrint = true,
            printCount = printCount,
            message = "Puedes imprimir (${printCount}/${MAX_PRINTS_PER_PERIOD})"
        )
    }
    
    fun recordPrint(): Boolean {
        val status = canPrint()
        
        if (!status.canPrint) {
            context.showToast(status.message)
            return false
        }
        
        val currentTime = System.currentTimeMillis()
        val currentCount = getInt(PRINT_COUNT_KEY, 0)
        
        // Update counters
        savePreference(PRINT_COUNT_KEY, currentCount + 1)
        savePreference(LAST_PRINT_TIME_KEY, currentTime)
        
        // Show remaining prints
        val newCount = currentCount + 1
        if (newCount < MAX_PRINTS_PER_PERIOD) {
            context.showToast("Impresión ${newCount}/${MAX_PRINTS_PER_PERIOD}")
        } else {
            context.showToast("Límite de impresiones alcanzado por ${COOLDOWN_MINUTES} minutos")
        }
        
        return true
    }
    
    // Info methods removed - not used externally
    
    fun resetPrintCount() {
        savePreference(PRINT_COUNT_KEY, 0)
        savePreference(LAST_PRINT_TIME_KEY, 0L)
    }
    
    fun getTimeUntilReset(): Long {
        val currentTime = System.currentTimeMillis()
        val lastPrintTime = getLong(LAST_PRINT_TIME_KEY, 0L)
        val timeSinceLastPrint = currentTime - lastPrintTime
        val cooldownTime = COOLDOWN_MINUTES * 60L * 1000L
        
        return if (timeSinceLastPrint >= cooldownTime) {
            0L
        } else {
            cooldownTime - timeSinceLastPrint
        }
    }
    
    // Cooldown info removed - not displayed in UI
}