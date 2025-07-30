import com.example.geniotecni.tigo.utils.PrintMessageGenerator

fun main() {
    // Test 1: Giros Tigo
    println("=== TEST 1: Giros Tigo ===")
    val fields1 = mapOf(
        "numero" to "0981234567",
        "cedula" to "1234567"
    )
    val references1 = PrintMessageGenerator.ReferenceData("TX123", "TX456")
    
    val message1 = PrintMessageGenerator.generateMessage(
        serviceId = 0,
        serviceName = "Giros Tigo",
        fields = fields1,
        amount = "100000",
        references = references1
    )
    
    println("RESULTADO:")
    println(message1)
    println()
    
    // Test 2: Personal
    println("=== TEST 2: Personal ===")
    val fields2 = mapOf("numero" to "0971234567")
    val references2 = PrintMessageGenerator.ReferenceData("PER789", "")
    
    val message2 = PrintMessageGenerator.generateMessage(
        serviceId = 10,
        serviceName = "Carga Billetera Personal",
        fields = fields2,
        amount = "50000",
        references = references2
    )
    
    println("RESULTADO:")
    println(message2)
    println()
    
    // Test 3: Simple
    println("=== TEST 3: Simple ===")
    val message3 = PrintMessageGenerator.generateSimpleMessage(
        serviceName = "Test Simple",
        fields = mapOf("numero" to "0981234567"),
        amount = "25000",
        ref1 = "SIMPLE123",
        ref2 = "SIMPLE456"
    )
    
    println("RESULTADO:")
    println(message3)
}