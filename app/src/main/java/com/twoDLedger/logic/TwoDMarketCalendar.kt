package com.twoDLedger.logic

import com.twoDLedger.network.TwoDHolidayItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SETHoliday(
    val date: String, // "yyyy-MM-dd"
    val nameEn: String,
    val nameMm: String
)

enum class DayMarketState {
    OPEN,            // Normal trading day (Mon - Fri)
    WEEKEND_CLOSED,  // Saturday or Sunday
    HOLIDAY_CLOSED   // Official SET public holiday
}

data class MarketDayInfo(
    val year: Int,
    val month: Int, // 1-12
    val day: Int,
    val dayOfWeek: Int, // Calendar.SUNDAY = 1, etc.
    val dateFormatted: String, // "yyyy-MM-dd"
    val state: DayMarketState,
    val titleMm: String,
    val reasonMm: String,
    val isToday: Boolean
)

data class TodayMarketOverview(
    val isOpen: Boolean,
    val statusBadge: String,
    val details: String,
    val reason: String,
    val morningSession: String = "မနက်ပိုင်း: ၉:၃၀ မှ ၁၂:၀၁ အထိ",
    val eveningSession: String = "ညနေပိုင်း: ၂:၀၀ မှ ၄:၃၀ အထိ",
    val officialSource: String = "Stock Exchange of Thailand (SET) & Live API"
)

object TwoDMarketCalendar {

