package com.example.geniotecni.tigo.data.entities

import com.example.geniotecni.tigo.utils.formatAsCurrency

data class ServiceUsage(
    val serviceId: Int,
    val serviceName: String,
    val count: Int,
    val totalAmount: Long, // in cents
    val percentage: Float
) {
    fun getFormattedAmount(): String = (totalAmount / 100.0).formatAsCurrency()
}