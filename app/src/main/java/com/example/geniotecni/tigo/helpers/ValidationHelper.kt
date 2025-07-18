package com.example.geniotecni.tigo.helpers

import android.util.Patterns
import java.util.regex.Pattern

class ValidationHelper {

    companion object {
        private val PHONE_PATTERN = Pattern.compile("^09\\d{8}$")
        private val CEDULA_PATTERN = Pattern.compile("^\\d{5,15}$")
        private val AMOUNT_MIN = 1000L
        private val AMOUNT_MAX = 10000000L
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validatePhone(phone: String): ValidationResult {
        val cleanPhone = phone.replace("-", "").replace(" ", "")

        return when {
            cleanPhone.isEmpty() -> ValidationResult(false, "El número de teléfono es requerido")
            cleanPhone.length != 10 -> ValidationResult(false, "El número debe tener 10 dígitos")
            !cleanPhone.startsWith("09") -> ValidationResult(false, "El número debe comenzar con 09")
            !PHONE_PATTERN.matcher(cleanPhone).matches() -> ValidationResult(false, "Formato de número inválido")
            else -> ValidationResult(true)
        }
    }

    fun validateCedula(cedula: String): ValidationResult {
        val cleanCedula = cedula.replace(".", "").replace("-", "").trim()

        return when {
            cleanCedula.isEmpty() -> ValidationResult(false, "La cédula es requerida")
            cleanCedula.length < 5 -> ValidationResult(false, "La cédula debe tener al menos 5 dígitos")
            cleanCedula.length > 15 -> ValidationResult(false, "La cédula no puede tener más de 15 dígitos")
            !CEDULA_PATTERN.matcher(cleanCedula).matches() -> ValidationResult(false, "La cédula solo debe contener números")
            else -> ValidationResult(true)
        }
    }

    fun validateAmount(amountStr: String): ValidationResult {
        val cleanAmount = amountStr.replace(",", "").replace(".", "").trim()

        return when {
            cleanAmount.isEmpty() -> ValidationResult(false, "El monto es requerido")
            !cleanAmount.all { it.isDigit() } -> ValidationResult(false, "El monto solo debe contener números")
            else -> {
                val amount = cleanAmount.toLongOrNull() ?: 0L
                when {
                    amount < AMOUNT_MIN -> ValidationResult(false, "El monto mínimo es ${formatAmount(AMOUNT_MIN)}")
                    amount > AMOUNT_MAX -> ValidationResult(false, "El monto máximo es ${formatAmount(AMOUNT_MAX)}")
                    else -> ValidationResult(true)
                }
            }
        }
    }

    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult(false, "El email es requerido")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult(false, "Email inválido")
            else -> ValidationResult(true)
        }
    }

    fun validateDate(date: String): ValidationResult {
        val datePattern = Pattern.compile("^\\d{2}-\\d{2}-\\d{4}$")

        return when {
            date.isEmpty() -> ValidationResult(false, "La fecha es requerida")
            !datePattern.matcher(date).matches() -> ValidationResult(false, "Formato de fecha inválido (DD-MM-YYYY)")
            else -> ValidationResult(true)
        }
    }

    fun formatPhone(phone: String): String {
        val clean = phone.replace(Regex("[^0-9]"), "")
        return when {
            clean.length == 10 -> "${clean.substring(0, 4)}-${clean.substring(4, 7)}-${clean.substring(7)}"
            else -> clean
        }
    }

    fun formatCedula(cedula: String): String {
        val clean = cedula.replace(Regex("[^0-9]"), "")
        return when {
            clean.length > 6 -> "${clean.substring(0, clean.length - 3)}.${clean.substring(clean.length - 3)}"
            else -> clean
        }
    }

    fun formatAmount(amount: Long): String {
        return String.format("%,d Gs.", amount)
    }

    fun parseAmount(amountStr: String): Long {
        return amountStr.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
    }
}