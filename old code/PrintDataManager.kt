package com.example.geniotecni.tigo

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class PrintData(
    val service: String,
    val date: String,
    val time: String,
    val message: String,
    val referenceData: MainActivity.ReferenceData
)

class PrintDataManager(private val context: Context) {
    private val fileName = "print_history.json"
    private val gson = Gson()

    fun savePrintData(printData: PrintData) {
        val existingData = getAllPrintData().toMutableList()
        existingData.add(0, printData) // Add new data at the beginning of the list
        val json = gson.toJson(existingData)
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    fun getAllPrintData(): List<PrintData> {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            val json = file.readText()
            gson.fromJson(json, object : TypeToken<List<PrintData>>() {}.type)
        } else {
            emptyList()
        }
    }
}