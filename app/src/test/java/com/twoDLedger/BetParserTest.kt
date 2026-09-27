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

        // Comma separated: 23,34,56=1000
        val commaLine = TwoDBetParser.parseLine("23,34,56=1000")
        assertEquals(3, commaLine.size)
        assertEquals("23" to 1000, commaLine[0])
        assertEquals("34" to 1000, commaLine[1])
        assertEquals("56" to 1000, commaLine[2])

        // 3D style dual amount: 23=1000R500 (23 is 1000, reverse 32 is 500)
        val dualR = TwoDBetParser.parseLine("23=1000R500")
        assertEquals(2, dualR.size)
        assertEquals("23" to 1000, dualR[0])
        assertEquals("32" to 500, dualR[1])

        // 3D style dual amount with / : 23=1000/500
        val dualSlash = TwoDBetParser.parseLine("23=1000/500")
        assertEquals(2, dualSlash.size)
        assertEquals("23" to 1000, dualSlash[0])
        assertEquals("32" to 500, dualSlash[1])

        // Multiple numbers with dual amount: 23,34=1000R500
        val multiDual = TwoDBetParser.parseLine("23,34=1000R500")
        assertEquals(4, multiDual.size)
        assertEquals("23" to 1000, multiDual[0])
        assertEquals("32" to 500, multiDual[1])
        assertEquals("34" to 1000, multiDual[2])
        assertEquals("43" to 500, multiDual[3])

        // / means Reverse: 23/=1000
        val slashR = TwoDBetParser.parseLine("23/=1000")
        assertEquals(2, slashR.size)
        assertEquals("23" to 1000, slashR[0])
        assertEquals("32" to 1000, slashR[1])

        // Only = accepted for amount: lines with * or x or / without = are NOT accepted
        assertTrue(TwoDBetParser.parseLine("12*500").isEmpty())
        assertTrue(TwoDBetParser.parseLine("34x1000").isEmpty())
        assertTrue(TwoDBetParser.parseLine("12/500").isEmpty())
        assertTrue(TwoDBetParser.parseLine("12 500").isEmpty())

        // Reversal with ပြန်
        val l7 = TwoDBetParser.parseLine("23ပြန်=500")
        assertEquals(2, l7.size)
        assertEquals("23" to 500, l7[0])
        assertEquals("32" to 500, l7[1])

        // Multiple numbers with global R, /, or ပြန်
        val l8 = TwoDBetParser.parseLine("12 34 R = 500")
        assertEquals(4, l8.size)
        assertTrue(l8.contains("12" to 500))
        assertTrue(l8.contains("21" to 500))
        assertTrue(l8.contains("34" to 500))
        assertTrue(l8.contains("43" to 500))

        val l9 = TwoDBetParser.parseLine("12 34 / = 500")
        assertEquals(4, l9.size)
        assertTrue(l9.contains("12" to 500))
        assertTrue(l9.contains("21" to 500))
        assertTrue(l9.contains("34" to 500))
        assertTrue(l9.contains("43" to 500))
    }

    @Test
    fun testTwoDBurmeseDigits() {
        val l = TwoDBetParser.parseLine("၁၂-၃၄=၅၀၀")
        assertEquals(2, l.size)
        assertEquals("12" to 500, l[0])
        assertEquals("34" to 500, l[1])

        val burmeseDual = TwoDBetParser.parseLine("၂၃=၁၀၀၀R၅၀၀")
        assertEquals(2, burmeseDual.size)
        assertEquals("23" to 1000, burmeseDual[0])
        assertEquals("32" to 500, burmeseDual[1])
    }

    @Test
    fun testTwoDShortcuts() {
        // Head (ထိပ်)
        val head = TwoDBetParser.parseLine("2ထိပ်=500")
        assertEquals(10, head.size)
        assertTrue(head.all { it.second == 500 })
        assertTrue(head.map { it.first }.containsAll(listOf("20", "21", "22", "29")))

        // Tail (ပိတ်)
        val peik = TwoDBetParser.parseLine("5ပိတ်=500")
        assertEquals(10, peik.size)
        assertTrue(peik.all { it.second == 500 })
        assertTrue(peik.map { it.first }.containsAll(listOf("05", "15", "25", "95")))

        // Tail (နောက် - legacy backwards compatibility)
        val nauk = TwoDBetParser.parseLine("5နောက်=500")
        assertEquals(10, nauk.size)
        assertTrue(nauk.all { it.second == 500 })
        assertTrue(nauk.map { it.first }.containsAll(listOf("05", "15", "25", "95")))

        // Doubles (အပူး)
        val doubles = TwoDBetParser.parseLine("အပူး=1000")
        assertEquals(10, doubles.size)
        assertTrue(doubles.map { it.first }.containsAll(listOf("00", "11", "55", "99")))

        // Power (ပါဝါ)
        val power = TwoDBetParser.parseLine("ပါဝါ=500")
        assertEquals(10, power.size)
        assertTrue(power.map { it.first }.containsAll(listOf("05", "50", "16", "61", "27", "72", "38", "83", "49", "94")))

        // Natkhat (နက္ခတ်)
        val natkhat = TwoDBetParser.parseLine("နက္ခတ်=500")
        assertEquals(10, natkhat.size)

        // Brothers (ညီကို)
        val bro = TwoDBetParser.parseLine("ညီကို=300")
        assertEquals(20, bro.size)

        // Even-Even (စုံစုံ)
        val ee = TwoDBetParser.parseLine("စုံစုံ=200")
        assertEquals(25, ee.size)

        // Odd-Odd (မမ)
        val oo = TwoDBetParser.parseLine("မမ=200")
        assertEquals(25, oo.size)

        // Roll (ပတ်)
        val roll = TwoDBetParser.parseLine("7ပတ်=500")
        assertEquals(19, roll.size)

        // Break (ဘရိတ်)
        val brk = TwoDBetParser.parseLine("5ဘရိတ်=1000")
        assertEquals(10, brk.size)
    }

    @Test
    fun testValidationAndPastedText() {
        val validText = """
            12-34 = 1000
            23,34,56 = 1000
            23 = 1000R500
            23 = 1000/500
            23R = 500
            23/ = 500
            5ပိတ် = 200
            5နောက် = 200
            7ထိပ် = 500
            အပူး = 1000
        """.trimIndent()
        val result = TwoDBetParser.validatePastedText(validText)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.validBets.isNotEmpty())

        val invalidText = """
            123 = 1000
            xyz = 500
            12*500
        """.trimIndent()
        val badResult = TwoDBetParser.validatePastedText(invalidText)
        assertFalse(badResult.isValid)
        assertEquals(3, badResult.errors.size)
        assertEquals(1, badResult.errors[0].lineNumber)
        assertEquals(2, badResult.errors[1].lineNumber)
        assertEquals(3, badResult.errors[2].lineNumber)
    }
}
