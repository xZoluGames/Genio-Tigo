package com.example.geniotecni.tigo

import com.example.geniotecni.tigo.models.PrintData
import com.example.geniotecni.tigo.models.ReferenceData
import com.example.geniotecni.tigo.utils.USSDConfiguration
import com.example.geniotecni.tigo.utils.PrintMessageGenerator
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests de integración para flujo completo de servicios
 * Fase 10: Validación del flujo USSD → SMS → PrintData
 */
class ServiceIntegrationTest {

    @Test
    fun testCompleteESSAPFlow() {
        // 1. Generación USSD
        val params = mapOf("issan" to "ZV987654")
        val ussdCode = USSDConfiguration.generateUSSDCode(8, params)
        assertEquals("USSD ESSAP correcto", "*555*5*1*2*2*ZV987654*ZV987654*#", ussdCode)
        
        // 2. Simulación de respuesta SMS (referencias)
        val references = ReferenceData("125000", "ESSAP789")
        
        // 3. Generación de PrintData
        val printData = PrintData.fromTransaction(
            serviceId = 8,
            serviceName = "ESSAP",
            fields = params,
            amount = null,
            referenceData = references
        )
        
        // 4. Validaciones del mensaje generado
        val message = printData.rawMessage
        assertTrue("Debe contener ESSAP", message.contains("ESSAP"))
        assertTrue("Debe contener ISSAN", message.contains("ISSAN: ZV987654"))
        assertTrue("Debe contener monto correcto", message.contains("125000"))
        assertTrue("Debe contener referencia", message.contains("ESSAP789"))
        
        // 5. Validar consistencia getDisplayMessage
        assertEquals("DisplayMessage debe ser igual a rawMessage", 
                    printData.rawMessage, printData.getDisplayMessage())
    }

    @Test
    fun testCompleteANDEFlow() {
        // 1. Generación USSD
        val params = mapOf("nis" to "123456789")
        val ussdCode = USSDConfiguration.generateUSSDCode(7, params)
        assertEquals("USSD ANDE correcto", "*555*5*1*2*1*123456789*123456789*#", ussdCode)
        
        // 2. Referencias simuladas
        val references = ReferenceData("89000", "ANDE456")
        
        // 3. PrintData
        val printData = PrintData.fromTransaction(
            serviceId = 7,
            serviceName = "ANDE",
            fields = params,
            amount = null,
            referenceData = references
        )
        
        // 4. Validaciones
        val message = printData.rawMessage
        assertTrue("Debe contener ANDE", message.contains("ANDE"))
        assertTrue("Debe contener NIS", message.contains("NIS: 123456789"))
        assertTrue("Debe contener monto de ref1", message.contains("89000"))
    }

    @Test
    fun testCompleteGirosTigoFlowWithCommission() {
        // 1. Generación USSD
        val params = mapOf(
            "numero" to "0981234567",
            "cedula" to "1234567",
            "monto" to "100000"
        )
        val ussdCode = USSDConfiguration.generateUSSDCode(0, params)
        assertEquals("USSD Giros correcto", "*555*1*0981234567*1234567*1*100000#", ussdCode)
        
        // 2. Referencias simuladas
        val references = ReferenceData("TX123", "TX456")
        
        // 3. PrintData con configuración de comisión
        val printData = PrintData.fromTransaction(
            serviceId = 0,
            serviceName = "Giros Tigo",
            fields = params,
            amount = "100000",
            referenceData = references
        )
        
        // 4. Validaciones
        val message = printData.rawMessage
        assertTrue("Debe contener Giros Tigo", message.contains("Giros Tigo"))
        assertTrue("Debe contener número", message.contains("0981234567"))
        assertTrue("Debe contener monto", message.contains("100000"))
        assertTrue("Debe calcular comisión 6%", 
                  message.contains("6000") || message.contains("6,000"))
        assertTrue("Debe contener ambas referencias", 
                  message.contains("TX123") && message.contains("TX456"))
    }

    @Test
    fun testCompletePersonalFlow() {
        // 1. Verificar SIM 2 para Personal
        val requiredSIM = USSDConfiguration.getRequiredSIM(10)
        assertEquals("Personal debe usar SIM 2", USSDConfiguration.SimCard.SIM2, requiredSIM)
        
        // 2. Generación USSD
        val params = mapOf(
            "numero" to "0971234567",
            "monto" to "50000"
        )
        val ussdCode = USSDConfiguration.generateUSSDCode(10, params)
        assertEquals("USSD Personal correcto", "*200*3*0971234567*50000#", ussdCode)
        
        // 3. Referencias
        val references = ReferenceData("PER789", "")
        
        // 4. PrintData
        val printData = PrintData.fromTransaction(
            serviceId = 10,
            serviceName = "Carga Billetera Personal",
            fields = params,
            amount = "50000",
            referenceData = references
        )
        
        // 5. Validaciones
        val message = printData.rawMessage
        assertTrue("Debe contener Personal", message.contains("Personal"))
        assertTrue("Debe contener número", message.contains("0971234567"))
        assertTrue("Debe contener monto", message.contains("50000"))
        assertFalse("No debe tener comisión", message.contains("Comision"))
    }

