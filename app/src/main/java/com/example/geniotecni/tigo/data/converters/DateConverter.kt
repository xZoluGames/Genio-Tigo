package com.example.geniotecni.tigo.data.converters

import java.util.Date

// Simplified date converter without Room annotations
class DateConverter {
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}