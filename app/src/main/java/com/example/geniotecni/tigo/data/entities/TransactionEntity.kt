package com.example.geniotecni.tigo.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import com.example.geniotecni.tigo.utils.formatAsDateTime
import com.example.geniotecni.tigo.utils.formatAsCurrency

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["service_id"]),
        Index(value = ["timestamp"]),
        Index(value = ["phone"]),
        Index(value = ["status"]),
        Index(value = ["created_date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "service_id")
    val serviceId: Int,
    
    @ColumnInfo(name = "service_name")
    val serviceName: String,
    
    @ColumnInfo(name = "phone")
    val phone: String = "",
    
    @ColumnInfo(name = "cedula")
    val cedula: String = "",
    
    @ColumnInfo(name = "amount")
    val amount: Long = 0, // Amount in cents to avoid floating point issues
    
    @ColumnInfo(name = "commission")
    val commission: Long = 0, // Commission in cents
    
    @ColumnInfo(name = "total")
    val total: Long = 0, // Total in cents
    
    @ColumnInfo(name = "reference")
    val reference: String = "",
    
    @ColumnInfo(name = "ussd_code")
    val ussdCode: String = "",
    
    @ColumnInfo(name = "status")
    val status: TransactionStatus = TransactionStatus.PENDING,
    
    @ColumnInfo(name = "type")
    val type: TransactionType = TransactionType.USSD,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "created_date")
    val createdDate: String = System.currentTimeMillis().formatAsDateTime(),
    
    @ColumnInfo(name = "print_data")
    val printData: String = "",
    
    @ColumnInfo(name = "response_data")
    val responseData: String = "",
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    
    @ColumnInfo(name = "is_printed")
    val isPrinted: Boolean = false,
    
    @ColumnInfo(name = "print_count")
    val printCount: Int = 0,
    
    @ColumnInfo(name = "last_print_time")
    val lastPrintTime: Long? = null,
    
    @ColumnInfo(name = "device_info")
    val deviceInfo: String = "",
    
    @ColumnInfo(name = "app_version")
    val appVersion: String = "",
    
    @ColumnInfo(name = "notes")
    val notes: String = ""
) {
    
    // Helper methods for UI display
    fun getFormattedAmount(): String = (amount / 100.0).formatAsCurrency()
    fun getFormattedCommission(): String = (commission / 100.0).formatAsCurrency()
    fun getFormattedTotal(): String = (total / 100.0).formatAsCurrency()
    fun getFormattedDate(): String = timestamp.formatAsDateTime()

    fun isSuccessful(): Boolean = status == TransactionStatus.COMPLETED
    fun hasFailed(): Boolean = status == TransactionStatus.FAILED
    fun isPending(): Boolean = status == TransactionStatus.PENDING
    
    fun canBePrinted(): Boolean = status == TransactionStatus.COMPLETED && printData.isNotEmpty()
    
    companion object {
        // Helper function to convert amount from user input to cents
        fun amountToCents(amount: Int): Long = (amount * 100).toLong()
        fun centsToAmount(cents: Long): Int = (cents / 100).toInt()
        
        // Factory methods
        fun create(
            serviceId: Int,
            serviceName: String,
            phone: String = "",
            cedula: String = "",
            amount: Int = 0,
            commission: Int = 0,
            reference: String = "",
            ussdCode: String = "",
            type: TransactionType = TransactionType.USSD
        ): TransactionEntity {
            val amountCents = amountToCents(amount)
            val commissionCents = amountToCents(commission)
            val totalCents = amountCents + commissionCents
            
            return TransactionEntity(
                serviceId = serviceId,
                serviceName = serviceName,
                phone = phone,
                cedula = cedula,
                amount = amountCents,
                commission = commissionCents,
                total = totalCents,
                reference = reference,
                ussdCode = ussdCode,
                type = type,
                status = TransactionStatus.PENDING
            )
        }
    }
}