    @Test
    fun testServiceWithSpecialFieldMapping() {
        // Test para verificar mapeo de campos especiales
        val essapParams = mapOf("issan" to "ZV555666")
        val andeParams = mapOf("nis" to "777888999")
        
        // ESSAP: cedula → issan
        val essapUSSD = USSDConfiguration.generateUSSDCode(8, essapParams)
        assertTrue("ESSAP debe usar campo issan", essapUSSD!!.contains("ZV555666"))
        
        // ANDE: cedula → nis  
        val andeUSSD = USSDConfiguration.generateUSSDCode(7, andeParams)
        assertTrue("ANDE debe usar campo nis", andeUSSD!!.contains("777888999"))
    }

    @Test
    fun testCooperativeCardService() {
        // Test para servicio con tarjeta
        val params = mapOf("tarjeta" to "87654321")
        val ussdCode = USSDConfiguration.generateUSSDCode(34, params) // Mastercard
        
        assertEquals("USSD cooperativa tarjeta correcto", 
                    "*555*5*1*5*1*2*87654321*87654321*#", ussdCode)
        
        // PrintData
        val references = ReferenceData("65000", "COOP123")
        val printData = PrintData.fromTransaction(
            serviceId = 34,
            serviceName = "Cooperativa Universitaria (Tarjeta Mastercard)",
            fields = params,
            amount = null,
            referenceData = references
        )
        
        val message = printData.rawMessage
        assertTrue("Debe contener tarjeta", message.contains("87654321"))
        assertTrue("Debe usar monto de ref1", message.contains("65000"))
    }

    @Test
    fun testResetPinSpecialCase() {
        // Test para servicio con campo adicional (nacimiento)
        val params = mapOf(
            "numero" to "0981111111",
            "cedula" to "1111111", 
            "nacimiento" to "15021985"
        )
        
        val ussdCode = USSDConfiguration.generateUSSDCode(75, params)
        assertEquals("USSD Reset PIN correcto", 
                    "*555*6*3*0981111111*1*1111111*15021985*#", ussdCode)
        
        val references = ReferenceData("RST123", "RST456")
        val printData = PrintData.fromTransaction(
            serviceId = 75,
            serviceName = "Reseteo de Pin (Cliente)",
            fields = params,
            amount = null,
            referenceData = references
        )
        
        val message = printData.rawMessage
        assertTrue("Debe contener todos los campos", 
                  message.contains("0981111111") && 
                  message.contains("1111111") && 
                  message.contains("15021985"))
    }

    @Test
    fun testPrintDataConsistency() {
        // Test para verificar consistencia entre diferentes métodos de creación
        val params = mapOf("numero" to "0981234567")
        val references = ReferenceData("TEST123", "TEST456")
        
        // Método fromTransaction
        val printData1 = PrintData.fromTransaction(
            serviceId = 10,
            serviceName = "Test Service",
            fields = params,
            amount = "10000",
            referenceData = references
        )
        
        // Método createSimple
        val printData2 = PrintData.createSimple(
            serviceName = "Test Service",
            fields = params,
            amount = "10000",
            ref1 = "TEST123",
            ref2 = "TEST456"
        )
        
        // Ambos deben generar mensajes similares (estructura)
        assertTrue("Ambos deben contener el servicio", 
                  printData1.rawMessage.contains("Test Service") && 
                  printData2.rawMessage.contains("Test Service"))
        
        assertTrue("Ambos deben contener el número",
                  printData1.rawMessage.contains("0981234567") && 
                  printData2.rawMessage.contains("0981234567"))
    }

    @Test
    fun testAllCriticalServicesIntegration() {
        val criticalServices = listOf(
            Triple(0, "Giros Tigo", mapOf("numero" to "0981111111", "cedula" to "1111111", "monto" to "50000")),
            Triple(1, "Retiros Tigo", mapOf("numero" to "0982222222", "cedula" to "2222222", "monto" to "75000")),
            Triple(7, "ANDE", mapOf("nis" to "333333333")),
            Triple(8, "ESSAP", mapOf("issan" to "ZV444444")),
            Triple(10, "Carga Billetera Personal", mapOf("numero" to "0975555555", "monto" to "25000")),
            Triple(75, "Reseteo de Pin (Cliente)", mapOf("numero" to "0986666666", "cedula" to "6666666", "nacimiento" to "01011990"))
        )
        
        criticalServices.forEach { (serviceId, serviceName, params) ->
            // 1. Debe generar USSD
            val ussdCode = USSDConfiguration.generateUSSDCode(serviceId, params)
            assertNotNull("Servicio $serviceName debe generar USSD", ussdCode)
            assertTrue("USSD debe empezar con *", ussdCode!!.startsWith("*"))
            assertTrue("USSD debe terminar con #", ussdCode.endsWith("#"))
            
            // 2. Debe generar PrintData
            val references = ReferenceData("REF$serviceId", "REF${serviceId}B")
            val printData = PrintData.fromTransaction(
                serviceId = serviceId,
                serviceName = serviceName,
                fields = params,
                amount = params["monto"],
                referenceData = references
            )
            
            // 3. Mensaje debe estar bien formateado
            val message = printData.rawMessage
            assertTrue("$serviceName debe contener formato estándar", 
                      message.contains("====================="))
            assertTrue("$serviceName debe contener Genio Tecni", 
                      message.contains("Genio Tecni"))
            assertTrue("$serviceName debe contener el nombre del servicio", 
                      message.contains(serviceName.split(" ")[0])) // Primera palabra
        }
    }
}