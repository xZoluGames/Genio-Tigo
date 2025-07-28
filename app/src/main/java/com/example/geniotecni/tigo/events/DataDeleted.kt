package com.example.geniotecni.tigo.events

data class DataDeleted(
    val dataType: String,
    val recordId: String
) : DataEvent
