package com.example.geniotecni.tigo.events

data class DataExported(
    val format: String,
    val filePath: String,
    val recordCount: Int
) : DataEvent
