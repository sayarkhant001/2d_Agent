package com.twoDLedger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twoDLedger.R
import com.twoDLedger.logic.LicenseManager
import com.twoDLedger.logic.TwoDMarketCalendar
import com.twoDLedger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GridMenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColors: List<Color>,
    val isLocked: Boolean = false,
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
    onNavigateToSettings: () -> Unit,
    onNavigateToResult: (Int) -> Unit = {}
) {
    val todayDayOfWeek = remember { TwoDMarketCalendar.getBurmeseDayOfWeek(Date()) }
    val currentDateStr = remember {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        "$d ($todayDayOfWeek)"
    }
    val currentBatch by viewModel.currentBatch.collectAsStateWithLifecycle()
    val maxBatch by viewModel.maxBatch.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val bannedNumbers by viewModel.bannedNumbers.collectAsStateWithLifecycle()
    val liveHoliday by viewModel.liveHoliday.collectAsStateWithLifecycle()
    val winningHistory by viewModel.winningHistory.collectAsStateWithLifecycle()
    val vouchersWithBets by viewModel.vouchersWithBets.collectAsStateWithLifecycle()
    val allExportRecords by viewModel.allExportRecords.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val winningNumber by viewModel.winningNumber.collectAsStateWithLifecycle()
    val win1200 by viewModel.winningNumber1200.collectAsStateWithLifecycle()
    val win1630 by viewModel.winningNumber1630.collectAsStateWithLifecycle()
    val ind900 by viewModel.indicator900.collectAsStateWithLifecycle()
    val ind1400 by viewModel.indicator1400.collectAsStateWithLifecycle()

    var showMarketCalendarDialog by remember { mutableStateOf(false) }
    var showBatchDropdown by remember { mutableStateOf(false) }
    val todayMarket = remember(liveHoliday) { TwoDMarketCalendar.getTodayOverview(liveHoliday) }
    val haptic = LocalHapticFeedback.current
    val rDimens = rememberResponsiveDimens()
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager(context) }
    var showLicenseDetailsDialog by remember { mutableStateOf(false) }
    var licenseDetails by remember { mutableStateOf(licenseManager.getLicenseDetails()) }
    var isBatchSummaryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        licenseManager.syncServerTime()
        licenseDetails = licenseManager.getLicenseDetails()
        viewModel.fetchLive2D()
        viewModel.fetch30DayHistory()
    }

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

    // Dynamic Financial Summary for the currently displayed batch (Modified Order)
    val stats = remember(currentBatch, vouchersWithBets, allExportRecords, customers, winningNumber) {
        viewModel.getBatchFinancialSummary(currentBatch)
    }

    val isBatchLocked = stats.isDeclared

    // All available batch numbers
    val availableBatches = remember(currentBatch, maxBatch, vouchersWithBets, allExportRecords) {
        viewModel.getAllBatchNumbers()
    }

    // Strictly ordered: 4 Core Modules with cohesive, elegant FinTech accents
    val menuItems = listOf(
        GridMenuItem(
            id = "customers",
            title = "ကော်မရှင်",
            subtitle = "စာရင်းသွင်းသူများ",
            icon = Icons.Default.People,
            iconColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
            onClick = onNavigateToCustomers
        ),
        GridMenuItem(
            id = "ledger",
            title = "ဂဏန်းများ",
            subtitle = "ပေါက်ဂဏန်း စစ်ဆေးချက်",
            icon = Icons.AutoMirrored.Filled.List,
            iconColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
            onClick = onNavigateToLedger
        ),
        GridMenuItem(
            id = "vouchers",
            title = "ဘောင်ချာ",
            subtitle = "ရောင်းရငွေ ဘောင်ချာများ",
            icon = Icons.Default.Receipt,
            iconColors = listOf(Color(0xFFD97706), Color(0xFFB45309)),
            onClick = onNavigateToVouchers
        ),
        GridMenuItem(
            id = "overflow",
            title = "တင်ကွက်များ",
            subtitle = "အထက်ဒိုင် တင်ကွက်",
            icon = Icons.Default.Payment,
            iconColors = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
            isLocked = isBatchLocked,
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
                                .border(1.dp, CobaltPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
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
                                LicenseStatusBadge(
                                    licenseDetails = licenseDetails,
                                    onClick = { showLicenseDetailsDialog = true }
                                )
                            }
                            Text(
                                text = "2D ဒိုင်ချုပ် စာရင်းစနစ်",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Calendar Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.fetch30DayHistory()
                            showMarketCalendarDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "2D ပြက္ခဒိန်",
                            tint = CobaltPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Winner Screen Button
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToWinner()
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.8f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ── 1. Date and Batch Number Selection Dropdown Bar ────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Current Date Display
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentDateStr,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Interactive Batch Selector Dropdown Pill
                        Box {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showBatchDropdown = true
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = CobaltLight,
                                border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "အကြိမ် $currentBatch",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CobaltPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = CobaltPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showBatchDropdown,
                                onDismissRequest = { showBatchDropdown = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                availableBatches.forEach { b ->
                                    val win = viewModel.getWinningNumberForBatch(b)
                                    val isDecl = win.length == 2
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = "အကြိမ် #$b",
                                                    fontWeight = if (b == currentBatch) FontWeight.Black else FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    color = if (b == currentBatch) CobaltPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isDecl) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFFEE2E2)
                                                    ) {
                                                        Text(
                                                            text = "ပေါက်: $win",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFB91C1C),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFDCFCE7)
                                                    ) {
                                                        Text(
                                                            text = "ဖွင့်လှစ်ဆဲ",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF15803D),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        trailingIcon = {
                                            if (b == currentBatch) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = CobaltPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.selectBatch(b)
                                            showBatchDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── 2. Batch Summary Overview (Collapsible breakdown) ──────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Section Header: Batch Indicator & Declaration Tag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isBatchSummaryExpanded = !isBatchSummaryExpanded }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "အကြိမ် #${currentBatch} ရှင်းတမ်း အနှစ်ချုပ်",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CobaltPrimary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    if (isBatchSummaryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isBatchSummaryExpanded) "Collapse" else "Expand",
                                    tint = CobaltPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (stats.isDeclared) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEE2E2),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Text(
                                            text = "ပေါက်: ${stats.winningNumber}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDCFCE7),
                                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                    ) {
                                        Text(
                                            text = "ဖွင့်လှစ်ဆဲ",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Collapsible Stats Grid Content (Image 3 Breakdown: Left Customers, Right Financials)
                        AnimatedVisibility(
                            visible = isBatchSummaryExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Left Column: Customer Breakdown (အားလုံး, ကော်မရှင်, ထိုးသား)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            SummaryStatRow(
                                                icon = Icons.Default.Groups,
                                                iconTint = Color(0xFF0284C7),
                                                label = "အားလုံး",
                                                value = "${stats.customerCount} ယောက်"
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            SummaryStatRow(
                                                icon = Icons.Default.Handshake,
                                                iconTint = Color(0xFF2563EB),
                                                label = "ကော်မရှင်",
                                                value = "${stats.commissionCustomerCount} ယောက်"
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            SummaryStatRow(
                                                icon = Icons.Default.Person,
                                                iconTint = Color(0xFF0D9488),
                                                label = "ထိုးသား",
                                                value = "${stats.directBettorCount} ယောက်"
                                            )
                                        }

                                        // Vertical separator line
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        )

                                        // Right Column: Financial Figures (အရောင်းကြေး, ကော်မရှင်ခ, ကျန်ရှိငွေ)
                                        Column(
                                            modifier = Modifier.weight(1.3f),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            SummaryStatRow(
                                                icon = Icons.Default.AccountBalanceWallet,
                                                iconTint = CobaltPrimary,
                                                label = "အရောင်း",
                                                value = "%,d Ks".format(stats.totalSales)
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            SummaryStatRow(
                                                icon = Icons.Default.Percent,
                                                iconTint = Color(0xFFD97706),
                                                label = "ကော်မရှင်",
                                                value = "%,d Ks".format(stats.commissionAmount)
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            SummaryStatRow(
                                                icon = Icons.Default.AccountBalance,
                                                iconTint = Color(0xFF059669),
                                                label = "ကျန်ငွေ",
                                                value = "%,d Ks".format(stats.netBalance)
                                            )
                                            if (stats.isDeclared) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                SummaryStatRow(
                                                    icon = Icons.Default.EmojiEvents,
                                                    iconTint = Color(0xFFDC2626),
                                                    label = "လျော်ငွေ",
                                                    value = "%,d Ks".format(stats.winningPayout)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Hero Action Card: "ထိုးကြေး စာရင်းသွင်းမည်" (Direct Betting Entry) ────
                val heroInteractionSource = remember { MutableInteractionSource() }
                val heroIsPressed by heroInteractionSource.collectIsPressedAsState()
                val heroScale by animateFloatAsState(
                    targetValue = if (heroIsPressed) 0.96f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "heroScale"
                )

                Card(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToBetting()
                    },
                    interactionSource = heroInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = heroScale
                            scaleY = heroScale
                        }
                        .shadow(if (heroIsPressed) 2.dp else 6.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CobaltPrimary)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF1D4ED8),
                                        Color(0xFF0284C7)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "ထိုးကြေး စာရင်းသွင်းမည်",
                                            fontWeight = FontWeight.Black,
                                            fontSize = rDimens.responsiveSp(16f),
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isBatchLocked) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFEE2E2),
                                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(11.dp))
                                                    Text(
                                                        "ပိတ်ပါပြီ",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB91C1C)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        if (isBatchLocked) "ပေါက်သီးထွက်ပြီးပါပြီ (စာရင်း ပိတ်ထားသည်)" else "ကီးပက်ဖြင့် အမြန် စာရင်းသွင်းရန် နှိပ်ပါ",
                                        fontSize = rDimens.responsiveSp(11.5f),
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        softWrap = false,
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

                if (bannedNumbers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "ပိတ်ဂဏန်း / ကန့်သတ်ဂဏန်းများ (${bannedNumbers.size})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(bannedNumbers) { bn ->
                                    val isFullBan = bn.amountLimit <= 0
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isFullBan) Color(0xFFFFE4E6) else Color(0xFFFEF3C7),
                                        border = BorderStroke(0.5.dp, if (isFullBan) Color(0xFFFDA4AF) else Color(0xFFFDE68A))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = bn.number,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isFullBan) Color(0xFFE11D48) else Color(0xFFB45309)
                                            )
                                            Text(
                                                text = if (isFullBan) "(ပိတ်)" else "(≤ %,d)".format(bn.amountLimit),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isFullBan) Color(0xFFBE123C) else Color(0xFF92400E)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 2x2 Grid Menu with 4 Core Action Cards ─────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val chunkedItems = menuItems.chunked(2)
                    chunkedItems.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MenuCard(
                                        title = item.title,
                                        subtitle = item.subtitle,
                                        icon = item.icon,
                                        iconColors = item.iconColors,
                                        isLocked = item.isLocked,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            item.onClick()
                                        }
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Settings & Preferences Navigation Card ───────────────────
                val settingsInteractionSource = remember { MutableInteractionSource() }
                val settingsIsPressed by settingsInteractionSource.collectIsPressedAsState()
                val settingsScale by animateFloatAsState(
                    targetValue = if (settingsIsPressed) 0.96f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "settingsScale"
                )

                Card(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToSettings()
                    },
                    interactionSource = settingsInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = settingsScale
                            scaleY = settingsScale
                        }
                        .shadow(if (settingsIsPressed) 1.dp else 2.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showMarketCalendarDialog) {
            TwoDMarketCalendarDialog(
                apiHoliday = liveHoliday,
                winningHistoryList = winningHistory,
                todayWin1200 = win1200,
                todayWin1630 = win1630,
                todayInd900 = ind900,
                todayInd1400 = ind1400,
                onDismissRequest = { showMarketCalendarDialog = false }
            )
        }

        if (showLicenseDetailsDialog) {
            LicenseDetailsDialog(
                licenseManager = licenseManager,
                onDismiss = {
                    showLicenseDetailsDialog = false
                    licenseDetails = licenseManager.getLicenseDetails()
                }
            )
        }

        if (licenseDetails.isClockTampered) {
            ClockTamperedBlockDialog(
                licenseManager = licenseManager,
                onRestored = {
                    licenseDetails = licenseManager.getLicenseDetails()
                }
            )
        }
    }
}

// ── Financial Stat Item Component ────────────────────────────────────────────
@Composable
fun BatchStatItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = value,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Tactile Pro Medallion Menu Card with Lock Status ───────────────────────────
@Composable
fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColors: List<Color>,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val rDimens = rememberResponsiveDimens()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "menuCardScale"
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (rDimens.isCompact) 104.dp else 114.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (isPressed) 1.dp else 3.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (isLocked) Color(0xFFFCA5A5).copy(alpha = 0.8f)
            else if (isPressed) CobaltPrimary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(rDimens.responsiveDp(13f)),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient Icon Medallion
                Box(
                    modifier = Modifier
                        .size(if (rDimens.isCompact) 36.dp else 40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Brush.linearGradient(colors = if (isLocked) listOf(Color(0xFF94A3B8), Color(0xFF64748B)) else iconColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(if (rDimens.isCompact) 18.dp else 20.dp),
                        tint = Color.White
                    )
                }

                if (isLocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(11.dp))
                            Text(
                                text = "ပိတ်ပါပြီ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = rDimens.responsiveSp(15.sp.value),
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = rDimens.responsiveSp(11.sp.value),
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
fun SummaryStatRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false
        )
    }
}
