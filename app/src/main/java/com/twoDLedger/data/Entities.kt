package com.twoDLedger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "customers")
@Serializable
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val commissionRate: Double = 0.0,
    val multiplier: Int = 80, // Standard 2D multiplier: 80x
    val paidAmount: Double = 0.0
)

@Entity(
    tableName = "vouchers",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["batchNumber"]),
        Index(value = ["session"]),
        Index(value = ["isArchived"])
    ]
)
@Serializable
data class Voucher(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val batchNumber: Int = 1,
    val session: String = "12:00 PM", // "12:00 PM" (Noon) or "4:30 PM" (Evening)
    val date: String,
    val time: String, // e.g. "12:00 PM"
    val totalAmount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val remark: String = ""
)

@Entity(
    tableName = "bets",
    indices = [
        Index(value = ["voucherId"]),
        Index(value = ["number"])
    ]
)
@Serializable
data class Bet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val voucherId: Int,
    val number: String, // 2-digit number (00 to 99)
    val amount: Int
)

@Entity(tableName = "export_records")
data class ExportRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val batchNumber: Int,
    val session: String = "12:00 PM",
    val type: String,
    val totalAmount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

@Entity(tableName = "banned_numbers")
data class BannedNumber(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val amountLimit: Int = 0 // 0 = completely banned (လုံးဝပိတ်), > 0 = maximum bet limit ceiling in Ks
)

data class BannedLimitRemoval(
    val number: String,
    val attemptedAmount: Int,
    val limitAmount: Int, // 0 = completely banned
    val currentBetTotal: Int,
    val acceptedAmount: Int,
    val removedAmount: Int,
    val reason: String
)

@Entity(
    tableName = "exported_numbers",
    indices = [
        Index(value = ["exportRecordId"]),
        Index(value = ["number"])
    ]
)
data class ExportedNumber(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exportRecordId: Int,
    val number: String,
    val amount: Int
)

@Entity(tableName = "winning_history")
data class WinningHistory(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val num900: String = "",      // 9:00 AM Modern indicator
    val num1200: String = "",     // 12:00 PM Official Winner
    val num1400: String = "",     // 2:00 PM Modern indicator
    val num1630: String = "",     // 4:30 PM Official Winner
    val set1200: String = "",
    val val1200: String = "",
    val set1630: String = "",
    val val1630: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
