package com.twoDLedger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val monthNameEn = remember(displayedMonth) {
        SimpleDateFormat("MMMM", Locale.ENGLISH).format(Calendar.getInstance().apply { set(Calendar.MONTH, displayedMonth - 1) }.time)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CobaltLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "2D ဈေးကွက် ပြက္ခဒိန်",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "တရားဝင် 2D ဈေးကွက် အခြေအနေ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Today Live Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (todayOverview.isOpen) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(1.dp, if (todayOverview.isOpen) Color(0xFF86EFAC) else Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (todayOverview.isOpen) Color(0xFF10B981) else Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (todayOverview.isOpen) Icons.Default.Check else Icons.Default.Block,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (todayOverview.isOpen) "ယနေ့ 2D ဈေးကွက် ဖွင့်လှစ်သည်" else "ယနေ့ 2D ဈေးကွက် ပိတ်ပါသည်",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (todayOverview.isOpen) Color(0xFF15803D) else Color(0xFFB91C1C)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (todayOverview.isOpen) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (todayOverview.isOpen) "ဖွင့်သည်" else "ပိတ်သည်",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (todayOverview.isOpen) Color(0xFF166534) else Color(0xFF991B1B),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = todayOverview.reason,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

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
                            text = "ဈေးကွက် ပြက္ခဒိန်",
                            fontSize = 11.sp,
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

                Spacer(Modifier.height(6.dp))

                // Weekday Headers: Sun, Mon, Tue, Wed, Thu, Fri, Sat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val days = listOf("တနင်္ဂနွေ", "တနင်္လာ", "အင်္ဂါ", "ဗုဒ္ဓဟူး", "ကြာသပတေး", "သောကြာ", "စနေ")
                    days.forEachIndexed { idx, d ->
                        val isWk = idx == 0 || idx == 6
                        Text(
                            text = d.take(3), // Sun, Mon shorthand
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWk) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Calendar Grid with Days
                val totalCells = firstDayOffset + monthDays.size
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Empty cells before the 1st
                    items(firstDayOffset) {
                        Box(modifier = Modifier.aspectRatio(1.1f))
                    }

                    // Actual month days
                    items(monthDays) { dayInfo ->
                        val isSelected = selectedDay?.day == dayInfo.day
                        val isToday = dayInfo.isToday

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
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1.1f)
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
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${dayInfo.day}",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.SemiBold,
                                    color = textColor,
                                    fontFamily = FontFamily.Monospace
                                )

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

                // Selected Day Details Card
                selectedDay?.let { sel ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${sel.year} ခုနှစ် ${monthNamesMm[sel.month - 1]} ${sel.day} ရက်",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
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
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (sel.state == DayMarketState.OPEN) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (sel.state == DayMarketState.OPEN) "🟢 ဖွင့်လှစ်သည်" else "🔴 ပိတ်ရက်",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel.state == DayMarketState.OPEN) Color(0xFF15803D) else Color(0xFFB91C1C),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = sel.reasonMm,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // If this date has winning history recorded, display it!
                            val historyMatch = winningHistoryList.find { it.date == sel.dateFormatted }
                            if (historyMatch != null) {
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ထွက်ဂဏန်းများ:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("၁၂:၀၁ -> ${historyMatch.num1200.ifBlank { "--" }}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CobaltPrimary, fontFamily = FontFamily.Monospace)
                                        Text("၄:၃၀ -> ${historyMatch.num1630.ifBlank { "--" }}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CobaltPrimary, fontFamily = FontFamily.Monospace)
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
