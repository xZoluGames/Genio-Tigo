package com.example.geniotecni.tigo.models

data class PrintData(
    val service: String,
    val date: String,
    val time: String,
    val message: String,
    val referenceData: ReferenceData,
    val transactionData: TransactionData = TransactionData()
)





