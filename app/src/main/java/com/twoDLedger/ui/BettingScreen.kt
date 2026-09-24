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
                        Text(
                            text = "အကြိမ် $currentBatch",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

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

                    // Input Fields: Number & Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tempNumber,
                            onValueChange = {
                                if (it.length <= 2) tempNumber = it.filter { c -> c.isDigit() }
                            },
                            label = { Text("ဂဏန်း (2D)", fontSize = 11.sp) },
                            placeholder = { Text("00-99", fontSize = 12.sp, color = TextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { focusedField = FocusField.NUMBER },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGold,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = tempAmount,
                            onValueChange = { tempAmount = it.filter { c -> c.isDigit() } },
                            label = { Text("ငွေပမာဏ (Ks)", fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable { focusedField = FocusField.AMOUNT },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGold,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Button(
                            onClick = { submit() },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                        ) {
                            Text("ထည့်", color = SlateDarkBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                onClick = { tempAmount = amt },
                                label = { Text(amt, fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = SlateDarkBackground,
                                    labelColor = if (tempAmount == amt) PrimaryGold else TextSecondary
                                ),
                                border = BorderStroke(0.5.dp, if (tempAmount == amt) PrimaryGold else CardBorder)
                            )
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
fun KeypadButton(
    text: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor)
    ) {
        Text(text)
    }
}
@Composable
fun TactileKeypadButton(
    text: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    bgColor: Color = Color.White,
    contentColor: Color = if (bgColor == Color.White) Color.Black else Color.White,
    bevelColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor, contentColor = contentColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = text, tint = contentColor)
            } else {
                Text(text, color = contentColor, fontWeight = FontWeight.Bold)
            }
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 9.sp, color = contentColor.copy(alpha = 0.8f))
            }
        }
    }
}