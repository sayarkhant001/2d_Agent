package com.twoDLedger.ui

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
                            text = "Past 30 Days 2D Winning History",
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
                        CircularProgressIndicator(color = EmeraldPrimary)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date
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
                            .background(EmeraldPrimary)
                    )
                    Text(
                        text = record.date,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "၂ ကြိမ် ထွက်",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4 Indicator Row: 9:00 AM, 12:00 PM (Win), 2:00 PM, 4:30 PM (Win)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 9:00 AM Modern Indicator
                IndicatorBox(
                    modifier = Modifier.weight(1f),
                    title = "9:00 AM",
                    subtitle = "အင်တာနက်",
                    number = record.num900.ifBlank { "--" },
                    isOfficialWinner = false
                )

                // 12:00 PM Official Winner 🏆
                IndicatorBox(
                    modifier = Modifier.weight(1.2f),
                    title = "12:00 PM",
                    subtitle = "ပေါက်ဂဏန်း 🏆",
                    number = record.num1200.ifBlank { "--" },
                    isOfficialWinner = true,
                    setDetail = if (record.set1200.isNotBlank()) "SET: ${record.set1200}" else null
                )

                // 2:00 PM Modern Indicator
                IndicatorBox(
                    modifier = Modifier.weight(1f),
                    title = "2:00 PM",
                    subtitle = "အင်တာနက်",
                    number = record.num1400.ifBlank { "--" },
                    isOfficialWinner = false
                )

                // 4:30 PM Official Winner 🏆
                IndicatorBox(
                    modifier = Modifier.weight(1.2f),
                    title = "4:30 PM",
                    subtitle = "ပေါက်ဂဏန်း 🏆",
                    number = record.num1630.ifBlank { "--" },
                    isOfficialWinner = true,
                    setDetail = if (record.set1630.isNotBlank()) "SET: ${record.set1630}" else null
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
    val bgColor = if (isOfficialWinner) {
        Color(0xFFFEF3C7).copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val borderColor = if (isOfficialWinner) {
        Color(0xFFF59E0B).copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }
    val numberColor = if (isOfficialWinner) {
        Color(0xFFB45309)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOfficialWinner) Color(0xFF92400E) else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = number,
                fontWeight = FontWeight.Black,
                fontSize = if (isOfficialWinner) 22.sp else 18.sp,
                fontFamily = FontFamily.Monospace,
                color = numberColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                fontWeight = if (isOfficialWinner) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isOfficialWinner) Color(0xFFB45309) else MaterialTheme.colorScheme.outline
            )
            if (setDetail != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = setDetail,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