    // ── Official Stock Exchange of Thailand (SET) Holiday Calendar ──────────────
    // Sourced from official SET (set.or.th) trading calendar
    val officialHolidays: List<SETHoliday> = listOf(
        // 2025 SET Holidays
        SETHoliday("2025-01-01", "New Year's Day", "နှစ်သစ်ကူး ရုံးပိတ်ရက်"),
        SETHoliday("2025-02-12", "Makha Bucha Day", "မာခါဘူချာနေ့"),
        SETHoliday("2025-04-07", "Substitution for Chakri Day", "ချက်ကရီနေ့ အစားထိုးပိတ်ရက်"),
        SETHoliday("2025-04-14", "Songkran Festival", "ထိုင်းသင်္ကြန်ပွဲတော် ပိတ်ရက်"),
        SETHoliday("2025-04-15", "Songkran Festival", "ထိုင်းသင်္ကြန်ပွဲတော် ပိတ်ရက်"),
        SETHoliday("2025-05-01", "National Labour Day", "အပြည်ပြည်ဆိုင်ရာ အလုပ်သမားနေ့"),
        SETHoliday("2025-05-05", "Substitution for Coronation Day", "နန်းတက်ပွဲ အစားထိုးပိတ်ရက်"),
        SETHoliday("2025-05-12", "Substitution for Visakha Bucha Day", "ကဆုန်လပြည့် ဗုဒ္ဓနေ့ အစားထိုးပိတ်ရက်"),
        SETHoliday("2025-06-03", "H.M. Queen Suthida's Birthday", "မိဖုရားကြီး သုထိတာ မွေးနေ့"),
        SETHoliday("2025-07-10", "Asarnha Bucha Day", "ဝါဆိုလပြည့်နေ့"),
        SETHoliday("2025-07-28", "H.M. King Maha Vajiralongkorn's Birthday", "ဘုရင်မင်းမြတ် မွေးနေ့"),
        SETHoliday("2025-08-12", "H.M. Queen Sirikit's Birthday / Mother's Day", "မိခင်များနေ့ ရုံးပိတ်ရက်"),
        SETHoliday("2025-10-13", "King Bhumibol Adulyadej Memorial Day", "ဘုရင်ကြီး ဘူမိဘော အောက်မေ့ဖွယ်နေ့"),
        SETHoliday("2025-10-23", "King Chulalongkorn Memorial Day", "ချူလာလောင်ကွန်းနေ့"),
        SETHoliday("2025-12-05", "King Bhumibol Adulyadej's Birthday", "ဖခင်များနေ့ ရုံးပိတ်ရက်"),
        SETHoliday("2025-12-10", "Constitution Day", "ဖွဲ့စည်းပုံအခြေခံဥပဒေနေ့"),
        SETHoliday("2025-12-31", "New Year's Eve", "နှစ်ဟောင်းကုန် ရုံးပိတ်ရက်"),

        // 2026 SET Holidays
        SETHoliday("2026-01-01", "New Year's Day", "နှစ်သစ်ကူး ရုံးပိတ်ရက်"),
        SETHoliday("2026-01-02", "Special SET Holiday", "အထူးရုံးပိတ်ရက်"),
        SETHoliday("2026-03-03", "Makha Bucha Day", "မာခါဘူချာနေ့"),
        SETHoliday("2026-04-06", "Chakri Memorial Day", "ချက်ကရီနေ့"),
        SETHoliday("2026-04-13", "Songkran Festival", "ထိုင်းသင်္ကြန်ပွဲတော် ပိတ်ရက်"),
        SETHoliday("2026-04-14", "Songkran Festival", "ထိုင်းသင်္ကြန်ပွဲတော် ပိတ်ရက်"),
        SETHoliday("2026-04-15", "Songkran Festival", "ထိုင်းသင်္ကြန်ပွဲတော် ပိတ်ရက်"),
        SETHoliday("2026-05-01", "National Labour Day", "အပြည်ပြည်ဆိုင်ရာ အလုပ်သမားနေ့"),
        SETHoliday("2026-05-04", "Coronation Day", "နန်းတက်ပွဲ အထိမ်းအမှတ်နေ့"),
        SETHoliday("2026-06-01", "Substitution for Visakha Bucha Day", "ကဆုန်လပြည့် ဗုဒ္ဓနေ့ အစားထိုးပိတ်ရက်"),
        SETHoliday("2026-06-03", "H.M. Queen Suthida's Birthday", "မိဖုရားကြီး သုထိတာ မွေးနေ့"),
        SETHoliday("2026-07-28", "H.M. King Maha Vajiralongkorn's Birthday", "ဘုရင်မင်းမြတ် မွေးနေ့"),
        SETHoliday("2026-07-29", "Asarnha Bucha Day", "ဝါဆိုလပြည့်နေ့"),
        SETHoliday("2026-08-12", "H.M. Queen Sirikit's Birthday / Mother's Day", "မိခင်များနေ့ ရုံးပိတ်ရက်"),
        SETHoliday("2026-10-13", "King Bhumibol Memorial Day", "ဘုရင်ကြီး ဘူမိဘော အောက်မေ့ဖွယ်နေ့"),
        SETHoliday("2026-10-16", "Special Public Holiday", "အထူးရုံးပိတ်ရက်"),
        SETHoliday("2026-10-23", "King Chulalongkorn Memorial Day", "ချူလာလောင်ကွန်းနေ့"),
        SETHoliday("2026-12-07", "Substitution for King Bhumibol Birthday", "ဖခင်များနေ့ အစားထိုးပိတ်ရက်"),
        SETHoliday("2026-12-10", "Constitution Day", "ဖွဲ့စည်းပုံအခြေခံဥပဒေနေ့"),
        SETHoliday("2026-12-31", "New Year's Eve", "နှစ်ဟောင်းကုန် ရုံးပိတ်ရက်")
    )

    fun getHoliday(dateStr: String): SETHoliday? {
        return officialHolidays.find { it.date == dateStr }
    }

    fun isWeekend(dayOfWeek: Int): Boolean {
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }

