package com.example.geniotecni.tigo.events

data class StatisticsUpdated(
    val totalTransactions: Int,
    val totalAmount: Long,
    val period: String
) : DataEvent
