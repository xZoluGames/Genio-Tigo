package com.example.geniotecni.tigo.events

data class BackupRestored(
    val backupPath: String,
    val recordsRestored: Int,
    val success: Boolean
) : DataEvent
