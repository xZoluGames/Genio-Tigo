package com.example.geniotecni.tigo

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.geniotecni.tigo.managers.PrintDataManager
import java.io.FileOutputStream
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*

// 1. Clase para exportar historial a CSV
class HistoryExporter(private val context: Context) {

    fun exportToCSV(): Boolean {
        return try {
            val printDataManager = PrintDataManager(context)
            val history = printDataManager.getAllPrintData()

            val csvContent = StringBuilder()
            csvContent.append("Servicio,Fecha,Hora,Referencia 1,Referencia 2,Mensaje Completo\n")

            history.forEach { data ->
                csvContent.append("\"${data.service}\",")
                csvContent.append("\"${data.date}\",")
                csvContent.append("\"${data.time}\",")
                csvContent.append("\"${data.referenceData.ref1}\",")
                csvContent.append("\"${data.referenceData.ref2}\",")
                csvContent.append("\"${data.message.replace("\"", "\"\"")}\"\n")
            }

            val fileName = "historial_geniotecni_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)

            file.writeText(csvContent.toString())

            // Notificar al usuario
            showExportNotification(file.absolutePath, "CSV")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportToPDF(): Boolean {
        return try {
            val printDataManager = PrintDataManager(context)
            val history = printDataManager.getAllPrintData()

            val fileName = "historial_geniotecni_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)

            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            // Título
            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
            val title = Paragraph("Historial de Transacciones - Genio Tecni", titleFont)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)
            document.add(Paragraph("\n"))

            // Fecha de exportación
            val dateFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC)
            val exportDate = Paragraph("Exportado el: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}", dateFont)
            exportDate.alignment = Element.ALIGN_RIGHT
            document.add(exportDate)
            document.add(Paragraph("\n"))

            // Tabla
            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(2f, 1.5f, 1.5f, 1f, 1f))

            // Headers
            val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
            val headerColor = BaseColor(0, 121, 107) // Teal

            arrayOf("Servicio", "Fecha", "Hora", "Ref 1", "Ref 2").forEach { header ->
                val cell = PdfPCell(Phrase(header, headerFont))
                cell.backgroundColor = headerColor
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.setPadding(8f)
                table.addCell(cell)
            }

            // Data
            val dataFont = Font(Font.FontFamily.HELVETICA, 10f)
            history.forEach { data ->
                table.addCell(PdfPCell(Phrase(data.service, dataFont)))
                table.addCell(PdfPCell(Phrase(data.date, dataFont)))
                table.addCell(PdfPCell(Phrase(data.time, dataFont)))
                table.addCell(PdfPCell(Phrase(data.referenceData.ref1, dataFont)))
                table.addCell(PdfPCell(Phrase(data.referenceData.ref2, dataFont)))
            }

            document.add(table)
            document.close()

            showExportNotification(file.absolutePath, "PDF")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun showExportNotification(filePath: String, format: String) {
        val channelId = "export_channel"
        val notificationId = 1001

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Exportación de Historial",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Historial Exportado")
            .setContentText("Se exportó el historial en formato $format")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Archivo guardado en:\n$filePath"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Verificar permisos de notificación para Android 13+
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}

