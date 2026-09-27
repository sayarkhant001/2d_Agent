package com.twoDLedger

import com.twoDLedger.logic.TwoDNumberGenerator
import com.twoDLedger.logic.TwoDBetParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTwoDNumberGenerator() {
        // Head / Prefix
        val head2 = TwoDNumberGenerator.head(2)
        assertEquals(10, head2.size)
        assertEquals("20", head2.first())
        assertEquals("29", head2.last())

        // Tail / Suffix
        val tail5 = TwoDNumberGenerator.tail(5)
        assertEquals(10, tail5.size)
        assertEquals("05", tail5.first())
        assertEquals("95", tail5.last())

        // Doubles / Twins
        val doubles = TwoDNumberGenerator.doubleNumbers()
        assertEquals(10, doubles.size)
        assertEquals("00", doubles.first())
        assertEquals("99", doubles.last())

        // Reverse
        val rev12 = TwoDNumberGenerator.reverse("12")
        assertEquals(listOf("12", "21"), rev12)
        val rev55 = TwoDNumberGenerator.reverse("55")
        assertEquals(listOf("55"), rev55)

        // Roll / Include (19 numbers containing the digit)
        val roll2 = TwoDNumberGenerator.roll(2)
        assertEquals(19, roll2.size)
        assertTrue(roll2.contains("02"))
        assertTrue(roll2.contains("20"))
        assertTrue(roll2.contains("22"))
        assertTrue(roll2.contains("92"))

        // Break
        val break5 = TwoDNumberGenerator.breakNum(5)
        assertEquals(10, break5.size)
        assertTrue(break5.all { (it[0].digitToInt() + it[1].digitToInt()) % 10 == 5 })

        // Even-Odd / Odd-Even
        assertEquals(25, TwoDNumberGenerator.evenOdd().size)
        assertEquals(25, TwoDNumberGenerator.oddEven().size)
    }

    @Test
    fun testTwoDBetParser() {
        // Direct bet
        val bets1 = TwoDBetParser.parseLine("12 500")
        assertEquals(1, bets1.size)
        assertEquals("12" to 500, bets1.first())

        // R bet
        val betsR = TwoDBetParser.parseLine("12R 500")
        assertEquals(2, betsR.size)
        assertTrue(betsR.contains("12" to 500))
        assertTrue(betsR.contains("21" to 500))

        // Roll / ပတ် bet
        val rollBets = TwoDBetParser.parseLine("2ပတ် 1000")
        assertEquals(19, rollBets.size)
        assertTrue(rollBets.contains("02" to 1000))
        assertTrue(rollBets.contains("20" to 1000))

        // Break / ဘရိတ် bet
        val breakBets = TwoDBetParser.parseLine("7ဘရိတ် 300")
        assertEquals(10, breakBets.size)
        assertTrue(breakBets.contains("25" to 300))
        assertTrue(breakBets.contains("52" to 300))

        // Even-Odd / စုံမ
        val evenOddBets = TwoDBetParser.parseLine("စုံမ 200")
        assertEquals(25, evenOddBets.size)

        // Odd-Even / မစုံ
        val oddEvenBets = TwoDBetParser.parseLine("မစုံ 200")
        assertEquals(25, oddEvenBets.size)
    }

    @Test
    fun testNumberedVoucherLineRegexStripping() {
        val stripRegex = Regex("""^\s*\d+[\.\)\-:]\s*""")

        val line1 = "1. 12 = 500"
        val stripped1 = line1.replaceFirst(stripRegex, "").trim()
        assertEquals("12 = 500", stripped1)

        val line2 = " 25) 45 = 1000 "
        val stripped2 = line2.replaceFirst(stripRegex, "").trim()
        assertEquals("45 = 1000", stripped2)

        val line3 = "3- 89-500"
        val stripped3 = line3.replaceFirst(stripRegex, "").trim()
        assertEquals("89-500", stripped3)
    }
}
