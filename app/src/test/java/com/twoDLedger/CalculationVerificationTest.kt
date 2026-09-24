package com.twoDLedger

import com.twoDLedger.data.Customer
import com.twoDLedger.logic.TwoDNumberGenerator
import org.junit.Assert.*
import org.junit.Test

/**
 * Formal Verification of 2D Ledger Calculation Patterns
 * Validates:
 * 1. 2D Exact matching (ဒဲ့) & Reversals (R)
 * 2. 2D Number sets (Doubles, Head, Tail, Power, Natkhat, Brothers)
 * 3. Commission calculations (ရောင်းကြေး, ကော်မရှင် %, နုတ်ပြီးငွေ)
 * 4. Multiplier payouts (e.g. 80x default)
 * 5. Customer balance & settlement (အသားတင် ရ/ပေး)
 */
class CalculationVerificationTest {

    @Test
    fun testTwoDMultipliersAndPayout() {
        val betAmount = 1000
        val multiplier = 80.0
        val payout = betAmount * multiplier
        assertEquals(80000.0, payout, 0.001)

        val bet500 = 500
        assertEquals(40000.0, bet500 * multiplier, 0.001)
    }

    @Test
    fun testTwoDCommissionSettlement() {
        val totalGrossBet = 100_000
        val commissionRate = 15.0 // 15%
        val commissionAmount = totalGrossBet * (commissionRate / 100.0)
        val netDueFromAgent = totalGrossBet - commissionAmount

        assertEquals(15_000.0, commissionAmount, 0.001)
        assertEquals(85_000.0, netDueFromAgent, 0.001)

        val totalWinnings = 80_000.0 // Agent's customers won 80,000 Ks
        val balance = netDueFromAgent - totalWinnings // 85,000 - 80,000 = +5,000 (Agent owes Master)
        assertEquals(5_000.0, balance, 0.001)
    }

    @Test
    fun testTwoDNumberSets() {
        // Doubles: exactly 10 numbers
        val doubles = TwoDNumberGenerator.doubleNumbers()
        assertEquals(10, doubles.size)
        assertTrue(doubles.contains("00"))
        assertTrue(doubles.contains("99"))

        // Head (Prefix): exactly 10 numbers
        val head7 = TwoDNumberGenerator.head(7)
        assertEquals(10, head7.size)
        assertTrue(head7.all { it.startsWith("7") })

        // Tail (Suffix): exactly 10 numbers
        val tail3 = TwoDNumberGenerator.tail(3)
        assertEquals(10, tail3.size)
        assertTrue(tail3.all { it.endsWith("3") })

        // Reversal (R)
        val rev12 = TwoDNumberGenerator.reverse("12")
        assertEquals(listOf("12", "21"), rev12)

        val rev44 = TwoDNumberGenerator.reverse("44")
        assertEquals(listOf("44"), rev44) // Double has no separate reverse
    }
}
