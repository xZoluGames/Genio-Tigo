// Test rápido para debuggear PrintMessageGenerator
fun main() {
    val fields = mapOf("issan" to "ZV123456")
    val references = PrintMessageGenerator.ReferenceData("150000", "REF123")
    
    val message = PrintMessageGenerator.generateMessage(
        serviceId = 8,
        serviceName = "ESSAP",
        fields = fields,
        amount = null,
        references = references
    )
    
    println("=== MENSAJE GENERADO ===")
    println(message)
    println("========================")
    
    // Verificaciones
    println("Contiene ESSAP: ${message.contains("ESSAP")}")
    println("Contiene ISSAN: ZV123456: ${message.contains("ISSAN: ZV123456")}")
    println("Contiene 150000: ${message.contains("150000")}")
    println("Contiene formato: ${message.contains("=====================")}")
}