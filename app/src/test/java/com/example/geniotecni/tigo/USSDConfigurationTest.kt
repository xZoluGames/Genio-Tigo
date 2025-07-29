package com.example.geniotecni.tigo

import com.example.geniotecni.tigo.utils.USSDConfiguration
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests unitarios para validar códigos USSD
 * Fase 9: Validación de integración con configuración centralizada
 */
class USSDConfigurationTest {

    @Test
    fun testGirosTigoUSSDGeneration() {
        val params = mapOf(
            "numero" to "0981234567",
            "cedula" to "1234567",
            "monto" to "50000"
        )
        val expected = "*555*1*0981234567*1234567*1*50000#"
        val actual = USSDConfiguration.generateUSSDCode(0, params)
        
        assertEquals("Código USSD para Giros Tigo no coincide", expected, actual)
    }

    @Test
    fun testRetirosTigoUSSDGeneration() {
        val params = mapOf(
            "numero" to "0985678901",
            "cedula" to "7654321",
            "monto" to "100000"
        )
        val expected = "*555*2*0985678901*7654321*1*100000#"
        val actual = USSDConfiguration.generateUSSDCode(1, params)
        
        assertEquals("Código USSD para Retiros Tigo no coincide", expected, actual)
    }

    @Test
    fun testESSAPUSSDGeneration() {
        val params = mapOf(
            "issan" to "ZV123456"
        )
        val expected = "*555*5*1*2*2*ZV123456*ZV123456*#"
        val actual = USSDConfiguration.generateUSSDCode(8, params)
        
        assertEquals("Código USSD para ESSAP no coincide", expected, actual)
    }

    @Test
    fun testANDEUSSDGeneration() {
        val params = mapOf(
            "nis" to "123456789"
        )
        val expected = "*555*5*1*2*1*123456789*123456789*#"
        val actual = USSDConfiguration.generateUSSDCode(7, params)
        
        assertEquals("Código USSD para ANDE no coincide", expected, actual)
    }

    @Test
    fun testPersonalCargaBilleteraUSSDGeneration() {
        val params = mapOf(
            "numero" to "0971234567",
            "monto" to "25000"
        )
        val expected = "*200*3*0971234567*25000#"
        val actual = USSDConfiguration.generateUSSDCode(10, params)
        
        assertEquals("Código USSD para Carga Billetera Personal no coincide", expected, actual)
    }

    @Test
    fun testPersonalRetiros() {
        val params = mapOf(
            "numero" to "0976543210",
            "monto" to "75000"
        )
        val expected = "*200*2*0976543210*75000#"
        val actual = USSDConfiguration.generateUSSDCode(11, params)
        
        assertEquals("Código USSD para Retiros Personal no coincide", expected, actual)
    }

    @Test
    fun testResetPinUSSDGeneration() {
        val params = mapOf(
            "numero" to "0981111111",
            "cedula" to "1111111",
            "nacimiento" to "01011990"
        )
        val expected = "*555*6*3*0981111111*1*1111111*01011990*#"
        val actual = USSDConfiguration.generateUSSDCode(75, params)
        
        assertEquals("Código USSD para Reseteo PIN no coincide", expected, actual)
    }

    @Test
    fun testSIMSelectionForTigo() {
        val requiredSIM = USSDConfiguration.getRequiredSIM(0)
        assertEquals("Giros Tigo debe usar SIM 1", USSDConfiguration.SimCard.SIM1, requiredSIM)
    }

    @Test
    fun testSIMSelectionForPersonal() {
        val requiredSIM = USSDConfiguration.getRequiredSIM(10)
        assertEquals("Personal debe usar SIM 2", USSDConfiguration.SimCard.SIM2, requiredSIM)
    }

    @Test
    fun testCooperativeTarjetaUSSD() {
        // Cooperativa Universitaria Tarjeta Mastercard (ID 34)
        val params = mapOf(
            "tarjeta" to "12345678"
        )
        val expected = "*555*5*1*5*1*2*12345678*12345678*#"
        val actual = USSDConfiguration.generateUSSDCode(34, params)
        
        assertEquals("Código USSD para Cooperativa Tarjeta Mastercard no coincide", expected, actual)
    }

    @Test
    fun testFinancieraUSSD() {
        // Alex S.A (ID 13)
        val params = mapOf(
            "ci" to "1234567"
        )
        val expected = "*555*5*1*3*1*1234567*1234567*#"
        val actual = USSDConfiguration.generateUSSDCode(13, params)
        
        assertEquals("Código USSD para Alex S.A no coincide", expected, actual)
    }

    @Test
    fun testInvalidServiceId() {
        val params = mapOf("test" to "value")
        val result = USSDConfiguration.generateUSSDCode(999, params)
        
        assertNull("Servicio inválido debe retornar null", result)
    }

    @Test
    fun testEmptyParams() {
        val result = USSDConfiguration.generateUSSDCode(0, emptyMap())
        
        assertNotNull("Debe generar código aunque los params estén vacíos", result)
        assertTrue("Debe contener placeholders sin reemplazar", result!!.contains("{"))
    }

    @Test
    fun testAllCriticalServicesHaveUSSDCodes() {
        val criticalServices = listOf(
            0,  // Giros Tigo
            1,  // Retiros Tigo
            7,  // ANDE
            8,  // ESSAP
            10, // Carga Billetera Personal
            11, // Retiros Personal
            75  // Reseteo PIN
        )
        
        criticalServices.forEach { serviceId ->
            val template = USSDConfiguration.getUSSDTemplate(serviceId)
            assertNotNull("Servicio crítico $serviceId debe tener código USSD", template)
            assertTrue("Template no debe estar vacío", template!!.template.isNotEmpty())
        }
    }

    @Test
    fun testUSSDTemplateStructure() {
        val template = USSDConfiguration.getUSSDTemplate(0)
        assertNotNull("Template debe existir", template)
        
        assertTrue("Template debe empezar con *", template!!.template.startsWith("*"))
        assertTrue("Template debe terminar con #", template.template.endsWith("#"))
        assertTrue("Template debe contener placeholders", template.template.contains("{"))
    }
}