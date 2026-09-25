package com.twoDLedger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Keyboard
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twoDLedger.data.Bet
import com.twoDLedger.logic.TwoDBetParser
import com.twoDLedger.logic.TwoDNumberGenerator
import com.twoDLedger.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

fun validateAndParseLine(raw: String, lineNumber: Int): LineParseResult {
    val trimmed = raw.trim()
    if (trimmed.isBlank() || TwoDBetParser.isVoucherMetadataLine(trimmed)) {
        return LineParseResult.Ignored
    }
    val bets = TwoDBetParser.parseLine(raw)
    if (bets.isEmpty()) {
        return LineParseResult.Error(
            BetLineParseError(
                lineNumber = lineNumber,
                rawLine = raw,
                reason = "၂ လုံး ဂဏန်း (00-99) သို့မဟုတ် ပုံစံမမှန်ပါ"
            )
        )
    }
    return LineParseResult.Success(bets)
}

fun validatePastedText(text: String): PasteValidationResult {
    val lines = text.lines()
    val allBets = mutableListOf<Pair<String, Int>>()
    val errors = mutableListOf<BetLineParseError>()

    lines.forEachIndexed { index, rawLine ->
        val lineNum = index + 1
        when (val res = validateAndParseLine(rawLine, lineNum)) {
            is LineParseResult.Success -> allBets.addAll(res.bets)
            is LineParseResult.Error -> errors.add(res.error)
            is LineParseResult.Ignored -> {}
        }
    }

    return if (errors.isNotEmpty()) {
        PasteValidationResult(isValid = false, validBets = emptyList(), errors = errors)
    } else {
        PasteValidationResult(isValid = true, validBets = allBets, errors = emptyList())
    }
}

