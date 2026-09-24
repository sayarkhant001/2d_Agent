package com.twoDLedger.logic

object TwoDNumberGenerator {
    // ထိပ် (Head/Prefix): 10 numbers starting with d (e.g. 2 -> 20, 21, ..., 29)
    fun head(d: Int): List<String> = (0..9).map { "$d$it" }

    // နောက် (Tail/Suffix): 10 numbers ending with d (e.g. 5 -> 05, 15, ..., 95)
    fun tail(d: Int): List<String> = (0..9).map { "$it$d" }

    // အပူး (Doubles/Twins): 10 numbers (00, 11, 22, ..., 99)
    fun doubleNumbers(): List<String> = (0..9).map { "$it$it" }

    // R / ပြန် (Reversal): given a 2-digit number "12", returns ["12", "21"]
    fun reverse(str: String): List<String> {
        val clean = str.padStart(2, '0').takeLast(2)
        val rev = clean.reversed()
        return if (clean == rev) listOf(clean) else listOf(clean, rev)
    }

    // ပါဝါ (Power pairs): 05, 16, 27, 38, 49 + reversals (10 numbers)
    fun power(): List<String> = listOf("05", "50", "16", "61", "27", "72", "38", "83", "49", "94")

    // နက္ခတ် (Natkhat pairs): 07, 18, 24, 35, 69 + reversals (10 numbers)
    fun natkhat(): List<String> = listOf("07", "70", "18", "81", "24", "42", "35", "53", "69", "96")

    // ညီကို (Brothers / Consecutive digits): 01, 12, 23, 34, 45, 56, 67, 78, 89, 90 + reversals (20 numbers)
    fun brothers(): List<String> = (0..9).flatMap { i ->
        val next = (i + 1) % 10
        listOf("$i$next", "$next$i")
    }.distinct()

    // စုံစုံ (Even-Even): 25 numbers (00, 02, ..., 88)
    fun evenEven(): List<String> = listOf(0, 2, 4, 6, 8).flatMap { i ->
        listOf(0, 2, 4, 6, 8).map { j -> "$i$j" }
    }

    // မမ (Odd-Odd): 25 numbers (11, 13, ..., 99)
    fun oddOdd(): List<String> = listOf(1, 3, 5, 7, 9).flatMap { i ->
        listOf(1, 3, 5, 7, 9).map { j -> "$i$j" }
    }

    // စုံမ (Even-Odd): 25 numbers
    fun evenOdd(): List<String> = listOf(0, 2, 4, 6, 8).flatMap { i ->
        listOf(1, 3, 5, 7, 9).map { j -> "$i$j" }
    }

    // မစုံ (Odd-Even): 25 numbers
    fun oddEven(): List<String> = listOf(1, 3, 5, 7, 9).flatMap { i ->
        listOf(0, 2, 4, 6, 8).map { j -> "$i$j" }
    }

    // ဘရိတ် (Break/Sum): 10 numbers whose digits sum % 10 == target
    fun breakNum(d: Int): List<String> = (0..99).map { it.toString().padStart(2, '0') }
        .filter { s -> (s[0].digitToInt() + s[1].digitToInt()) % 10 == d }
}
