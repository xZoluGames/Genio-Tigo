package com.example.geniotecni.tigo.data.dao

data class ServiceUsageRaw(
    val service_id: Int,
    val service_name: String,
    val count: Int,
    val totalAmount: Long
)