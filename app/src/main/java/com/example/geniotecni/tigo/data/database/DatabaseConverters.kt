package com.example.geniotecni.tigo.data.database

import androidx.room.TypeConverter
import com.example.geniotecni.tigo.data.entities.TransactionStatus
import com.example.geniotecni.tigo.data.entities.TransactionType
import com.example.geniotecni.tigo.utils.AppLogger

// Convertidores de tipos para Room
class DatabaseConverters {

    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTransactionStatus(status: String): TransactionStatus {
        return try {
            TransactionStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            AppLogger.w("DatabaseConverters", "Estado desconocido: $status, usando PENDING")
            TransactionStatus.PENDING
        }
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(type: String): TransactionType {
        return try {
            TransactionType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            AppLogger.w("DatabaseConverters", "Tipo desconocido: $type, usando USSD")
            TransactionType.USSD
        }
    }
}