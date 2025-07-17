package com.example.geniotecni.tigo.managers

import android.content.Context
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.utils.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class PrintDataManager(private val context: Context) {
    private val fileName = Constants.PRINT_HISTORY_FILE
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
    
    fun clearAllData() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }
    
    fun getDataCount(): Int {
        return getAllPrintData().size
    }
}