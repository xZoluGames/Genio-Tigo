package com.example.geniotecni.tigo

import com.example.geniotecni.tigo.utils.PrintMessageGenerator
import com.example.geniotecni.tigo.utils.PrintConfiguration
import com.example.geniotecni.tigo.models.ReferenceData
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitarios para PrintMessageGenerator
 * Fase 10: Validación de formatos de impresión consistentes
 */
class PrintMessageGeneratorTest {

    @Test
    fun testESSAPPrintFormat() {
        val fields = mapOf("issan" to "ZV123456")
        val references = ReferenceData("150000", "REF123")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 8,
            serviceName = "ESSAP",
            fields = fields,
            amount = null,
            references = references
        )
        
        assertTrue("Debe contener servicio ESSAP", message.contains("ESSAP"))
        assertTrue("Debe contener ISSAN correcto", message.contains("ISSAN: ZV123456"))
        assertTrue("Debe contener monto de ref1", message.contains("150000"))
        assertTrue("Debe contener formato estándar", message.contains("====================="))
        assertTrue("Debe contener Genio Tecni", message.contains("Genio Tecni"))
    }

    @Test
    fun testANDEPrintFormat() {
        val fields = mapOf("nis" to "987654321")
        val references = ReferenceData("125000", "ANDE789")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 7,
            serviceName = "ANDE",
            fields = fields,
            amount = null,
            references = references
        )
        
        assertTrue("Debe contener servicio ANDE", message.contains("ANDE"))
        assertTrue("Debe contener NIS correcto", message.contains("NIS: 987654321"))
        assertTrue("Debe contener monto de ref1", message.contains("125000"))
        assertTrue("Debe seguir formato estándar", message.contains("====================="))
    }

    @Test
    fun testGirosTigoWithCommission() {
        val fields = mapOf(
            "numero" to "0981234567",
            "cedula" to "1234567"
        )
        val references = ReferenceData("TX123", "TX456")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 0,
            serviceName = "Giros Tigo",
            fields = fields,
            amount = "100000",
            references = references
        )
        
        assertTrue("Debe contener Giros Tigo", message.contains("Giros Tigo"))
        assertTrue("Debe contener número", message.contains("0981234567"))
        assertTrue("Debe contener cédula", message.contains("1234567"))
        assertTrue("Debe contener monto", message.contains("100000"))
        assertTrue("Debe calcular comisión 6%", message.contains("6000") || message.contains("6,000"))
        assertTrue("Debe contener ref1", message.contains("TX123"))
        assertTrue("Debe contener ref2", message.contains("TX456"))
    }

    @Test
    fun testPersonalPrintFormat() {
        val fields = mapOf("numero" to "0971234567")
        val references = ReferenceData("PER789", "")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 10,
            serviceName = "Carga Billetera Personal",
            fields = fields,
            amount = "50000",
            references = references
        )
        
        assertTrue("Debe contener Personal", message.contains("Personal"))
        assertTrue("Debe contener número", message.contains("0971234567"))
        assertTrue("Debe contener monto", message.contains("50000"))
        assertTrue("Debe contener ref1", message.contains("PER789"))
        assertFalse("No debe tener comisión", message.contains("Comision"))
    }

    @Test
    fun testGenericServiceFormat() {
        val fields = mapOf("ci" to "7654321")
        val references = ReferenceData("85000", "GEN456")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 13, // Alex S.A
            serviceName = "Alex S.A",
            fields = fields,
            amount = null,
            references = references
        )
        
        assertTrue("Debe contener servicio", message.contains("Alex S.A"))
        assertTrue("Debe contener CI", message.contains("7654321"))
        assertTrue("Debe usar monto de ref1", message.contains("85000"))
        assertTrue("Debe contener referencia", message.contains("GEN456"))
    }

    @Test
    fun testCardServiceFormat() {
        val fields = mapOf("tarjeta" to "12345678")
        val references = ReferenceData("95000", "CARD123")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 34, // Cooperativa Universitaria Mastercard
            serviceName = "Cooperativa Universitaria (Tarjeta Mastercard)",
            fields = fields,
            amount = null,
            references = references
        )
        
        assertTrue("Debe contener servicio", message.contains("Cooperativa"))
        assertTrue("Debe contener tarjeta", message.contains("12345678"))
        assertTrue("Debe usar monto de ref1", message.contains("95000"))
    }

    @Test
    fun testResetPinFormat() {
        val fields = mapOf(
            "numero" to "0981111111",
            "cedula" to "1111111",
            "nacimiento" to "01011990"
        )
        val references = ReferenceData("RST123", "RST456")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 75,
            serviceName = "Reseteo de Pin (Cliente)",
            fields = fields,
            amount = null,
            references = references
        )
        
        assertTrue("Debe contener reseteo", message.contains("Reseteo"))
        assertTrue("Debe contener número", message.contains("0981111111"))
        assertTrue("Debe contener cédula", message.contains("1111111"))
        assertTrue("Debe contener fecha nacimiento", message.contains("01011990"))
    }

    @Test
    fun testDateTimeFormatting() {
        val fields = mapOf("numero" to "0981234567")
        val references = ReferenceData("TEST123", "")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 10,
            serviceName = "Test Service",
            fields = fields,
            amount = "10000",
            references = references
        )
        
        // Verificar que contiene formato de fecha y hora
        assertTrue("Debe contener Fecha:", message.contains("Fecha:"))
        assertTrue("Debe contener Hora:", message.contains("Hora:"))
        
        // Verificar patrón de fecha dd-MM-yyyy (básico)
        val datePattern = Regex("\\d{2}-\\d{2}-\\d{4}")
        assertTrue("Debe tener formato de fecha correcto", datePattern.containsMatchIn(message))
        
        // Verificar patrón de hora HH:mm:ss (básico)
        val timePattern = Regex("\\d{2}:\\d{2}:\\d{2}")
        assertTrue("Debe tener formato de hora correcto", timePattern.containsMatchIn(message))
    }

    @Test
    fun testPlaceholdersCleaning() {
        val fields = mapOf("numero" to "0981234567")
        val references = ReferenceData("", "")
        
        val message = PrintMessageGenerator.generateMessage(
            serviceId = 10,
            serviceName = "Test Service",
            fields = fields,
            amount = null,
            references = references
        )
        
        // Verificar que no queden placeholders sin reemplazar
        assertFalse("No debe tener placeholders sin reemplazar", message.contains("{"))
        assertFalse("No debe tener placeholders sin reemplazar", message.contains("}"))
    }

    @Test
    fun testSimpleMessageGeneration() {
        val message = PrintMessageGenerator.generateSimpleMessage(
            serviceName = "Test Simple",
            fields = mapOf("numero" to "0981234567"),
            amount = "25000",
            ref1 = "SIMPLE123",
            ref2 = "SIMPLE456"
        )
        
        assertTrue("Debe contener servicio", message.contains("Test Simple"))
        assertTrue("Debe contener número", message.contains("0981234567"))
        assertTrue("Debe contener monto", message.contains("25000"))
        assertTrue("Debe contener referencia", message.contains("SIMPLE123"))
    }
}