package com.twoDLedger

import com.twoDLedger.logic.TwoDBetParser
import com.twoDLedger.logic.TwoDNumberGenerator
import com.twoDLedger.ui.validateAndParseLine
import com.twoDLedger.ui.validatePastedText
import com.twoDLedger.ui.LineParseResult
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

        // Tail (နောက်)
        val tail = TwoDBetParser.parseLine("5နောက် 500")
        assertEquals(10, tail.size)
        assertTrue(tail.all { it.second == 500 })
        assertTrue(tail.map { it.first }.containsAll(listOf("05", "15", "25", "95")))

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
    }

    @Test
    fun testValidationAndPastedText() {
        val validText = """
            12-34 = 1000
            23R = 500
            5နောက် 200
        """.trimIndent()
        val result = validatePastedText(validText)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.validBets.isNotEmpty())

        val invalidText = """
            123 = 1000
            xyz = 500
        """.trimIndent()
        val badResult = validatePastedText(invalidText)
        assertFalse(badResult.isValid)
        assertTrue(badResult.errors.isNotEmpty())
    }
}