enum class FocusField { NUMBER, AMOUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BettingScreen(
    viewModel: MainViewModel,
    initialCustomerId: Int? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCustomerVouchers: (Int) -> Unit = {}
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val currentBatch by viewModel.currentBatch.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()

    var selectedCustomer by remember { mutableStateOf<Int?>(initialCustomerId) }
    var expandedCustomer by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var isParsing by remember { mutableStateOf(false) }
    val rDimens = rememberResponsiveDimens()
    val haptic = LocalHapticFeedback.current

    var bannedRemovalsNotification by remember { mutableStateOf<List<com.twoDLedger.data.BannedLimitRemoval>>(emptyList()) }

    BackHandler {
        if (showClearConfirmDialog) {
            showClearConfirmDialog = false
        } else if (showPasteDialog) {
            showPasteDialog = false
        } else {
            onNavigateBack()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.bannedNumberEvent.collect {
            android.widget.Toast.makeText(context, "ထိုးထားသော ဂဏန်းများထဲတွင် ပိတ်ထားသော ဂဏန်းများ ပါဝင်နေသဖြင့် ဖယ်ရှားလိုက်ပါသည်", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.bannedLimitNotificationEvent.collect { removals ->
            if (removals.isNotEmpty()) {
                bannedRemovalsNotification = removals
            }
        }
    }

    LaunchedEffect(customers) {
        if (selectedCustomer == null && customers.isNotEmpty()) {
            val defaultCust = customers.firstOrNull { !it.name.contains("တင်ကွက်") && !it.name.contains("overflow", ignoreCase = true) }
            if (defaultCust != null) {
                selectedCustomer = defaultCust.id
            }
        }
    }

    var currentBetType by remember { mutableStateOf("ဒဲ့") }
    var pasteText by remember { mutableStateOf("") }
    var parseProgress by remember { mutableStateOf(0f) }
    var parseStatus by remember { mutableStateOf("") }
    var pasteErrors by remember { mutableStateOf<List<BetLineParseError>>(emptyList()) }

    val pendingBets = remember { mutableStateListOf<Bet>() }

    var focusedField by remember { mutableStateOf(FocusField.NUMBER) }
    var tempNumber by remember { mutableStateOf("") }
    var tempAmount by remember { mutableStateOf("1000") }
    var tempRemark by remember { mutableStateOf("") }
    var showManualKeypad by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val quickAmounts = listOf("100", "300", "500", "1000", "2000", "5000", "10000")

    fun addBets(numbers: List<String>) {
        val amount = tempAmount.toIntOrNull() ?: 0
        if (amount <= 0) return

        val candidateBets = numbers.map { num -> Bet(voucherId = 0, number = num, amount = amount) }
        val pendingMap = pendingBets.groupBy { it.number }.mapValues { (_, list) -> list.sumOf { it.amount } }
        val (validBets, removals) = viewModel.validateAndFilterBetsWithBannedLimits(candidateBets, pendingMap)

        for (bet in validBets) {
            pendingBets.add(bet)
        }

        if (removals.isNotEmpty()) {
            bannedRemovalsNotification = removals
        }

        tempNumber = ""
        focusedField = FocusField.NUMBER
    }

    fun appendText(txt: String) {
        if (focusedField == FocusField.NUMBER) {
            if (tempNumber.length < 2) tempNumber += txt
        } else {
            if (tempAmount == "0" || tempAmount.isEmpty()) {
                tempAmount = txt
            } else {
                tempAmount += txt
            }
        }
    }

    fun backspace() {
        if (focusedField == FocusField.NUMBER && tempNumber.isNotEmpty()) {
            tempNumber = tempNumber.dropLast(1)
        } else if (focusedField == FocusField.AMOUNT && tempAmount.isNotEmpty()) {
            tempAmount = tempAmount.dropLast(1)
        }
    }

    fun clearAll() {
        tempNumber = ""
        tempAmount = "1000"
        focusedField = FocusField.NUMBER
    }

    fun addBetsFromPasteAsync(text: String) {
        val validation = validatePastedText(text)
        if (!validation.isValid) {
            pasteErrors = validation.errors
            return
        }

        isParsing = true
        parseProgress = 0f
        parseStatus = "ပြင်ဆင်နေသည်..."
        coroutineScope.launch {
            val candidateBets = validation.validBets
                .filter { it.second > 0 }
                .map { (num, amt) -> Bet(voucherId = 0, number = num, amount = amt) }
            val pendingMap = pendingBets.groupBy { it.number }.mapValues { (_, list) -> list.sumOf { it.amount } }
            val (validBets, removals) = viewModel.validateAndFilterBetsWithBannedLimits(candidateBets, pendingMap)

            val addedCount = validBets.size
            if (addedCount <= 500) {
                pendingBets.addAll(validBets)
            } else {
                if (selectedCustomer != null) {
                    val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    viewModel.addVoucherWithBetList(selectedCustomer!!, time, validBets, tempRemark)
                    tempRemark = ""
                    android.widget.Toast.makeText(context, "${validBets.size} ကြောင်း ထိုးကြေး သိမ်းဆည်းပြီး", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    pendingBets.addAll(validBets)
                }
            }

            if (removals.isNotEmpty()) {
                bannedRemovalsNotification = removals
            }
            if (addedCount in 1..500) {
                android.widget.Toast.makeText(context, "$addedCount ကြောင်း ထည့်သွင်းပြီး", android.widget.Toast.LENGTH_SHORT).show()
            }

            isParsing = false
            parseProgress = 1f
            parseStatus = "$addedCount ကြောင်း ထည့်သွင်းပြီး"
        }
    }

    fun submit() {
        val num = tempNumber.toIntOrNull()
        val digits = tempNumber
        if (tempNumber.isEmpty()) return

        when (currentBetType) {
            "ဒဲ့" -> if (digits.length == 2) addBets(listOf(digits))
            "ထိပ်" -> if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.head(num))
            "နောက်" -> if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.tail(num))
            "ဘရိတ်" -> if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.breakNum(num))
            else -> if (digits.length == 2) addBets(listOf(digits))
        }
    }

    fun handleSpecial(cmd: String) {
        val num = tempNumber.toIntOrNull()
        val digits = tempNumber
        when (cmd) {
            "R" -> if (digits.length == 2) addBets(TwoDNumberGenerator.reverse(digits))
            "အပူး" -> addBets(TwoDNumberGenerator.doubleNumbers())
            "ပါဝါ" -> addBets(TwoDNumberGenerator.power())
            "နက္ခတ်" -> addBets(TwoDNumberGenerator.natkhat())
            "ညီကို" -> addBets(TwoDNumberGenerator.brothers())
            "စုံစုံ" -> addBets(TwoDNumberGenerator.evenEven())
            "မမ" -> addBets(TwoDNumberGenerator.oddOdd())
            "စုံမ" -> addBets(TwoDNumberGenerator.evenOdd())
            "မစုံ" -> addBets(TwoDNumberGenerator.oddEven())
            "ဘရိတ်" -> if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.breakNum(num))
            "/" -> backspace()
            "ဖျက်" -> backspace()
            "ရှင်းပါ" -> {
                if (pendingBets.isNotEmpty()) {
                    showClearConfirmDialog = true
                } else {
                    val hadInput = tempNumber.isNotEmpty() || tempAmount != "1000"
                    clearAll()
                    if (!hadInput) {
                        android.widget.Toast.makeText(context, "ရှင်းရန် စာရင်း မရှိပါ", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun submitVoucher() {
        if (selectedCustomer == null) {
            expandedCustomer = true
            android.widget.Toast.makeText(context, "ထိုးသူ ရွေးပါ", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (pendingBets.isEmpty()) return

        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        viewModel.addVoucherWithBetList(selectedCustomer!!, time, pendingBets.toList(), tempRemark)
        tempRemark = ""
        pendingBets.clear()
        android.widget.Toast.makeText(context, "ဘောင်ချာ သိမ်းဆည်းပြီးပါပြီ", android.widget.Toast.LENGTH_SHORT).show()
    }

    val primaryBlue = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "2D ထိုးကြေး စာရင်း",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGold
                        )
                        // 2D Session Selector Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryGold.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                val nextSess = if (currentSession == "12:00 PM") "4:30 PM" else "12:00 PM"
                                viewModel.setSession(nextSess)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentSession,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGold
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryGold)
                    }
                },
                actions = {
                    IconButton(onClick = { showPasteDialog = true }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste Bets", tint = PrimaryGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDarkBackground)
            )
        },
        containerColor = SlateBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Customer Header Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            modifier = Modifier.clickable { expandedCustomer = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = customers.find { it.id == selectedCustomer }?.name ?: "ထိုးသူ ရွေးပါ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryGold)
                        }

                        DropdownMenu(
                            expanded = expandedCustomer,
                            onDismissRequest = { expandedCustomer = false }
                        ) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedCustomer = c.id
                                        expandedCustomer = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentSession == "12:00 PM") Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                        ) {
                            Text(
                                text = if (currentSession == "12:00 PM") "☀️ ၁၂:၀၀" else "🌙 ၄:၃၀",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSession == "12:00 PM") Color(0xFF92400E) else Color(0xFF1E40AF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Button(
                            onClick = { submitVoucher() },
                            enabled = pendingBets.isNotEmpty() && selectedCustomer != null,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ဘောင်ချာသိမ်း (${pendingBets.size})",
                                color = SlateDarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Bet Items List
            Box(modifier = Modifier.weight(1f)) {
                if (pendingBets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "ထိုးဂဏန်းများ ထည့်သွင်းပါ (သို့) အမြန်ထိုး သုံးပါ",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(pendingBets.reversed()) { bet ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                border = BorderStroke(0.5.dp, CardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bet.number,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${String.format("%,d", bet.amount)} Ks",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldPrimary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { pendingBets.remove(bet) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input Controls & 2D Keypad
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Quick Bet / Shortcut Chips: ဒဲ့, R, အပူး, ထိပ်, နောက်, ပါဝါ, နက္ခတ်, ညီကို, စုံစုံ, မမ, ဘရိတ်
                    val shortcuts = listOf(
                        Triple("ဒဲ့", true, { currentBetType = "ဒဲ့" }),
                        Triple("R (ပြန်)", false, { handleSpecial("R") }),
                        Triple("အပူး", false, { handleSpecial("အပူး") }),
                        Triple("ထိပ်", true, { currentBetType = "ထိပ်" }),
                        Triple("နောက်", true, { currentBetType = "နောက်" }),
                        Triple("ပါဝါ", false, { handleSpecial("ပါဝါ") }),
                        Triple("နက္ခတ်", false, { handleSpecial("နက္ခတ်") }),
                        Triple("ညီကို", false, { handleSpecial("ညီကို") }),
                        Triple("စုံစုံ", false, { handleSpecial("စုံစုံ") }),
                        Triple("မမ", false, { handleSpecial("မမ") }),
                        Triple("ဘရိတ်", false, { handleSpecial("ဘရိတ်") })
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(shortcuts.size) { i ->
                            val (label, isType, action) = shortcuts[i]
                            val isSel = isType && currentBetType == label
                            FilterChip(
                                selected = isSel,
                                onClick = { action() },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGold.copy(alpha = 0.2f),
                                    selectedLabelColor = PrimaryGold,
                                    containerColor = SlateDarkBackground,
                                    labelColor = TextSecondary
                                ),
                                border = BorderStroke(1.dp, if (isSel) PrimaryGold else CardBorder)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Tactile Dual Displays: Number & Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Number Box
                        Surface(
                            onClick = { focusedField = FocusField.NUMBER },
                            shape = RoundedCornerShape(12.dp),
                            color = SlateDarkBackground,
                            border = BorderStroke(
                                if (focusedField == FocusField.NUMBER) 1.8.dp else 1.dp,
                                if (focusedField == FocusField.NUMBER) PrimaryGold else CardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(
                                    text = "ဂဏန်း (2D)",
                                    fontSize = 10.sp,
                                    color = if (focusedField == FocusField.NUMBER) PrimaryGold else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (tempNumber.isEmpty()) "--" else tempNumber,
                                    fontSize = 22.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (tempNumber.isEmpty()) TextMuted else TextPrimary
                                )
                            }
                        }

                        // Amount Box
                        Surface(
                            onClick = { focusedField = FocusField.AMOUNT },
                            shape = RoundedCornerShape(12.dp),
                            color = SlateDarkBackground,
                            border = BorderStroke(
                                if (focusedField == FocusField.AMOUNT) 1.8.dp else 1.dp,
                                if (focusedField == FocusField.AMOUNT) EmeraldPrimary else CardBorder
                            ),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(
                                    text = "ငွေပမာဏ (Ks)",
                                    fontSize = 10.sp,
                                    color = if (focusedField == FocusField.AMOUNT) EmeraldPrimary else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${String.format("%,d", tempAmount.toIntOrNull() ?: 0)} Ks",
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                submit()
                            },
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                        ) {
                            Text("ထည့်", color = SlateDarkBackground, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Quick Amount Chips: 100, 300, 500, 1000, 2000, 5000, 10000
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickAmounts) { amt ->
                            SuggestionChip(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tempAmount = amt
                                },
                                label = { Text(amt, fontSize = 10.sp, fontWeight = if (tempAmount == amt) FontWeight.Bold else FontWeight.Normal) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = SlateDarkBackground,
                                    labelColor = if (tempAmount == amt) PrimaryGold else TextSecondary
                                ),
                                border = BorderStroke(0.5.dp, if (tempAmount == amt) PrimaryGold else CardBorder)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── 2D Dedicated Tactile Number Pad (Uses အပူး instead of Tri) ──
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Row 1: [ 1 ] [ 2 ] [ 3 ] [ အပူး ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PadKey("1", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("1") }
                            PadKey("2", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("2") }
                            PadKey("3", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("3") }
                            PadActionKey(
                                title = "အပူး",
                                subtitle = "00-99",
                                bgColor = KeypadActionTeal,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                handleSpecial("အပူး")
                            }
                        }

                        // Row 2: [ 4 ] [ 5 ] [ 6 ] [ R ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PadKey("4", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("4") }
                            PadKey("5", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("5") }
                            PadKey("6", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("6") }
                            PadActionKey(
                                title = "R",
                                subtitle = "အပြန်",
                                bgColor = CobaltPrimary,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                handleSpecial("R")
                            }
                        }

                        // Row 3: [ 7 ] [ 8 ] [ 9 ] [ ⌫ ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PadKey("7", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("7") }
                            PadKey("8", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("8") }
                            PadKey("9", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("9") }
                            PadActionKey(
                                title = "⌫",
                                subtitle = "ဖျက်",
                                bgColor = KeypadBackspaceRed,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                backspace()
                            }
                        }

                        // Row 4: [ ရှင်း ] [ 0 ] [ ငွေ/ဂဏန်း ] [ ထည့် ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PadActionKey(
                                title = "ရှင်း",
                                subtitle = "Clear",
                                bgColor = KeypadClearAmber,
                                modifier = Modifier.weight(1f)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                handleSpecial("ရှင်းပါ")
                            }
                            PadKey("0", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); appendText("0") }
                            PadActionKey(
                                title = if (focusedField == FocusField.NUMBER) "ငွေသို့" else "ဂဏန်းသို့",
                                subtitle = if (focusedField == FocusField.NUMBER) "Amount" else "Number",
                                bgColor = SlateSurfaceVariant,
                                contentColor = PrimaryGold,
                                modifier = Modifier.weight(1.1f)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                focusedField = if (focusedField == FocusField.NUMBER) FocusField.AMOUNT else FocusField.NUMBER
                            }
                            PadActionKey(
                                title = "ထည့်",
                                subtitle = "OK",
                                bgColor = PrimaryGold,
                                contentColor = SlateDarkBackground,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                submit()
                            }
                        }
                    }
                }
            }
        }

        // Paste Dialog
        if (showPasteDialog) {
            AlertDialog(
                onDismissRequest = { showPasteDialog = false },
                title = { Text("အမြန်ထိုး စာရင်းထည့်သွင်းခြင်း", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ဥပမာ: 12=500 သို့မဟုတ် 12-34-56=1000 သို့မဟုတ် 12R=500 သို့မဟုတ် 2ထိပ်=500 သို့မဟုတ် အပူး=500",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            placeholder = { Text("စာရင်း ကူးထည့်ပါ...", fontSize = 12.sp, color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGold,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pasteText.isNotBlank()) {
                                addBetsFromPasteAsync(pasteText)
                                showPasteDialog = false
                                pasteText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("ထည့်မည်", color = SlateDarkBackground, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasteDialog = false }) {
                        Text("မလုပ်တော့")
                    }
                }
            )
        }

        // Banned Limit Notification Dialog
        if (bannedRemovalsNotification.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { bannedRemovalsNotification = emptyList() },
                title = { Text("သတိပေးချက် (ဂဏန်းကန့်သတ်ချက်)", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("အောက်ပါဂဏန်းများသည် ပိတ်ထားခြင်း သို့မဟုတ် ကန့်သတ်ငွေပြည့်သွားသဖြင့် ဖယ်ရှားလိုက်ပါသည် -", fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        bannedRemovalsNotification.forEach { rem ->
                            Text("• ဂဏန်း ${rem.number}: ${rem.reason}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { bannedRemovalsNotification = emptyList() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("နားလည်ပါပြီ", color = SlateDarkBackground)
                    }
                }
            )
        }

        // Paste Errors Dialog
        if (pasteErrors.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { pasteErrors = emptyList() },
                title = { Text("စာရင်း ပယ်ချပါသည်", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("၂ လုံး မပြည့်သော ဂဏန်းများ သို့မဟုတ် ပုံစံမမှန်သော စာကြောင်းများ ပါဝင်နေသဖြင့် ပယ်ချလိုက်ပါသည် -", fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        pasteErrors.take(5).forEach { err ->
                            Text("• စာကြောင်း ${err.lineNumber}: ${err.reason} (${err.rawLine})", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { pasteErrors = emptyList() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                    ) {
                        Text("နားလည်ပါပြီ", color = SlateDarkBackground)
                    }
                }
            )
        }
    }
}

@Composable
fun PadKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = SlateDarkBackground,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun PadActionKey(
    title: String,
    subtitle: String? = null,
    bgColor: Color,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        shadowElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = contentColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}