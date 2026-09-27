package com.twoDLedger.ui

import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.activity.compose.BackHandler
import com.twoDLedger.ui.theme.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class WinnerResult(
    val customerName: String,
    val customerId: Int,
    val voucherId: Int,
    val betNumber: String,
    val betAmount: Int,
    val payoutAmount: Double,
    val session: String
)

data class OverflowWinResult(
    val exportRecordId: Int,
    val type: String,
    val number: String,
    val amount: Int,
    val payoutAmount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinnerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val currentBatch = viewModel.currentBatch.collectAsStateWithLifecycle().value
    var targetBatch by remember { mutableStateOf(currentBatch.toString()) }
    var selectedSession by remember { mutableStateOf(viewModel.currentSession.value) }
    var winningNumber by remember { mutableStateOf("") }
    var multiplierText by remember { mutableStateOf("80") }

    val liveData by viewModel.live2DData.collectAsStateWithLifecycle()
    val isFetchingLive by viewModel.isFetchingLive.collectAsStateWithLifecycle()
    val ind900 by viewModel.indicator900.collectAsStateWithLifecycle()
    val win1200 by viewModel.winningNumber1200.collectAsStateWithLifecycle()
    val ind1400 by viewModel.indicator1400.collectAsStateWithLifecycle()
    val win1630 by viewModel.winningNumber1630.collectAsStateWithLifecycle()

    val allBets by viewModel.allBets.collectAsStateWithLifecycle()
    val allVWB by viewModel.vouchersWithBets.collectAsStateWithLifecycle()
    val allCustomers by viewModel.customers.collectAsStateWithLifecycle()
    val allExportRecords by viewModel.allExportRecords.collectAsStateWithLifecycle()
    val allDines by viewModel.allDines.collectAsStateWithLifecycle()

    val targetBatchInt = targetBatch.toIntOrNull() ?: currentBatch
    val dineSettlements = remember(allExportRecords, winningNumber, targetBatchInt, allDines) {
        viewModel.getDineSettlementsForBatch(targetBatchInt)
    }

    var isDeclared by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var results by remember { mutableStateOf<List<WinnerResult>>(emptyList()) }
    var overflowResults by remember { mutableStateOf<List<OverflowWinResult>>(emptyList()) }

    BackHandler {
        if (showClearDialog) {
            showClearDialog = false
        } else {
            onNavigateBack()
        }
    }

    fun runCalculation(num: String, multStr: String, session: String) {
        val cleanNum = num.trim()
        if (cleanNum.length != 2) return
        val batchInt = targetBatch.toIntOrNull() ?: currentBatch
        val mult = multStr.toDoubleOrNull() ?: 80.0

        results = allBets.mapNotNull { bet ->
            val vWB = allVWB.find { it.voucher.id == bet.voucherId } ?: return@mapNotNull null
            if (vWB.voucher.batchNumber != batchInt) return@mapNotNull null
            if (vWB.voucher.session.isNotBlank() && vWB.voucher.session != session) return@mapNotNull null

            val customer = allCustomers.find { it.id == vWB.voucher.customerId } ?: return@mapNotNull null
            if (customer.name.contains("တင်ကွက်") || customer.name.contains("overflow", ignoreCase = true) ||
                customer.name.contains("upper", ignoreCase = true) || customer.name.contains("အထက်ဒိုင်") ||
                vWB.voucher.remark.contains("တင်ကွက်") || vWB.voucher.remark.contains("overflow", ignoreCase = true)) {
                return@mapNotNull null
            }

            if (bet.number == cleanNum) {
                WinnerResult(
                    customerName = customer.name,
                    customerId = customer.id,
                    voucherId = bet.voucherId,
                    betNumber = bet.number,
                    betAmount = bet.amount,
                    payoutAmount = bet.amount * mult,
                    session = vWB.voucher.session
                )
            } else null
        }.sortedWith(compareBy({ it.customerName }, { it.voucherId }))

        overflowResults = allExportRecords
            .filter { it.record.batchNumber == batchInt }
            .flatMap { exp ->
                exp.numbers.mapNotNull { en ->
                    if (en.number == cleanNum) {
                        OverflowWinResult(
                            exportRecordId = exp.record.id,
                            type = exp.record.type,
                            number = en.number,
                            amount = en.amount,
                            payoutAmount = en.amount * mult
                        )
                    } else null
                }
            }

        viewModel.saveWinningNumber(cleanNum, session, batchInt)
        viewModel.saveMultipliers(mult, mult, mult, batchInt)
        isDeclared = true
    }

    LaunchedEffect(targetBatch, selectedSession) {
        val b = targetBatch.toIntOrNull() ?: currentBatch
        val saved = viewModel.getWinningNumberForBatch(b)
        val (eM, _, _) = viewModel.getMultipliersForBatch(b)
        multiplierText = eM.toInt().toString()
        if (saved.length == 2) {
            winningNumber = saved
            isDeclared = true
            runCalculation(saved, multiplierText, selectedSession)
        } else {
            val sessionLive = if (selectedSession == "12:00 PM") win1200 else win1630
            if (sessionLive.isNotBlank() && sessionLive.length == 2) {
                winningNumber = sessionLive
            } else {
                winningNumber = ""
                isDeclared = false
                results = emptyList()
                overflowResults = emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ပေါက်သီး စာရင်း (2D)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "2D ပေါက်ဂဏန်း • $selectedSession",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "နောက်သို့", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onNavigateToHistory,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = CobaltPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ရလဒ် ၃၀ ရက်", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = { viewModel.fetchLive2D() },
                        enabled = !isFetchingLive
                    ) {
                        if (isFetchingLive) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CobaltPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "တိုက်ရိုက် အချက်အလက် ရယူရန်", tint = CobaltPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val infiniteTransition = rememberInfiniteTransition(label = "winnerPulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.85f,
                            targetValue = 1.30f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "winnerPulseScale"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.45f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "winnerPulseAlpha"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .graphicsLayer {
                                            scaleX = pulseScale
                                            scaleY = pulseScale
                                            alpha = pulseAlpha
                                        }
                                        .clip(CircleShape)
                                        .background(CobaltPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ထိုင်း 2D တိုက်ရိုက်",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                            liveData?.let {
                                Text(
                                    text = "အညွှန်း: ${it.set} | တန်ဖိုး: ${it.value}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SlotCard(
                                modifier = Modifier.weight(1f),
                                time = "နံနက် ၉:၀၀",
                                label = "အဖွင့်",
                                number = ind900.ifBlank { "--" },
                                isOfficial = false,
                                onSelect = {}
                            )

                            SlotCard(
                                modifier = Modifier.weight(1f),
                                time = "မွန်းတည့် ၁၂:၀၀",
                                label = "ပေါက်သီး ★",
                                number = win1200.ifBlank { "--" },
                                isOfficial = true,
                                onSelect = {
                                    if (win1200.isNotBlank() && win1200 != "--") {
                                        selectedSession = "12:00 PM"
                                        winningNumber = win1200
                                        runCalculation(win1200, multiplierText, "12:00 PM")
                                    }
                                }
                            )

                            SlotCard(
                                modifier = Modifier.weight(1f),
                                time = "မွန်းလွဲ ၂:၀၀",
                                label = "အဖွင့်",
                                number = ind1400.ifBlank { "--" },
                                isOfficial = false,
                                onSelect = {}
                            )

                            SlotCard(
                                modifier = Modifier.weight(1f),
                                time = "ညနေ ၄:၃၀",
                                label = "ပေါက်သီး ★",
                                number = win1630.ifBlank { "--" },
                                isOfficial = true,
                                onSelect = {
                                    if (win1630.isNotBlank() && win1630 != "--") {
                                        selectedSession = "4:30 PM"
                                        winningNumber = win1630
                                        runCalculation(win1630, multiplierText, "4:30 PM")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "* နံနက် ၉:၀၀ နှင့် မွန်းလွဲ ၂:၀၀ မှာ အစောပိုင်း အဖွင့်ဂဏန်းသာဖြစ်ပြီး၊ မွန်းတည့် ၁၂:၀၀ နှင့် ညနေ ၄:၃၀ မှာ တရားဝင်ပေါက်သီး ဖြစ်ပါသည်။",
                            fontSize = 10.sp,
                            color = TextMuted,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("12:00 PM", "4:30 PM").forEach { sess ->
                                val isSel = selectedSession == sess
                                OutlinedButton(
                                    onClick = {
                                        selectedSession = sess
                                        val sLive = if (sess == "12:00 PM") win1200 else win1630
                                        if (sLive.isNotBlank() && sLive.length == 2) {
                                            winningNumber = sLive
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSel) PrimaryGold.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isSel) PrimaryGold else TextSecondary
                                    ),
                                    border = BorderStroke(1.5.dp, if (isSel) PrimaryGold else CardBorder)
                                ) {
                                    Text(
                                        text = if (sess == "12:00 PM") "မနက်ပိုင်း (၁၂:၀၀)" else "ညနေပိုင်း (၄:၃၀)",
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedSession == "12:00 PM") Color(0xFFFEF3C7) else Color(0xFFDBEAFE),
                                modifier = Modifier.weight(1.1f).height(54.dp),
                                onClick = {
                                    selectedSession = if (selectedSession == "12:00 PM") "4:30 PM" else "12:00 PM"
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("အချိန်ပိုင်း", fontSize = 9.5.sp, color = if (selectedSession == "12:00 PM") Color(0xFF92400E) else Color(0xFF1E40AF))
                                    Text(if (selectedSession == "12:00 PM") "၁၂:၀၀" else "၄:၃၀", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (selectedSession == "12:00 PM") Color(0xFF92400E) else Color(0xFF1E40AF))
                                }
                            }

                            OutlinedTextField(
                                value = winningNumber,
                                onValueChange = {
                                    if (it.length <= 2) winningNumber = it.filter { c -> c.isDigit() }
                                },
                                label = { Text("ပေါက်ဂဏန်း (2D)", fontSize = 11.sp) },
                                placeholder = { Text("00-99", fontSize = 12.sp, color = TextMuted) },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = multiplierText,
                                onValueChange = { multiplierText = it.filter { c -> c.isDigit() } },
                                label = { Text("အဆ", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGold,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    runCalculation(winningNumber, multiplierText, selectedSession)
                                },
                                modifier = Modifier.weight(1.5f),
                                enabled = winningNumber.length == 2,
                                colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ပေါက်သီးတွက်ချက်မည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            if (isDeclared) {
                                OutlinedButton(
                                    onClick = { showClearDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                                    border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ပြန်ဖျက်", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (isDeclared && winningNumber.length == 2) {
                val totalAgentPayout = results.sumOf { it.payoutAmount }
                val totalOverflowPayout = overflowResults.sumOf { it.payoutAmount }
                val netDeductiblePayout = totalAgentPayout - totalOverflowPayout

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ပေါက်ဂဏန်း: $winningNumber (${selectedSession})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CobaltPrimary
                                    )
                                    Text(
                                        text = "စုစုပေါင်း ပေါက်ကွက် ${results.size} ကွက်",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "လျော်ကြေးစုစုပေါင်း",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${String.format("%,.0f", totalAgentPayout)} ကျပ်",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF5252)
                                    )
                                }
                            }

                            if (totalOverflowPayout > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "အထက်ဒိုင် ပြန်ရငွေ: ${String.format("%,.0f", totalOverflowPayout)} ကျပ်",
                                        fontSize = 11.sp,
                                        color = CobaltPrimary
                                    )
                                    Text(
                                        text = "ဒိုင်အသားတင်လျော်ငွေ: ${String.format("%,.0f", netDeductiblePayout)} ကျပ်",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = SlateSurface,
                        contentColor = PrimaryGold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("ကိုယ်စားလှယ် (${results.map { it.customerId }.distinct().size})", fontSize = 11.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("ဘောင်ချာ (${results.map { it.voucherId }.distinct().size})", fontSize = 11.5.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("ဒိုင်ရှင်းတမ်း (${dineSettlements.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("တင်ကွက် (${overflowResults.size})", fontSize = 11.5.sp) }
                        )
                    }
                }

                if (selectedTab == 0) {
                    val groupedByAgent = results.groupBy { it.customerId }
                    if (groupedByAgent.isEmpty()) {
                        item {
                            EmptyState(msg = "ပေါက်သူ မရှိပါ။")
                        }
                    } else {
                        items(groupedByAgent.entries.toList(), key = { it.key }) { entry ->
                            val cName = entry.value.first().customerName
                            val totalPayout = entry.value.sumOf { it.payoutAmount }
                            AgentCard(
                                customerName = cName,
                                count = entry.value.size,
                                payout = totalPayout,
                                bets = entry.value,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                } else if (selectedTab == 1) {
                    val groupedByVoucher = results.groupBy { it.voucherId }
                    if (groupedByVoucher.isEmpty()) {
                        item {
                            EmptyState(msg = "ပေါက်သည့် ဘောင်ချာ မရှိပါ။")
                        }
                    } else {
                        items(groupedByVoucher.entries.toList(), key = { it.key }) { entry ->
                            val vId = entry.key
                            val cName = entry.value.first().customerName
                            val totalPayout = entry.value.sumOf { it.payoutAmount }
                            VoucherCard(
                                voucherId = vId,
                                customerName = cName,
                                payout = totalPayout,
                                bets = entry.value,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                } else if (selectedTab == 2) {
                    if (dineSettlements.isEmpty()) {
                        item {
                            EmptyState(msg = "ဤပွဲစဉ်တွင် ဒိုင်သို့ တင်ပို့ထားသော စာရင်း မရှိပါ။")
                        }
                    } else {
                        val totalExp = dineSettlements.sumOf { it.totalExported.toLong() }
                        val totalComm = dineSettlements.sumOf { it.commissionAmount.toLong() }
                        val totalDineNetPay = dineSettlements.sumOf { it.netCost.toLong() }
                        val totalWonPayout = dineSettlements.sumOf { it.winningPayout }
                        val overallDineBalance = dineSettlements.sumOf { it.netBalance }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateDarkBackground),
                                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "ဒိုင်များ စုစုပေါင်း ရှင်းတမ်း ချုပ်",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryGold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("စုစုပေါင်း တင်ပို့ငွေ:", fontSize = 11.5.sp, color = TextSecondary)
                                        Text("%,d ကျပ်".format(totalExp), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("ကော်မရှင် ရငွေ:", fontSize = 11.5.sp, color = TextSecondary)
                                        Text("+%,d ကျပ်".format(totalComm), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("ဒိုင်ထံမှ ပေါက်သီးရငွေ:", fontSize = 11.5.sp, color = TextSecondary)
                                        Text("%,d ကျပ်".format(totalWonPayout), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (overallDineBalance > 0) "🟢 စုစုပေါင်း ဒိုင်များထံမှ ရရန်:" else if (overallDineBalance < 0) "🔴 စုစုပေါင်း ဒိုင်များသို့ ပေးရန်:" else "⚪ စုစုပေါင်း ကျေအေး:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (overallDineBalance > 0) Color(0xFF10B981) else if (overallDineBalance < 0) Color(0xFFEF4444) else TextSecondary
                                        )
                                        Text(
                                            text = if (overallDineBalance > 0) "+%,d ကျပ်".format(overallDineBalance) else if (overallDineBalance < 0) "%,d ကျပ်".format(-overallDineBalance) else "၀ ကျပ်",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp,
                                            color = if (overallDineBalance > 0) Color(0xFF10B981) else if (overallDineBalance < 0) Color(0xFFEF4444) else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        items(dineSettlements, key = { "dine_${it.dineId}_${it.dineName}" }) { settlement ->
                            DineSettlementCard(
                                settlement = settlement,
                                winningNumber = winningNumber,
                                batchNumber = targetBatch.toIntOrNull() ?: currentBatch,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                } else {
                    if (overflowResults.isEmpty()) {
                        item {
                            EmptyState(msg = "တင်ကွက် ပေါက်ငွေ မရှိပါ။")
                        }
                    } else {
                        items(overflowResults, key = { "${it.exportRecordId}_${it.number}_${it.amount}" }) { ov ->
                            OverflowCard(item = ov, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("ပေါက်သီးဖျက်မည်လော") },
            text = { Text("$selectedSession ၏ ပေါက်ဂဏန်းနှင့် တွက်ချက်ထားသော စာရင်းများကို ဖျက်ပါမည်။") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val b = targetBatch.toIntOrNull() ?: currentBatch
                        viewModel.clearWinningNumber(selectedSession, b)
                        winningNumber = ""
                        isDeclared = false
                        results = emptyList()
                        overflowResults = emptyList()
                        showClearDialog = false
                    }
                ) {
                    Text("ဖျက်မည်", color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("မဖျက်ပါ")
                }
            }
        )
    }
}

@Composable
fun SlotCard(
    modifier: Modifier = Modifier,
    time: String,
    label: String,
    number: String,
    isOfficial: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = isOfficial && number.length == 2 && number != "--") { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOfficial) SlateDarkBackground else SlateSurfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            if (isOfficial) PrimaryGold.copy(alpha = 0.5f) else CardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = time,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOfficial) PrimaryGold else TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                color = if (isOfficial) CobaltPrimary else TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = number,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = if (isOfficial) Color(0xFFFFD54F) else TextPrimary
            )
            if (isOfficial && number.length == 2 && number != "--") {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryGold.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "အသုံးပြု",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AgentCard(
    customerName: String,
    count: Int,
    payout: Double,
    bets: List<WinnerResult>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "${String.format("%,.0f", payout)} ကျပ်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFFF5252)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ပေါက်ကွက်: ${bets.joinToString(", ") { "${it.betNumber} (${it.betAmount} ကျပ်)" }}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun VoucherCard(
    voucherId: Int,
    customerName: String,
    payout: Double,
    bets: List<WinnerResult>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ဘောင်ချာ #$voucherId ($customerName)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = "${String.format("%,.0f", payout)} ကျပ်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFFF5252)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ပေါက်ဂဏန်း: ${bets.joinToString(", ") { "${it.betNumber} (${it.betAmount} ကျပ်)" }}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun OverflowCard(item: OverflowWinResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${item.type} • ဂဏန်း: ${item.number}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    text = "တင်ငွေ: ${item.amount} ကျပ်",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "+${String.format("%,.0f", item.payoutAmount)} ကျပ်",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = CobaltPrimary
            )
        }
    }
}

@Composable
fun EmptyState(msg: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = msg, color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
fun DineSettlementCard(
    settlement: DineSettlement,
    winningNumber: String,
    batchNumber: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Dine Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CobaltPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = settlement.dineName.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CobaltPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = settlement.dineName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "ကော်မရှင် ${settlement.commissionRate.toInt()}% • လျော်ဆ ${settlement.multiplier}ဆ",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Copy Slip Button
                OutlinedButton(
                    onClick = {
                        val slipText = buildString {
                            appendLine("=== ဒိုင်ရှင်းတမ်း ===")
                            appendLine("ဒိုင်: ${settlement.dineName}")
                            appendLine("ပွဲစဉ်: $batchNumber")
                            if (winningNumber.isNotBlank()) appendLine("ပေါက်ဂဏန်း: $winningNumber")
                            appendLine("------------------------")
                            appendLine("တင်ပို့ငွေ စုစုပေါင်း: %,d ကျပ်".format(settlement.totalExported))
                            appendLine("ကော်မရှင် (${settlement.commissionRate.toInt()}%): -%,d ကျပ်".format(settlement.commissionAmount))
                            appendLine("ဒိုင်သို့ ပေးချေငွေ: %,d ကျပ်".format(settlement.netCost))
                            appendLine("------------------------")
                            if (settlement.wonAmount > 0) {
                                appendLine("ပေါက်သီးရငွေ: %,d ကျပ် (${settlement.wonAmount} x ${settlement.multiplier}ဆ)".format(settlement.winningPayout))
                            } else {
                                appendLine("ပေါက်သီးရငွေ: ၀ ကျပ်")
                            }
                            appendLine("------------------------")
                            if (settlement.netBalance > 0) {
                                appendLine("အသားတင်: ဒိုင်ထံမှ ရရန် +%,d ကျပ်".format(settlement.netBalance))
                            } else if (settlement.netBalance < 0) {
                                appendLine("အသားတင်: ဒိုင်သို့ ပေးရန် %,d ကျပ်".format(-settlement.netBalance))
                            } else {
                                appendLine("အသားတင်: ကျေအေး (၀ ကျပ်)")
                            }
                            appendLine("========================")
                        }
                        clipboardManager.setText(AnnotatedString(slipText))
                        Toast.makeText(context, "${settlement.dineName} ရှင်းတမ်း ကော်ပီကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CobaltPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = CobaltPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ကော်ပီ", fontSize = 11.sp, color = CobaltPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Details rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("တင်ပို့ငွေ စုစုပေါင်း", fontSize = 12.sp, color = TextSecondary)
                Text("%,d ကျပ်".format(settlement.totalExported), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ကော်မရှင် ရငွေ (${settlement.commissionRate.toInt()}%)", fontSize = 12.sp, color = TextSecondary)
                Text("+%,d ကျပ် (ရငွေ)".format(settlement.commissionAmount), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF10B981))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ဒိုင်သို့ ပေးချေငွေ (အသားတင်)", fontSize = 12.sp, color = TextSecondary)
                Text("%,d ကျပ်".format(settlement.netCost), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ဒိုင်ထံမှ ပေါက်သီးလျော်ငွေ", fontSize = 12.sp, color = TextSecondary)
                if (settlement.wonAmount > 0) {
                    Text("%,d ကျပ် (x%dဆ)".format(settlement.winningPayout, settlement.multiplier), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                } else {
                    Text("၀ ကျပ်", fontSize = 12.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Net balance highlight banner
            val isReceivable = settlement.netBalance > 0
            val isPayable = settlement.netBalance < 0
            val balanceColor = if (isReceivable) Color(0xFF059669) else if (isPayable) Color(0xFFDC2626) else TextSecondary
            val bgTint = if (isReceivable) Color(0xFFD1FAE5) else if (isPayable) Color(0xFFFEE2E2) else Color(0xFFF3F4F6)
            val borderTint = if (isReceivable) Color(0xFF10B981) else if (isPayable) Color(0xFFEF4444) else CardBorder

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = bgTint,
                border = BorderStroke(1.dp, borderTint)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isReceivable) "🟢 ဒိုင်ထံမှ ရရန်" else if (isPayable) "🔴 ဒိုင်သို့ ပေးရန်" else "⚪ ကျေအေး",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = balanceColor
                    )
                    Text(
                        text = if (isReceivable) "+%,d ကျပ်".format(settlement.netBalance)
                               else if (isPayable) "%,d ကျပ်".format(-settlement.netBalance)
                               else "၀ ကျပ်",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = balanceColor
                    )
                }
            }
        }
    }
}
