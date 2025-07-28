package com.example.geniotecni.tigo.events

data class PrintJobCompleted(
    val deviceAddress: String,
    val jobId: String,
    val success: Boolean,
    val errorMessage: String? = null
) : BluetoothEvent
