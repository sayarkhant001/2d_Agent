package com.twoDLedger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twoDLedger.data.WinningHistory
import com.twoDLedger.logic.DayMarketState
import com.twoDLedger.logic.MarketDayInfo
import com.twoDLedger.logic.TwoDMarketCalendar
import com.twoDLedger.network.TwoDHolidayItem
import com.twoDLedger.ui.theme.CobaltLight
import com.twoDLedger.ui.theme.CobaltPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TwoDMarketCalendarDialog(
    apiHoliday: TwoDHolidayItem?,
    winningHistoryList: List<WinningHistory> = emptyList(),
    todayWin1200: String = "",
    todayWin1630: String = "",
    todayInd900: String = "",
    todayInd1400: String = "",
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val currentCal = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH) + 1) }

    val todayOverview = remember(apiHoliday) {
        TwoDMarketCalendar.getTodayOverview(apiHoliday)
    }

    // Days in displayed month
    val monthDays = remember(displayedYear, displayedMonth) {
        TwoDMarketCalendar.getMonthDays(displayedYear, displayedMonth)
    }

    // Start day offset for first day of month (1 = Sunday, 2 = Monday, ...)
    val firstDayOffset = remember(displayedYear, displayedMonth) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        c.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    }

    // Currently selected day (defaults to today if in current month, or 1st day)
    val todayDay = currentCal.get(Calendar.DAY_OF_MONTH)
    val isCurrentMonthDisplayed = (displayedYear == currentCal.get(Calendar.YEAR) && displayedMonth == currentCal.get(Calendar.MONTH) + 1)
    var selectedDay by remember(displayedYear, displayedMonth) {
        mutableStateOf(
            if (isCurrentMonthDisplayed) {
                monthDays.find { it.day == todayDay } ?: monthDays.firstOrNull()
            } else {
                monthDays.firstOrNull()
            }
        )
    }

    val monthNamesMm = listOf(
        "ဇန်နဝါရီ", "ဖေဖော်ဝါရီ", "မတ်", "ဧပြီ", "မေ", "ဇွန်",
        "ဇူလိုင်", "သြဂုတ်", "စက်တင်ဘာ", "အောက်တိုဘာ", "နိုဝင်ဘာ", "ဒီဇင်ဘာ"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CobaltLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "2D ဈေးကွက် ပြက္ခဒိန်",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "တရားဝင် SET ဈေးကွက်နှင့် ထွက်ဂဏန်းများ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Today Live Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todayOverview.isOpen) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(1.dp, if (todayOverview.isOpen) Color(0xFF86EFAC) else Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (todayOverview.isOpen) Color(0xFF10B981) else Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (todayOverview.isOpen) Icons.Default.Check else Icons.Default.Block,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (todayOverview.isOpen) "ယနေ့ 2D ဈေးကွက် ဖွင့်လှစ်ပါသည်" else "ယနေ့ 2D ဈေးကွက် ပိတ်ပါသည်",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (todayOverview.isOpen) Color(0xFF15803D) else Color(0xFFB91C1C),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = todayOverview.reason,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Month Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (displayedMonth == 1) {
                                displayedMonth = 12
                                displayedYear -= 1
                            } else {
                                displayedMonth -= 1
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month", tint = CobaltPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${displayedYear} ခုနှစ် ${monthNamesMm[displayedMonth - 1]}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "ရက်စွဲကို နှိပ်၍ ထွက်ဂဏန်း ကြည့်ရှုနိုင်ပါသည်",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (displayedMonth == 12) {
                                displayedMonth = 1
                                displayedYear += 1
                            } else {
                                displayedMonth += 1
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = CobaltPrimary)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Weekday Headers: Sun, Mon, Tue, Wed, Thu, Fri, Sat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val weekdayList = listOf(
                        Pair("နွေ", true),
                        Pair("လာ", false),
                        Pair("ဂါ", false),
                        Pair("ဟူး", false),
                        Pair("ကြာ", false),
                        Pair("သော", false),
                        Pair("နေ", true)
                    )
                    weekdayList.forEach { (name, isWk) ->
                        Text(
                            text = name,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWk) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Calendar Grid with Days
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Empty cells before the 1st
                    items(firstDayOffset) {
                        Box(modifier = Modifier.aspectRatio(1.05f))
                    }

                    // Actual month days
                    items(monthDays) { dayInfo ->
                        val isSelected = selectedDay?.day == dayInfo.day
                        val isToday = dayInfo.isToday

                        // Find winning numbers for this cell if any
                        val hist = winningHistoryList.find { it.date == dayInfo.dateFormatted }
                        val cellWinNum = when {
                            hist != null && hist.num1630.isNotBlank() && hist.num1630 != "--" -> hist.num1630
                            hist != null && hist.num1200.isNotBlank() && hist.num1200 != "--" -> hist.num1200
                            isToday && todayWin1630.isNotBlank() && todayWin1630 != "--" -> todayWin1630
                            isToday && todayWin1200.isNotBlank() && todayWin1200 != "--" -> todayWin1200
                            else -> null
                        }

                        val bgColor = when {
                            isSelected -> CobaltPrimary
                            isToday -> CobaltLight.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val textColor = when {
                            isSelected -> Color.White
                            dayInfo.state != DayMarketState.OPEN -> Color(0xFFDC2626)
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        val borderColor = when {
                            isSelected -> CobaltPrimary
                            isToday -> CobaltPrimary
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1.05f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedDay = dayInfo
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "${dayInfo.day}",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.SemiBold,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace
                                )

                                if (cellWinNum != null) {
                                    Text(
                                        text = cellWinNum,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFFFD54F) else CobaltPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                } else {
                                    // Market Status Dot Indicator
                                    val dotColor = when (dayInfo.state) {
                                        DayMarketState.OPEN -> Color(0xFF10B981)
                                        DayMarketState.WEEKEND_CLOSED -> Color(0xFFEF4444)
                                        DayMarketState.HOLIDAY_CLOSED -> Color(0xFFF59E0B)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else dotColor)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Selected Day Details Card (Shows winning number of tapped day) ──
                selectedDay?.let { sel ->
                    val historyMatch = winningHistoryList.find { it.date == sel.dateFormatted }
                    val n12 = historyMatch?.num1200?.takeIf { it.isNotBlank() && it != "--" }
                        ?: if (sel.isToday) todayWin1200.takeIf { it.isNotBlank() && it != "--" } else null
                    val n16 = historyMatch?.num1630?.takeIf { it.isNotBlank() && it != "--" }
                        ?: if (sel.isToday) todayWin1630.takeIf { it.isNotBlank() && it != "--" } else null
                    val n9 = historyMatch?.num900?.takeIf { it.isNotBlank() && it != "--" }
                        ?: if (sel.isToday) todayInd900.takeIf { it.isNotBlank() && it != "--" } else null
                    val n14 = historyMatch?.num1400?.takeIf { it.isNotBlank() && it != "--" }
                        ?: if (sel.isToday) todayInd1400.takeIf { it.isNotBlank() && it != "--" } else null

                    val hasAnyWin = n12 != null || n16 != null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, if (hasAnyWin) CobaltPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Date and State Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${sel.year} ခုနှစ် ${monthNamesMm[sel.month - 1]} ${sel.day} ရက်",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (sel.isToday) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CobaltLight
                                        ) {
                                            Text(
                                                "ယနေ့",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CobaltPrimary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (sel.state == DayMarketState.OPEN) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (sel.state == DayMarketState.OPEN) "ဖွင့်လှစ်သည်" else "ပိတ်ရက်",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel.state == DayMarketState.OPEN) Color(0xFF15803D) else Color(0xFFB91C1C),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            if (sel.state == DayMarketState.OPEN) {
                                if (hasAnyWin) {
                                    // Official Winning Numbers Display
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 12:01 PM Session Box
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFEFF6FF),
                                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "မွန်းတည့် ၁၂:၀၁ ပေါက်သီး",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1E40AF),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = n12 ?: "--",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (n12 != null) CobaltPrimary else Color(0xFF94A3B8)
                                                )
                                                if (historyMatch?.set1200?.isNotBlank() == true) {
                                                    Text(
                                                        text = "SET ${historyMatch.set1200}",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF64748B),
                                                        fontFamily = FontFamily.Monospace,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }

                                        // 4:30 PM Session Box
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFEFF6FF),
                                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "ညနေ ၄:၃၀ ပေါက်သီး",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1E40AF),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = n16 ?: "--",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (n16 != null) CobaltPrimary else Color(0xFF94A3B8)
                                                )
                                                if (historyMatch?.set1630?.isNotBlank() == true) {
                                                    Text(
                                                        text = "SET ${historyMatch.set1630}",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF64748B),
                                                        fontFamily = FontFamily.Monospace,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Early opening indicators if present
                                    if (n9 != null || n14 != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "မနက် ၉:၀၀ အဖွင့်: ${n9 ?: "--"}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "မွန်းလွဲ ၂:၀၀ အဖွင့်: ${n14 ?: "--"}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (sel.isToday) "ယနေ့ ထွက်ဂဏန်း စောင့်ဆိုင်းနေပါသည် (၁၂:၀၁ / ၄:၃၀)" else "ဤနေ့အတွက် ထွက်ဂဏန်း မှတ်တမ်း မရှိသေးပါ",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                // Closed day description
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EventBusy,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = sel.reasonMm,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFDC2626)
                                        )
                                        Text(
                                            text = "ရုံးပိတ်ရက်ဖြစ်သဖြင့် 2D ထွက်ဂဏန်းများ မရှိပါ",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
