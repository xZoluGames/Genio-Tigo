package com.example.geniotecni.tigo.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.geniotecni.tigo.managers.PrintDataManager
import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.utils.showToast
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExportHelper(private val context: Context) {
    
    private val printDataManager = PrintDataManager(context)
    
    companion object {
        private const val EXPORT_FOLDER = "GenioTecni_Exports"
        private const val CSV_HEADER = "Servicio,Fecha,Hora,Referencia 1,Referencia 2,Monto,Comisión\n"
    }
    
    fun exportToCSV(shareAfterExport: Boolean = false): File? {
        try {
            val history = printDataManager.getAllPrintData()
            if (history.isEmpty()) {
                context.showToast("No hay datos para exportar")
                return null
            }
            
            val csvContent = StringBuilder()
            csvContent.append(CSV_HEADER)
            
            history.forEach { data ->
                // Extract amount and commission from message
                val amount = extractAmount(data.message)
                val commission = extractCommission(data.message)
                
                csvContent.append("\"${data.serviceName}\",")
                csvContent.append("\"${data.date}\",")
                csvContent.append("\"${data.time}\",")
                csvContent.append("\"${data.referenceData.ref1}\",")
                csvContent.append("\"${data.referenceData.ref2}\",")
                csvContent.append("\"$amount\",")
                csvContent.append("\"$commission\"\n")
            }
            
            val file = saveToFile(csvContent.toString(), "csv")
            
            if (shareAfterExport && file != null) {
                shareFile(file, "text/csv")
            }
            
            context.showToast("Archivo CSV exportado exitosamente")
            return file
            
        } catch (e: Exception) {
            e.printStackTrace()
            context.showToast("Error al exportar CSV: ${e.message}")
            return null
        }
    }
    
    fun exportToPDF(shareAfterExport: Boolean = false): File? {
        try {
            val history = printDataManager.getAllPrintData()
            if (history.isEmpty()) {
                context.showToast("No hay datos para exportar")
                return null
            }
            
            val fileName = "historial_geniotecni_${getTimestamp()}.pdf"
            val file = File(getExportDirectory(), fileName)
            
            val document = Document(PageSize.A4)
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()
            
            // Add header
            addPDFHeader(document)
            
            // Add summary
            addPDFSummary(document, history)
            
            // Add transactions table
            addPDFTransactionsTable(document, history)
            
            document.close()
            
            if (shareAfterExport) {
                shareFile(file, "application/pdf")
            }
            
            context.showToast("Archivo PDF exportado exitosamente")
            return file
            
        } catch (e: Exception) {
            e.printStackTrace()
            context.showToast("Error al exportar PDF: ${e.message}")
            return null
        }
    }
    
    private fun addPDFHeader(document: Document) {
        val titleFont = Font(Font.FontFamily.HELVETICA, 24f, Font.BOLD, BaseColor(0, 121, 107))
        val title = Paragraph("GENIO TECNI", titleFont)
        title.alignment = Element.ALIGN_CENTER
        document.add(title)
        
        val subtitleFont = Font(Font.FontFamily.HELVETICA, 16f, Font.NORMAL)
        val subtitle = Paragraph("Historial de Transacciones", subtitleFont)
        subtitle.alignment = Element.ALIGN_CENTER
        document.add(subtitle)
        
        document.add(Paragraph("\n"))
        
        val dateFont = Font(Font.FontFamily.HELVETICA, 10f, Font.ITALIC)
        val exportDate = Paragraph("Generado el: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}", dateFont)
        exportDate.alignment = Element.ALIGN_RIGHT
        document.add(exportDate)
        
        document.add(Paragraph("\n\n"))
    }
    
    private fun addPDFSummary(document: Document, history: List<PrintData>) {
        val summaryFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD)
        document.add(Paragraph("RESUMEN", summaryFont))
        document.add(Paragraph("\n"))
        
        val normalFont = Font(Font.FontFamily.HELVETICA, 12f)
        
        var totalAmount = 0L
        var totalCommission = 0L
        val serviceCount = mutableMapOf<String, Int>()
        
        history.forEach { data ->
            totalAmount += extractAmount(data.message)
            totalCommission += extractCommission(data.message)
            serviceCount[data.serviceName] = serviceCount.getOrDefault(data.serviceName, 0) + 1
        }
        
        document.add(Paragraph("Total de transacciones: ${history.size}", normalFont))
        document.add(Paragraph("Monto total: ${formatAmount(totalAmount)} Gs.", normalFont))
        document.add(Paragraph("Comisión total: ${formatAmount(totalCommission)} Gs.", normalFont))
        
        val mostUsedService = serviceCount.maxByOrNull { it.value }
        mostUsedService?.let {
            document.add(Paragraph("Servicio más usado: ${it.key} (${it.value} veces)", normalFont))
        }
        
        document.add(Paragraph("\n\n"))
    }
    
    private fun addPDFTransactionsTable(document: Document, history: List<PrintData>) {
        val tableFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD)
        document.add(Paragraph("DETALLE DE TRANSACCIONES", tableFont))
        document.add(Paragraph("\n"))
        
        val table = PdfPTable(6)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f))
        
        // Headers
        val headerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.WHITE)
        val headerColor = BaseColor(0, 121, 107)
        
        arrayOf("Servicio", "Fecha", "Hora", "Monto", "Ref 1", "Ref 2").forEach { header ->
            val cell = com.itextpdf.text.pdf.PdfPCell(Phrase(header, headerFont))
            cell.backgroundColor = headerColor
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.setPadding(8f)
            table.addCell(cell)
        }
        
        // Data
        val dataFont = Font(Font.FontFamily.HELVETICA, 9f)
        var rowColor = true
        
        history.forEach { data ->
            val amount = extractAmount(data.message)
            
            val cells = arrayOf(
                data.serviceName,
                data.date,
                data.time,
                formatAmount(amount),
                data.referenceData.ref1,
                data.referenceData.ref2
            )
            
            cells.forEach { text ->
                val cell = com.itextpdf.text.pdf.PdfPCell(Phrase(text, dataFont))
                if (rowColor) {
                    cell.backgroundColor = BaseColor(240, 240, 240)
                }
                cell.setPadding(5f)
                table.addCell(cell)
            }
            
            rowColor = !rowColor
        }
        
        document.add(table)
    }
    
    private fun extractAmount(message: String): Long {
        val regex = Regex("""Monto: (\d+) Gs\.""")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }
    
    private fun extractCommission(message: String): Long {
        val regex = Regex("""Comision: (\d+) Gs\.""")
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }
    
    private fun formatAmount(amount: Long): String {
        return String.format("%,d", amount)
    }
    
    private fun saveToFile(content: String, extension: String): File? {
        return try {
            val fileName = "historial_geniotecni_${getTimestamp()}.$extension"
            val file = File(getExportDirectory(), fileName)
            file.writeText(content)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getExportDirectory(): File {
        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), EXPORT_FOLDER)
        } else {
            @Suppress("DEPRECATION")
            File(Environment.getExternalStorageDirectory(), "Documents/$EXPORT_FOLDER")
        }
        
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        return dir
    }
    
    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    
    private fun shareFile(file: File, mimeType: String) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Historial Genio Tecni")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Compartir historial"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            context.showToast("Error al compartir archivo: ${e.message}")
        }
    }
}