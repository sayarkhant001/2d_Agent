package com.twoDLedger.logic

import com.twoDLedger.data.Bet

data class BetLineParseError(
    val lineNumber: Int,
    val rawLine: String,
    val reason: String,
    val invalidTokens: List<String> = emptyList()
)

sealed class LineParseResult {
    data class Success(val bets: List<Pair<String, Int>>) : LineParseResult()
    data class Error(val error: BetLineParseError) : LineParseResult()
    object Ignored : LineParseResult()
}

data class PasteValidationResult(
    val isValid: Boolean,
    val validBets: List<Pair<String, Int>>,
    val errors: List<BetLineParseError>
)

object TwoDBetParser {
    fun myanmarToEnglish(input: String): String {
        val myanmarDigits = "၀၁၂၃၄၅၆၇၈၉"
        val englishDigits = "0123456789"
        return input.map { c ->
            val idx = myanmarDigits.indexOf(c)
            if (idx >= 0) englishDigits[idx] else c
        }.joinToString("")
    }

    fun isVoucherMetadataLine(raw: String): Boolean {
        val trimmed = myanmarToEnglish(raw).trim()
        if (trimmed.isBlank()) return true
        if (trimmed.all { it in "-=*_— ၊။" }) return true
        val lower = trimmed.lowercase()
        return lower.contains("တင်ကွက်") || lower.contains("ဘောင်ချာ") ||
                lower.contains("အကြိမ်") || lower.contains("အချိန်") ||
                lower.contains("စုစုပေါင်း") || lower.contains("ကော်မရှင်")
    }

    fun parseLine(rawLine: String): List<Pair<String, Int>> {
        val clean = myanmarToEnglish(rawLine).trim()
        if (clean.isBlank() || isVoucherMetadataLine(clean)) return emptyList()

        // 1. Check for 2D shortcut patterns:
        // ထိပ် (Head: e.g. "2ထိပ် 500", "2ထိပ်=500")
        val headMatch = Regex("""^(\d)\s*ထိပ်\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (headMatch != null) {
            val d = headMatch.groupValues[1].toInt()
            val amt = headMatch.groupValues[2].toInt()
            return TwoDNumberGenerator.head(d).map { it to amt }
        }

        // နောက် (Tail: e.g. "5နောက် 500", "5နောက်=500")
        val tailMatch = Regex("""^(\d)\s*နောက်\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (tailMatch != null) {
            val d = tailMatch.groupValues[1].toInt()
            val amt = tailMatch.groupValues[2].toInt()
            return TwoDNumberGenerator.tail(d).map { it to amt }
        }

        // အပူး (Doubles: e.g. "အပူး 500", "ပူး 500", "အပူး=500")
        val doubleMatch = Regex("""^(?:အပူး|ပူး)\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (doubleMatch != null) {
            val amt = doubleMatch.groupValues[1].toInt()
            return TwoDNumberGenerator.doubleNumbers().map { it to amt }
        }

        // ပါဝါ (Power pairs: e.g. "ပါဝါ 500")
        val powerMatch = Regex("""^ပါဝါ\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (powerMatch != null) {
            val amt = powerMatch.groupValues[1].toInt()
            return TwoDNumberGenerator.power().map { it to amt }
        }

        // နက္ခတ် (Natkhat pairs: e.g. "နက္ခတ် 500")
        val natkhatMatch = Regex("""^နက္ခတ်\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (natkhatMatch != null) {
            val amt = natkhatMatch.groupValues[1].toInt()
            return TwoDNumberGenerator.natkhat().map { it to amt }
        }

        // ညီကို (Brothers: e.g. "ညီကို 500")
        val brotherMatch = Regex("""^ညီကို\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (brotherMatch != null) {
            val amt = brotherMatch.groupValues[1].toInt()
            return TwoDNumberGenerator.brothers().map { it to amt }
        }

        // စုံစုံ (Even-Even: e.g. "စုံစုံ 500")
        val evenEvenMatch = Regex("""^စုံစုံ\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (evenEvenMatch != null) {
            val amt = evenEvenMatch.groupValues[1].toInt()
            return TwoDNumberGenerator.evenEven().map { it to amt }
        }

        // မမ (Odd-Odd: e.g. "မမ 500")
        val oddOddMatch = Regex("""^မမ\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (oddOddMatch != null) {
            val amt = oddOddMatch.groupValues[1].toInt()
            return TwoDNumberGenerator.oddOdd().map { it to amt }
        }

        // စုံမ (Even-Odd: e.g. "စုံမ 500")
        val evenOddMatch = Regex("""^စုံမ\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (evenOddMatch != null) {
            val amt = evenOddMatch.groupValues[1].toInt()
            return TwoDNumberGenerator.evenOdd().map { it to amt }
        }

        // မစုံ (Odd-Even: e.g. "မစုံ 500")
        val oddEvenMatch = Regex("""^မစုံ\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (oddEvenMatch != null) {
            val amt = oddEvenMatch.groupValues[1].toInt()
            return TwoDNumberGenerator.oddEven().map { it to amt }
        }

        // ပတ် / အပါ (Roll/Include: e.g. "2ပတ် 500", "2ပတ်=500", "2အပါ 500", "2ပါ=500")
        val rollMatch = Regex("""^(\d)\s*(?:ပတ်|အပါ|ပါ)\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (rollMatch != null) {
            val d = rollMatch.groupValues[1].toInt()
            val amt = rollMatch.groupValues[2].toInt()
            return TwoDNumberGenerator.roll(d).map { it to amt }
        }

        // ဘရိတ် (Break: e.g. "5ဘရိတ် 500", "5ဘရိတ်=500")
        val breakMatch = Regex("""^(\d)\s*ဘရိတ်\s*[=:\- ]?\s*(\d+)\s*(?:ks|ကျပ်)?$""").find(clean)
        if (breakMatch != null) {
            val d = breakMatch.groupValues[1].toInt()
            val amt = breakMatch.groupValues[2].toInt()
            return TwoDNumberGenerator.breakNum(d).map { it to amt }
        }

        // 2. Standard 2-digit patterns: e.g. "12-34-56 = 1000", "12=500", "12R=500", "12/500", "12 r 500"
        val tailAmtMatch = Regex("""[=:\s/,\-_]+(\d+)\s*(?:ks|ကျပ်)?$""").find(clean) ?: return emptyList()
        val amount = tailAmtMatch.groupValues[1].toIntOrNull() ?: return emptyList()
        val numPart = clean.substring(0, tailAmtMatch.range.first).trim()

        val tokens = numPart.split(Regex("""[\s,./+၊။\-_]+""")).filter { it.isNotBlank() }
        val results = mutableListOf<Pair<String, Int>>()
        for (tok in tokens) {
            val isR = tok.endsWith("R", ignoreCase = true)
            val digits = tok.replace(Regex("""(?i)R"""), "")
            if (digits.length == 2 && digits.all { it.isDigit() }) {
                if (isR) {
                    TwoDNumberGenerator.reverse(digits).forEach { results.add(it to amount) }
                } else {
                    results.add(digits to amount)
                }
            }
        }
        return results
    }

    fun parsePastedText(rawText: String): List<Bet> {
        val list = mutableListOf<Bet>()
        rawText.lines().forEach { line ->
            parseLine(line).forEach { (num, amt) ->
                if (amt > 0) list.add(Bet(voucherId = 0, number = num, amount = amt))
            }
        }
        return list
    }
}
