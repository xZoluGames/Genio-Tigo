package com.example.geniotecni.tigo.events

data class DataSaved(
    val dataType: String,
    val recordId: String,
    val size: Long
) : DataEvent