    fun checkDate(year: Int, month: Int, day: Int, todayStr: String): MarketDayInfo {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val dateFormatted = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        val isToday = dateFormatted == todayStr

        val officialHoliday = getHoliday(dateFormatted)
        val isWk = isWeekend(dow)

        return when {
            officialHoliday != null -> {
                MarketDayInfo(
                    year = year,
                    month = month,
                    day = day,
                    dayOfWeek = dow,
                    dateFormatted = dateFormatted,
                    state = DayMarketState.HOLIDAY_CLOSED,
                    titleMm = "ပိတ်သည် (${officialHoliday.nameMm})",
                    reasonMm = "SET တရားဝင် ရုံးပိတ်ရက်: ${officialHoliday.nameEn}",
                    isToday = isToday
                )
            }
            isWk -> {
                val dowName = if (dow == Calendar.SATURDAY) "စနေနေ့" else "တနင်္ဂနွေနေ့"
                MarketDayInfo(
                    year = year,
                    month = month,
                    day = day,
                    dayOfWeek = dow,
                    dateFormatted = dateFormatted,
                    state = DayMarketState.WEEKEND_CLOSED,
                    titleMm = "ပိတ်သည် ($dowName)",
                    reasonMm = "စနေ/တနင်္ဂနွေ အပတ်စဉ် ဈေးကွက်ပိတ်ရက်",
                    isToday = isToday
                )
            }
            else -> {
                MarketDayInfo(
                    year = year,
                    month = month,
                    day = day,
                    dayOfWeek = dow,
                    dateFormatted = dateFormatted,
                    state = DayMarketState.OPEN,
                    titleMm = "ဖွင့်လှစ်သည်",
                    reasonMm = "ပုံမှန် 2D ဈေးကွက်ဖွင့်ရက် (၁၂:၀၁ / ၄:၃၀)",
                    isToday = isToday
                )
            }
        }
    }

    fun getMonthDays(year: Int, month: Int): List<MarketDayInfo> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<MarketDayInfo>()
        for (d in 1..maxDays) {
            list.add(checkDate(year, month, d, todayStr))
        }
        return list
    }

    fun getTodayOverview(apiHoliday: TwoDHolidayItem? = null): TodayMarketOverview {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        val day = now.get(Calendar.DAY_OF_MONTH)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
        val dayInfo = checkDate(year, month, day, todayStr)

        // If the live API returns an explicit holiday marker
        val apiIndicatesHoliday = apiHoliday != null && (
            (apiHoliday.name.isNotBlank() && apiHoliday.name.lowercase() != "null") ||
            apiHoliday.status == "3" ||
            apiHoliday.status == "2"
        )
        val apiReason = apiHoliday?.name?.takeIf { it.isNotBlank() && it.lowercase() != "null" }

        val isActuallyClosed = dayInfo.state != DayMarketState.OPEN || apiIndicatesHoliday

        return if (isActuallyClosed) {
            val finalReason = when {
                apiReason != null && apiReason.isNotBlank() -> "တရားဝင် Live API မှ အသိပေးချက်: $apiReason"
                dayInfo.state == DayMarketState.HOLIDAY_CLOSED -> dayInfo.reasonMm
                dayInfo.state == DayMarketState.WEEKEND_CLOSED -> dayInfo.reasonMm
                else -> "2D ဈေးကွက် ပိတ်ထားပါသည်"
            }
            TodayMarketOverview(
                isOpen = false,
                statusBadge = "ဈေးကွက် ပိတ်ပါသည်",
                details = "ယနေ့ ထိုင်းစတော့အိတ်ချိန်း (SET) 2D ဈေးကွက် ပိတ်ထားပါသည်",
                reason = finalReason
            )
        } else {
            TodayMarketOverview(
                isOpen = true,
                statusBadge = "ဈေးကွက် ဖွင့်လှစ်ပါသည်",
                details = "ယနေ့ 2D ပုံမှန် ဈေးကွက်ဖွင့်လှစ်ပါသည် (၂ ကြိမ် ထွက်ရှိမည်)",
                reason = "တနင်္လာ မှ သောကြာ ပုံမှန်ရုံးဖွင့်ရက်"
            )
        }
    }

    fun getUpcomingHolidays(limit: Int = 5): List<SETHoliday> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return officialHolidays
            .filter { it.date >= todayStr }
            .sortedBy { it.date }
            .take(limit)
    }
}
