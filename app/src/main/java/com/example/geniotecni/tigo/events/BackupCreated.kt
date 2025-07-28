package com.example.geniotecni.tigo.events

data class BackupCreated(
    val backupPath: String,
    val backupSize: Long,
    val recordCount: Int
) : DataEvent
