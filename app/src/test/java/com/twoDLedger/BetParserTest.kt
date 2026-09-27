package com.twoDLedger

import com.twoDLedger.logic.TwoDBetParser
import com.twoDLedger.logic.TwoDNumberGenerator
import org.junit.Assert.*
import org.junit.Test

class BetParserTest {

    @Test
    fun testTwoDStandardLines() {
        val l1 = TwoDBetParser.parseLine("12-34-56 = 2000")
        assertEquals(3, l1.size)
        assertEquals("12" to 2000, l1[0])
        assertEquals("34" to 2000, l1[1])
        assertEquals("56" to 2000, l1[2])

        val l2 = TwoDBetParser.parseLine("46=1000")
        assertEquals(1, l2.size)
        assertEquals("46" to 1000, l2[0])

        val l3 = TwoDBetParser.parseLine("23R=500")
        assertEquals(2, l3.size)
        assertEquals("23" to 500, l3[0])
        assertEquals("32" to 500, l3[1])

        val l4 = TwoDBetParser.parseLine("77R=1000")
        assertEquals(1, l4.size)
        assertEquals("77" to 1000, l4[0])

        // Delimiter with * and x
        val l5 = TwoDBetParser.parseLine("12*500")
        assertEquals(1, l5.size)
        assertEquals("12" to 500, l5[0])

        // Delimiter with x
        val l6 = TwoDBetParser.parseLine("34x1000")
        assertEquals(1, l6.size)
        assertEquals("34" to 1000, l6[0])

        // Reversal with ပြန်
        val l7 = TwoDBetParser.parseLine("23ပြန်=500")
        assertEquals(2, l7.size)
        assertEquals("23" to 500, l7[0])
        assertEquals("32" to 500, l7[1])

        // Multiple numbers with global R or ပြန်
        val l8 = TwoDBetParser.parseLine("12 34 R 500")
        assertEquals(4, l8.size)
        assertTrue(l8.contains("12" to 500))
        assertTrue(l8.contains("21" to 500))
        assertTrue(l8.contains("34" to 500))
        assertTrue(l8.contains("43" to 500))
    }

    @Test
    fun testTwoDBurmeseDigits() {
        val l = TwoDBetParser.parseLine("၁၂-၃၄=၅၀၀")
        assertEquals(2, l.size)
        assertEquals("12" to 500, l[0])
        assertEquals("34" to 500, l[1])
    }

    @Test
    fun testTwoDShortcuts() {
        // Head (ထိပ်)
        val head = TwoDBetParser.parseLine("2ထိပ် 500")
        assertEquals(10, head.size)
        assertTrue(head.all { it.second == 500 })
        assertTrue(head.map { it.first }.containsAll(listOf("20", "21", "22", "29")))

        // Tail (ပိတ်)
        val peik = TwoDBetParser.parseLine("5ပိတ် 500")
        assertEquals(10, peik.size)
        assertTrue(peik.all { it.second == 500 })
        assertTrue(peik.map { it.first }.containsAll(listOf("05", "15", "25", "95")))

        // Tail (နောက် - legacy backwards compatibility)
        val nauk = TwoDBetParser.parseLine("5နောက် 500")
        assertEquals(10, nauk.size)
        assertTrue(nauk.all { it.second == 500 })
        assertTrue(nauk.map { it.first }.containsAll(listOf("05", "15", "25", "95")))

        // Doubles (အပူး)
        val doubles = TwoDBetParser.parseLine("အပူး 1000")
        assertEquals(10, doubles.size)
        assertTrue(doubles.map { it.first }.containsAll(listOf("00", "11", "55", "99")))

        // Power (ပါဝါ)
        val power = TwoDBetParser.parseLine("ပါဝါ 500")
        assertEquals(10, power.size)
        assertTrue(power.map { it.first }.containsAll(listOf("05", "50", "16", "61", "27", "72", "38", "83", "49", "94")))

        // Natkhat (နက္ခတ်)
        val natkhat = TwoDBetParser.parseLine("နက္ခတ် 500")
        assertEquals(10, natkhat.size)

        // Brothers (ညီကို)
        val bro = TwoDBetParser.parseLine("ညီကို 300")
        assertEquals(20, bro.size)

        // Even-Even (စုံစုံ)
        val ee = TwoDBetParser.parseLine("စုံစုံ 200")
        assertEquals(25, ee.size)

        // Odd-Odd (မမ)
        val oo = TwoDBetParser.parseLine("မမ 200")
        assertEquals(25, oo.size)

        // Roll (ပတ်)
        val roll = TwoDBetParser.parseLine("7ပတ် 500")
        assertEquals(19, roll.size)

        // Break (ဘရိတ်)
        val brk = TwoDBetParser.parseLine("5ဘရိတ် 1000")
        assertEquals(10, brk.size)
    }

    @Test
    fun testValidationAndPastedText() {
        val validText = """
            12-34 = 1000
            23R = 500
            5ပိတ် 200
            5နောက် 200
            7ထိပ် = 500
            အပူး 1000
        """.trimIndent()
        val result = TwoDBetParser.validatePastedText(validText)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.validBets.isNotEmpty())

        val invalidText = """
            123 = 1000
            xyz = 500
        """.trimIndent()
        val badResult = TwoDBetParser.validatePastedText(invalidText)
        assertFalse(badResult.isValid)
        assertEquals(2, badResult.errors.size)
        assertEquals(1, badResult.errors[0].lineNumber)
        assertEquals(2, badResult.errors[1].lineNumber)
    }
}
