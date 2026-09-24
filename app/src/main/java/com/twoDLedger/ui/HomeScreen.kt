package com.twoDLedger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twoDLedger.R
import com.twoDLedger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColors: List<Color>,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToCustomers: () -> Unit,
    onNavigateToBetting: () -> Unit,
    onNavigateToWinner: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToVouchers: () -> Unit,
    onNavigateToReceipt: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToOverflow: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val currentDate = dateFormat.format(Date())
    val currentBatch by viewModel.currentBatch.collectAsStateWithLifecycle()
    val bannedNumbers by viewModel.bannedNumbers.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val rDimens = rememberResponsiveDimens()
    val context = androidx.compose.ui.platform.LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            android.widget.Toast.makeText(context, "အက်ပ်မှ ထွက်ရန် နောက်သို့ ထပ်နှိပ်ပါ", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Strictly ordered: 4 Core Modules with cohesive, elegant FinTech accents
    val menuItems = listOf(
        MenuItem(
            title = "ကော်မရှင်",
            subtitle = "စာရင်းသွင်းသူများ",
            icon = Icons.Default.People,
            iconColors = listOf(Color(0xFF059669), Color(0xFF047857)),
            onClick = onNavigateToCustomers
        ),
        MenuItem(
            title = "ဂဏန်းများ",
            subtitle = "ပေါက်/တွတ် စစ်ဆေးချက်",
            icon = Icons.AutoMirrored.Filled.List,
            iconColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
            onClick = onNavigateToLedger
        ),
        MenuItem(
            title = "ဘောင်ချာ",
            subtitle = "ရောင်းရငွေ ဘောင်ချာများ",
            icon = Icons.Default.Receipt,
            iconColors = listOf(Color(0xFFD97706), Color(0xFFB45309)),
            onClick = onNavigateToVouchers
        ),
        MenuItem(
            title = "တင်ကွက်များ",
            subtitle = "အထက်ဒိုင် တင်ကွက်",
            icon = Icons.Default.Payment,
            iconColors = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
            onClick = onNavigateToOverflow
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "2D စာရင်း Logo",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "2D စာရင်း",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    letterSpacing = 0.3.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToWinner()
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.8f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "ပေါက်ဂဏန်း",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ပေါက်ဂဏန်း",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .padding(padding)
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // ── 2D Session Selector: 12:00 PM vs 4:30 PM ─────────────────
                val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 12:00 PM (Noon)
                        val isNoon = currentSession == "12:00 PM"
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setSession("12:00 PM")
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNoon) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "☀️ နေ့လယ် ၁၂:၀၀",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isNoon) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isNoon) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 4:30 PM (Evening)
                        val isEvening = currentSession == "4:30 PM"
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setSession("4:30 PM")
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isEvening) EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌙 ညနေ ၄:၃၀",
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isEvening) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isEvening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Live Thailand SET Index & 2D Ticker ───────────────────────
                val liveData by viewModel.live2DData.collectAsStateWithLifecycle()
                val n9 by viewModel.indicator900.collectAsStateWithLifecycle()
                val n12 by viewModel.winningNumber1200.collectAsStateWithLifecycle()
                val n14 by viewModel.indicator1400.collectAsStateWithLifecycle()
                val n16 by viewModel.winningNumber1630.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.fetchLive2D()
                }

                Card(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.fetchLive2D()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                Text(
                                    text = "🇹🇭 Thai Stock (SET) Live",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (liveData != null && liveData!!.twod.isNotBlank()) {
                                Text(
                                    text = "Live: ${liveData!!.twod}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LiveMiniSlot(modifier = Modifier.weight(1f), time = "9:00", value = n9.ifBlank { "--" }, isWin = false)
                            LiveMiniSlot(modifier = Modifier.weight(1.1f), time = "12:00 🏆", value = n12.ifBlank { "--" }, isWin = true)
                            LiveMiniSlot(modifier = Modifier.weight(1f), time = "2:00", value = n14.ifBlank { "--" }, isWin = false)
                            LiveMiniSlot(modifier = Modifier.weight(1.1f), time = "4:30 🏆", value = n16.ifBlank { "--" }, isWin = true)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── 2D Weekday & Fast Recheck / Calculation Header Card ─────────
                val myanmarDayOfWeek = remember {
                    val cal = java.util.Calendar.getInstance()
                    when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                        java.util.Calendar.MONDAY -> "တနင်္လာနေ့"
                        java.util.Calendar.TUESDAY -> "အင်္ဂါနေ့"
                        java.util.Calendar.WEDNESDAY -> "ဗုဒ္ဓဟူးနေ့"
                        java.util.Calendar.THURSDAY -> "ကြာသပတေးနေ့"
                        java.util.Calendar.FRIDAY -> "သောကြာနေ့"
                        java.util.Calendar.SATURDAY -> "စနေနေ့ (ပိတ်ရက်)"
                        java.util.Calendar.SUNDAY -> "တနင်္ဂနွေနေ့ (ပိတ်ရက်)"
                        else -> "ရုံးဖွင့်ရက်"
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = myanmarDayOfWeek,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = CobaltPrimary,
                                    fontSize = 16.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentSession == "12:00 PM") Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                                ) {
                                    Text(
                                        text = if (currentSession == "12:00 PM") "နေ့လယ်ပိုင်း စာရင်း" else "ညနေပိုင်း စာရင်း",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (currentSession == "12:00 PM") Color(0xFF92400E) else Color(0xFF1E40AF),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ရုံးဖွင့်ရက် (တနင်္လာ - သောကြာ) ၂ ကြိမ် စာရင်းတွက်ချက်မှု",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        // Fast Recheck Action Button
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigateToLedger()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = CobaltLight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "📊 ပြန်စစ်",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CobaltPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Hero Action Card: "ထိုးကြေး စာရင်းသွင်းမည်" (Direct Betting Entry) ────
                Card(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToBetting()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF1D4ED8),
                                        Color(0xFF2563EB)
                                    )
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "ထိုးကြေး စာရင်းသွင်းမည်",
                                        fontWeight = FontWeight.Black,
                                        fontSize = rDimens.responsiveSp(16f),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "ကီးပက်ဖြင့် အမြန် စာရင်းသွင်းရန် နှိပ်ပါ",
                                        fontSize = rDimens.responsiveSp(11.5f),
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFFFFD93D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (bannedNumbers.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Alert", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ပိတ်ထားသော ဂဏန်းများ",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bannedNumbers.joinToString(", ") {
                                        if (it.amountLimit > 0) "${it.number} (≤%,d Ks)".format(it.amountLimit)
                                        else "${it.number} (လုံးဝပိတ်)"
                                    },
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── 2x2 Grid Menu with Tactile Medallion Cards ─────────────────
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(menuItems) { item ->
                        MenuCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = item.icon,
                            iconColors = item.iconColors,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                item.onClick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Settings & Preferences Navigation Card ───────────────────
                Card(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToSettings()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "ဆက်တင်နှင့် အချက်အလက်",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "လိုင်စင်၊ စကားဝှက်၊ အရန်သိမ်းဆည်းမှု စီမံရန်",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ── Tactile Pro Medallion Menu Card ──────────────────────────────────────────
@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColors: List<Color>,
    onClick: () -> Unit
) {
    val rDimens = rememberResponsiveDimens()
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (rDimens.isCompact) 112.dp else 124.dp)
            .shadow(2.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(rDimens.responsiveDp(14f)),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Gradient Icon Medallion
            Box(
                modifier = Modifier
                    .size(if (rDimens.isCompact) 40.dp else 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colors = iconColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(if (rDimens.isCompact) 20.dp else 22.dp),
                    tint = Color.White
                )
            }

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rDimens.responsiveSp(15.5f),
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = rDimens.responsiveSp(11f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



@Composable
fun LiveMiniSlot(modifier: Modifier = Modifier, time: String, value: String, isWin: Boolean) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isWin) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, fontSize = 9.sp, fontWeight = if (isWin) FontWeight.Bold else FontWeight.Normal, color = if (isWin) Color(0xFF92400E) else MaterialTheme.colorScheme.outline)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = if (isWin) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface)
        }
    }
}
