package com.example.geniotecni.tigo

object PhoneValidator {

    private val VALID_PREFIXES = listOf(
        "0981", "0982", "0983", "0984", "0985", "0986",
        "0961", "0962", "0963", "0971", "0972", "0973",
        "0974", "0975", "0976", "0991", "0992", "0993",
        "0994", "0995"
    )

    fun isValidParaguayanPhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")

        return when {
            cleanPhone.length != 10 -> false
            !cleanPhone.startsWith("09") -> false
            else -> VALID_PREFIXES.any { cleanPhone.startsWith(it) }
        }
    }

    fun getOperator(phone: String): String? {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")

        return when {
            cleanPhone.startsWith("098") -> "Tigo"
            cleanPhone.startsWith("096") || cleanPhone.startsWith("097") -> "Personal"
            cleanPhone.startsWith("099") -> "Claro"
            else -> null
        }
    }

    fun formatPhone(phone: String): String {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")

        return if (cleanPhone.length == 10) {
            "${cleanPhone.substring(0, 4)}-${cleanPhone.substring(4, 7)}-${cleanPhone.substring(7)}"
        } else {
            phone
        }
    }
}