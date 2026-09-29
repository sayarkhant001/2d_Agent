package com.twoDLedger.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twoDLedger.data.WinningHistory
import com.twoDLedger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinningHistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val historyList by viewModel.winningHistory.collectAsStateWithLifecycle()
    val isFetching by viewModel.isFetchingHistory.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (historyList.isEmpty()) {
            viewModel.fetch30DayHistory()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ရလဒ်မှတ်တမ်း (၃၀ ရက်)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "လွန်ခဲ့သော ရက် ၃၀ အတွင်း 2D ထွက်ဂဏန်း မှတ်တမ်း",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.fetch30DayHistory() },
                        enabled = !isFetching
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (historyList.isEmpty() && isFetching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CobaltPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "ရလဒ်မှတ်တမ်းများ ရယူနေပါသည်...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ရလဒ်မှတ်တမ်း မရှိသေးပါ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.fetch30DayHistory() }) {
                            Text("ပြန်လည် ရယူမည်")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(historyList, key = { it.date }) { record ->
                        HistoryDayCard(record)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDayCard(record: WinningHistory) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val isToday = record.date == todayStr

    val dowMm = remember(record.date) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(record.date)
            if (d != null) {
                val cal = Calendar.getInstance().apply { time = d }
                when (cal.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SUNDAY -> "တနင်္ဂနွေ"
                    Calendar.MONDAY -> "တနင်္လာ"
                    Calendar.TUESDAY -> "အင်္ဂါ"
                    Calendar.WEDNESDAY -> "ဗုဒ္ဓဟူး"
                    Calendar.THURSDAY -> "ကြာသပတေး"
                    Calendar.FRIDAY -> "သောကြာ"
                    Calendar.SATURDAY -> "စနေ"
                    else -> ""
                }
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Date + Day of week + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CobaltPrimary)
                    )
                    Text(
                        text = record.date,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (dowMm.isNotBlank()) {
                        Text(
                            text = "($dowMm)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isToday) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CobaltLight
                        ) {
                            Text(
                                text = "ယနေ့",
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
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        text = "၂ ကြိမ် ထွက်",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CobaltPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4 Indicator Row: 9:00 AM, 12:00 PM (Official), 2:00 PM, 4:30 PM (Official)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 9:00 AM Modern Opening
                IndicatorBox(
                    modifier = Modifier.weight(1f),
                    title = "9:00 AM",
                    subtitle = "အဖွင့်",
                    number = record.num900.ifBlank { "--" },
                    isOfficialWinner = false
                )

                // 12:00 PM Official Winner
                IndicatorBox(
                    modifier = Modifier.weight(1.2f),
                    title = "12:00 PM",
                    subtitle = "ပေါက်ဂဏန်း",
                    number = record.num1200.ifBlank { "--" },
                    isOfficialWinner = true,
                    setDetail = if (record.set1200.isNotBlank()) "SET ${record.set1200}" else null
                )

                // 2:00 PM Modern Opening
                IndicatorBox(
                    modifier = Modifier.weight(1f),
                    title = "2:00 PM",
                    subtitle = "အဖွင့်",
                    number = record.num1400.ifBlank { "--" },
                    isOfficialWinner = false
                )

                // 4:30 PM Official Winner
                IndicatorBox(
                    modifier = Modifier.weight(1.2f),
                    title = "4:30 PM",
                    subtitle = "ပေါက်ဂဏန်း",
                    number = record.num1630.ifBlank { "--" },
                    isOfficialWinner = true,
                    setDetail = if (record.set1630.isNotBlank()) "SET ${record.set1630}" else null
                )
            }
        }
    }
}

@Composable
fun IndicatorBox(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    number: String,
    isOfficialWinner: Boolean,
    setDetail: String? = null
) {
    val bgColor = if (isOfficialWinner) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
    val borderColor = if (isOfficialWinner) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
    val titleColor = if (isOfficialWinner) Color(0xFF1E40AF) else Color(0xFF334155)
    val numberColor = if (isOfficialWinner) CobaltPrimary else Color(0xFF0F172A)
    val subtitleColor = if (isOfficialWinner) Color(0xFF1D4ED8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = number,
                fontWeight = FontWeight.Black,
                fontSize = if (isOfficialWinner) 22.sp else 18.sp,
                fontFamily = FontFamily.Monospace,
                color = numberColor,
                maxLines = 1,
                softWrap = false
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                fontWeight = if (isOfficialWinner) FontWeight.Bold else FontWeight.Medium,
                color = subtitleColor,
                maxLines = 1,
                softWrap = false
            )
            if (setDetail != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = setDetail,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
