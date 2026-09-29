package com.twoDLedger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twoDLedger.data.Bet
import com.twoDLedger.logic.BetLineParseError
import com.twoDLedger.logic.TwoDBetParser
import com.twoDLedger.logic.TwoDNumberGenerator
import com.twoDLedger.ui.theme.*
import kotlinx.coroutines.launch

enum class FocusField { NUMBER, AMOUNT }
private val BettingPrimary = CobaltPrimary          // 2D Royal Cobalt Blue (0xFF1D4ED8)
private val BettingPrimaryContainer = CobaltLight // 2D Soft Ice Blue Container (0xFFDBEAFE)
private val BettingOnPrimaryContainer = CobaltDark // 2D Deep Navy Container (0xFF1E3A8A)
private val BettingGoldContainer = Color(0xFFFEF3C7)    // Soft Amber Chip
private val BettingGoldText = Color(0xFF92400E)


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
    val winningNumberForBatch = remember(currentBatch, currentSession) {
        viewModel.getWinningNumberForBatch(currentBatch)
    }
    val isWonDeclared = winningNumberForBatch.length == 2 || viewModel.isBatchDeclared(currentBatch)

    var selectedCustomer by remember { mutableStateOf<Int?>(initialCustomerId) }
    var expandedCustomer by remember { mutableStateOf(false) }

    // Dialog states
    var showPasteDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showBetConfirmDialog by remember { mutableStateOf(false) }
    var pasteErrors by remember { mutableStateOf<List<BetLineParseError>>(emptyList()) }
    var bannedRemovalsNotification by remember { mutableStateOf<List<com.twoDLedger.data.BannedLimitRemoval>>(emptyList()) }

    // Paste handling
    var pasteText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var parseProgress by remember { mutableStateOf(0f) }
    var parseStatus by remember { mutableStateOf("") }
    var detectedClipboardText by remember { mutableStateOf<String?>(null) }

    // Keypad and Input state
    var showManualKeypad by remember { mutableStateOf(false) }
    var currentBetType by remember { mutableStateOf("ဒဲ့") }
    var expandedBetTypeMenu by remember { mutableStateOf(false) }
    var focusedField by remember { mutableStateOf(FocusField.NUMBER) }
    var tempNumber by remember { mutableStateOf("") }
    var tempAmount by remember { mutableStateOf("1000") }
    var tempRemark by remember { mutableStateOf("") }
    var isFreshAmountInput by remember { mutableStateOf(true) }

    val pendingBets = remember { mutableStateListOf<Bet>() }
    val rDimens = rememberResponsiveDimens()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val quickAmounts = listOf("100", "300", "500", "1000", "2000", "3000", "5000", "10000")

    BackHandler {
        when {
            showBetConfirmDialog -> showBetConfirmDialog = false
            showClearConfirmDialog -> showClearConfirmDialog = false
            showPasteDialog -> showPasteDialog = false
            pasteErrors.isNotEmpty() -> pasteErrors = emptyList()
            showManualKeypad -> showManualKeypad = false
            else -> onNavigateBack()
        }
    }

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
            val defaultCust = customers.firstOrNull {
                !it.name.contains("တင်ကွက်") && !it.name.contains("overflow", ignoreCase = true)
            }
            if (defaultCust != null) {
                selectedCustomer = defaultCust.id
            }
        }
    }

    fun checkClipboardForBets() {
        val clip = clipboardManager.getText()?.text?.toString()?.trim()
        if (!clip.isNullOrBlank() && clip != pasteText) {
            val lines = clip.lines().filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                detectedClipboardText = clip
            }
        }
    }

    fun addBets(numbers: List<String>) {
        if (isWonDeclared) {
            android.widget.Toast.makeText(context, "ပေါက်ဂဏန်း ထွက်ပြီးပါပြီ။ ထိုးကြေးတင်၍ မရတော့ပါ။", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val amount = tempAmount.toIntOrNull() ?: 1000
        if (amount <= 0 || numbers.isEmpty()) return

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
        currentBetType = "ဒဲ့"
        focusedField = FocusField.NUMBER
        isFreshAmountInput = true
    }

    fun appendText(txt: String) {
        if (focusedField == FocusField.NUMBER) {
            if (tempNumber.length < 2) {
                tempNumber += txt
                isFreshAmountInput = true
            } else {
                // Number reached 2 digits (e.g. 34) -> auto overflow excess digits to amount field!
                focusedField = FocusField.AMOUNT
                tempAmount = txt
                isFreshAmountInput = false
            }
        } else {
            // Currently focused on AMOUNT
            if (isFreshAmountInput || tempAmount == "0" || tempAmount.isEmpty()) {
                tempAmount = txt
                isFreshAmountInput = false
            } else {
                if (tempAmount.length < 9) {
                    tempAmount += txt
                }
            }
        }
    }

    fun backspace() {
        if (focusedField == FocusField.AMOUNT) {
            if (tempAmount.isNotEmpty()) {
                tempAmount = tempAmount.dropLast(1)
            } else {
                focusedField = FocusField.NUMBER
            }
        } else if (focusedField == FocusField.NUMBER) {
            if (tempNumber.isNotEmpty()) {
                tempNumber = tempNumber.dropLast(1)
            }
        }
    }

    fun clearAll() {
        tempNumber = ""
        tempAmount = "1000"
        currentBetType = "ဒဲ့"
        focusedField = FocusField.NUMBER
        isFreshAmountInput = true
    }

    fun submit() {
        val digits = tempNumber.trim()
        val num = digits.toIntOrNull()
        val amt = tempAmount.toIntOrNull() ?: 1000
        if (amt <= 0) return

        var betsToAdd: List<String> = emptyList()

        when (currentBetType) {
            "ဒဲ့" -> {
                if (digits.length == 2) betsToAdd = listOf(digits)
                else if (digits.length == 1) betsToAdd = listOf("0$digits")
            }
            "R" -> {
                if (digits.length == 2) betsToAdd = TwoDNumberGenerator.reverse(digits)
                else if (digits.length == 1) betsToAdd = listOf("0$digits", "${digits}0")
            }
            "ထိပ်" -> {
                if (num != null && digits.length == 1) betsToAdd = TwoDNumberGenerator.head(num)
            }
            "ပိတ်", "နောက်" -> {
                if (num != null && digits.length == 1) betsToAdd = TwoDNumberGenerator.tail(num)
            }
            "ပတ်" -> {
                if (num != null && digits.length == 1) betsToAdd = TwoDNumberGenerator.roll(num)
            }
            "ဘရိတ်" -> {
                if (num != null && digits.length == 1) betsToAdd = TwoDNumberGenerator.breakNum(num)
            }
            "အပူး" -> betsToAdd = TwoDNumberGenerator.doubleNumbers()
            "ပါဝါ" -> betsToAdd = TwoDNumberGenerator.power()
            "နက္ခတ်" -> betsToAdd = TwoDNumberGenerator.natkhat()
            "ညီကို" -> betsToAdd = TwoDNumberGenerator.brothers()
            "စုံစုံ" -> betsToAdd = TwoDNumberGenerator.evenEven()
            "မမ" -> betsToAdd = TwoDNumberGenerator.oddOdd()
            "စုံမ" -> betsToAdd = TwoDNumberGenerator.evenOdd()
            "မစုံ" -> betsToAdd = TwoDNumberGenerator.oddEven()
            else -> {
                if (digits.length == 2) betsToAdd = listOf(digits)
            }
        }

        if (betsToAdd.isNotEmpty()) {
            addBets(betsToAdd)
        }
    }

    fun handleSpecial(cmd: String) {
        val digits = tempNumber.trim()
        val num = digits.toIntOrNull()
        when (cmd) {
            "R" -> {
                currentBetType = "R"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "အပူး" -> {
                currentBetType = "အပူး"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "ပါဝါ" -> {
                currentBetType = "ပါဝါ"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "နက္ခတ်" -> {
                currentBetType = "နက္ခတ်"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "ညီကို" -> {
                currentBetType = "ညီကို"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "စုံစုံ" -> {
                currentBetType = "စုံစုံ"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "မမ" -> {
                currentBetType = "မမ"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "စုံမ" -> {
                currentBetType = "စုံမ"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "မစုံ" -> {
                currentBetType = "မစုံ"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "ပတ်" -> {
                currentBetType = "ပတ်"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "ထိပ်" -> {
                currentBetType = "ထိပ်"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "ပိတ်", "နောက်" -> {
                currentBetType = "ပိတ်"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "ဘရိတ်" -> {
                currentBetType = "ဘရိတ်"
                focusedField = FocusField.AMOUNT
                isFreshAmountInput = true
            }
            "ဖျက်" -> backspace()
            "ရှင်းပါ" -> {
                if (pendingBets.isNotEmpty()) {
                    showClearConfirmDialog = true
                } else {
                    val hadInput = tempNumber.isNotEmpty() || tempAmount != "1000" || currentBetType != "ဒဲ့"
                    clearAll()
                    if (!hadInput) {
                        android.widget.Toast.makeText(context, "ရှင်းရန် စာရင်း မရှိပါ", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun addBetsFromPasteAsync(text: String) {
        if (isWonDeclared) {
            android.widget.Toast.makeText(context, "ပေါက်ဂဏန်း ထွက်ပြီးပါပြီ။ ထိုးကြေးတင်၍ မရတော့ပါ။", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val validation = TwoDBetParser.validatePastedText(text)
        if (!validation.isValid) {
            pasteErrors = validation.errors
            return
        }

        isParsing = true
        parseProgress = 0f
        parseStatus = "ထိုးကြေးများ ထည့်သွင်းနေသည်..."

        coroutineScope.launch {
            val candidateBets = validation.validBets.map { Bet(voucherId = 0, number = it.first, amount = it.second) }
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

            isParsing = false
            parseProgress = 1f
            parseStatus = "$addedCount ကြောင်း ထည့်သွင်းပြီးပါပြီ"
            showPasteDialog = false
            pasteText = ""
        }
    }

    fun submitVoucher() {
        if (isWonDeclared) {
            android.widget.Toast.makeText(context, "ပေါက်ဂဏန်း ထွက်ပြီးပါပြီ။ ထိုးကြေးတင်၍ မရတော့ပါ။", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCustomer == null) {
            expandedCustomer = true
            android.widget.Toast.makeText(context, "ထိုးသူ ရွေးချယ်ပေးပါ", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (pendingBets.isEmpty()) {
            android.widget.Toast.makeText(context, "ထိုးမည့် ဂဏန်းများ မရှိသေးပါ", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        showBetConfirmDialog = true
    }

    fun confirmAndSaveVoucher() {
        if (selectedCustomer == null || pendingBets.isEmpty()) return
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        viewModel.addVoucherWithBetList(selectedCustomer!!, time, pendingBets.toList(), tempRemark)
        tempRemark = ""
        pendingBets.clear()
        clearAll()
        showManualKeypad = false
        showBetConfirmDialog = false
        android.widget.Toast.makeText(context, "ဘောင်ချာ သိမ်းဆည်းပြီးပါပြီ", android.widget.Toast.LENGTH_SHORT).show()
    }

    val totalAmount = pendingBets.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- TOP BAR (Matches Screenshot 1) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BettingPrimaryContainer)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "အကြိမ် : $currentBatch",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BettingOnPrimaryContainer
                )
                // 2D Session Selector Chip (Minimalist)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, BettingPrimary.copy(alpha = 0.3f)),
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
                            text = if (currentSession == "12:00 PM") "၁၂:၀၀ PM" else "၄:၃၀ PM",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BettingPrimary
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = BettingPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = BettingOnPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // --- WINNING DECLARED LOCK BANNER ---
        if (isWonDeclared) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFEE2E2),
                border = BorderStroke(1.dp, Color(0xFFF87171))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "ပေါက်ဂဏန်း (${if (winningNumberForBatch.length == 2) winningNumberForBatch else viewModel.winningNumber.value}) ထွက်ပြီးပါပြီ။ ဤအကြိမ်တွင် ထိုးကြေးတင်၍ မရတော့ပါ။",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFFB91C1C)
                        )
                        Text(
                            text = "ပေါက်ဂဏန်း ထွက်ရှိပြီးဖြစ်၍ ထိုးကြေးထည့်သွင်းခြင်း ပိတ်ထားပါသည်",
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }
        }

        // --- CUSTOMER SELECTOR BAR (Matches Screenshot 1) ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clickable { expandedCustomer = true }
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = BettingPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "ထိုးသူ : ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        customers.find { it.id == selectedCustomer }?.name ?: "ကော်မရှင် ရွေးပါ ▾",
                        color = if (selectedCustomer != null) BettingPrimary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (selectedCustomer != null) {
                    Surface(
                        onClick = { onNavigateToCustomerVouchers(selectedCustomer!!) },
                        shape = RoundedCornerShape(8.dp),
                        color = BettingPrimaryContainer
                    ) {
                        Text(
                            "ဘောင်ချာများ ကြည့်ရန်",
                            color = BettingOnPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            DropdownMenu(expanded = expandedCustomer, onDismissRequest = { expandedCustomer = false }) {
                customers
                    .filter { !it.name.contains("တင်ကွက်") && !it.name.contains("overflow", ignoreCase = true) && !it.name.contains("upper", ignoreCase = true) }
                    .forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.name, fontWeight = FontWeight.SemiBold) },
                            onClick = {
                                selectedCustomer = customer.id
                                expandedCustomer = false
                            }
                        )
                    }
            }
        }

        // ── BET LIST BOX (Matches Screenshot 1) ──────────────────────────────
        val maxAmtB   = if (pendingBets.isNotEmpty()) pendingBets.maxOf { it.amount } else 0
        val amtWidthB = if (maxAmtB > 0) "%,d".format(maxAmtB).length else 5

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                .border(2.dp, BettingPrimary, RoundedCornerShape(10.dp))
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        BettingPrimary,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "စဉ်   ဂဏန်း",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "ပမာဏ",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(90.dp)
                )
                if (pendingBets.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "အားလုံး ရှင်းမည်",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(28.dp))
                }
            }

            // ── Rows or Empty State ───────────────────────────────────────────
            if (pendingBets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showManualKeypad = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "ဂဏန်းထည့်ရန်",
                            color = BettingPrimary.copy(alpha = 0.35f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "ကီးပက်ကို သုံး၍ ထိုးနိုင်သည်",
                            color = BettingPrimary.copy(alpha = 0.25f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pendingBets.size) { i ->
                        val bet = pendingBets[i]
                        val isEven = i % 2 == 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isEven) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                                .padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${i + 1}.",
                                fontSize = 11.sp,
                                color = BettingPrimary.copy(alpha = 0.45f),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(26.dp),
                                textAlign = TextAlign.End
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                bet.number,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BettingPrimary,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                modifier = Modifier.width(44.dp)
                            )
                            Text(
                                "=",
                                fontSize = 16.sp,
                                color = Color(0xFF9CA3AF),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                "%,d".format(bet.amount).padStart(amtWidthB),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827),
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                " ကျပ်",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = { pendingBets.removeAt(i) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "ဖျက်",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        if (i < pendingBets.lastIndex)
                            HorizontalDivider(
                                color = BettingPrimary.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )
                    }
                }
            }
        }

        // ── PRO CASHIER VOUCHER ACTION BAR (Matches Screenshot 1) ───────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        ) {
            val actionBarHeight = if (rDimens.isCompact) 42.dp else 46.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (rDimens.isCompact) 6.dp else 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Summary Badge (0 ကွက် = 0 Ks)
                Surface(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .height(actionBarHeight),
                    shape = RoundedCornerShape(9.dp),
                    color = BettingPrimaryContainer.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, BettingPrimary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            "${pendingBets.size} ကွက်",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (rDimens.isCompact) 11.sp else 11.5.sp,
                            lineHeight = 13.sp,
                            color = BettingPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        val amountFontSize = when {
                            totalAmount >= 100_000_000 -> 10.sp
                            totalAmount >= 10_000_000  -> 10.5.sp
                            totalAmount >= 1_000_000   -> 11.5.sp
                            else                       -> 12.5.sp
                        }
                        Text(
                            "= %,d ကျပ်".format(totalAmount),
                            fontWeight = FontWeight.Black,
                            fontSize = amountFontSize,
                            lineHeight = 15.sp,
                            color = BettingOnPrimaryContainer,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(Modifier.width(5.dp))

                // Right: Quick Bet (အမြန်ထိုး), Keypad Toggle (ကီးပက်/ဝှက်), & ထိုးမည်
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            checkClipboardForBets()
                            pasteText = ""
                            showPasteDialog = true
                        },
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(
                            horizontal = if (rDimens.isCompact) 7.dp else 9.dp,
                            vertical = 2.dp
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BettingGoldContainer,
                            contentColor = BettingGoldText
                        ),
                        modifier = Modifier.height(actionBarHeight)
                    ) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = "Quick Bet", modifier = Modifier.size(14.dp), tint = BettingGoldText)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "အမြန်ထိုး",
                            fontSize = if (rDimens.isCompact) 11.sp else 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BettingGoldText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showManualKeypad = !showManualKeypad
                        },
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(
                            horizontal = if (rDimens.isCompact) 6.dp else 8.dp,
                            vertical = 2.dp
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (showManualKeypad) BettingPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (showManualKeypad) BettingPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.height(actionBarHeight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            if (showManualKeypad) "ဝှက်" else "ကီးပက်",
                            fontSize = if (rDimens.isCompact) 10.5.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            submitVoucher()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pendingBets.isNotEmpty()) BettingPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (pendingBets.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(9.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (pendingBets.isNotEmpty()) 2.dp else 0.dp),
                        contentPadding = PaddingValues(
                            horizontal = if (rDimens.isCompact) 9.dp else 12.dp,
                            vertical = 2.dp
                        ),
                        modifier = Modifier.height(actionBarHeight)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "ထိုးမည်",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

            // ── 3. INPUT AREA & NUMBER PAD (Clean Minimal Ergonomics) ─────────────
            AnimatedVisibility(
                visible = showManualKeypad,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        // Dual Display: Number & Amount + Bet Type Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Focused Number Box (2D)
                            val isNumFocused = focusedField == FocusField.NUMBER
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    focusedField = FocusField.NUMBER
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isNumFocused) BettingPrimaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (isNumFocused) 2.dp else 1.dp,
                                    color = if (isNumFocused) BettingPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier
                                    .weight(1.15f)
                                    .height(44.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "ဂဏန်း",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isNumFocused) BettingPrimary else MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = if (tempNumber.isEmpty()) "00" else tempNumber,
                                        color = if (tempNumber.isEmpty()) MaterialTheme.colorScheme.outline.copy(alpha = 0.6f) else BettingPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Interactive Bet Type Toggle Box with Dropdown Menu
                            Box(modifier = Modifier.weight(0.95f)) {
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        expandedBetTypeMenu = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "အမျိုးအစား",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = currentBetType,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Spacer(Modifier.width(1.dp))
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = CobaltPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = expandedBetTypeMenu,
                                    onDismissRequest = { expandedBetTypeMenu = false }
                                ) {
                                    val menuOptions = listOf(
                                        "ဒဲ့", "R", "ထိပ်", "ပိတ်", "ပတ်", "ဘရိတ်", "အပူး", "ပါဝါ", "နက္ခတ်", "ညီကို"
                                    )
                                    menuOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = opt,
                                                    fontWeight = if (currentBetType == opt) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (currentBetType == opt) CobaltPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                currentBetType = opt
                                                expandedBetTypeMenu = false
                                                if (opt in listOf("အပူး", "ပါဝါ", "နက္ခတ်", "ညီကို")) {
                                                    handleSpecial(opt)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Focused Amount Box (Ks)
                            val isAmtFocused = focusedField == FocusField.AMOUNT
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    focusedField = FocusField.AMOUNT
                                    isFreshAmountInput = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAmtFocused) BettingPrimaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    width = if (isAmtFocused) 2.dp else 1.dp,
                                    color = if (isAmtFocused) BettingPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .height(44.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "ငွေပမာဏ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isAmtFocused) BettingPrimary else MaterialTheme.colorScheme.outline
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = tempAmount,
                                            color = if (isAmtFocused) BettingPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            "ကျပ်",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Remark Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (tempRemark.isEmpty()) {
                                Text(
                                    "မှတ်ချက် (မထည့်လည်းရသည်)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                            BasicTextField(
                                value = tempRemark,
                                onValueChange = { tempRemark = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Quick Amount Thumb Pills
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(quickAmounts) { amt ->
                                val isSel = tempAmount == amt
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        tempAmount = amt
                                        focusedField = FocusField.AMOUNT
                                        isFreshAmountInput = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) CobaltPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSel) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 9.dp)) {
                                        Text(
                                            amt,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // 2D Shortcut Chips
                        val shortcuts = listOf(
                            Triple("ဒဲ့", true, { currentBetType = "ဒဲ့" }),
                            Triple("R (ပြန်)", false, { handleSpecial("R") }),
                            Triple("ပတ် (အပါ)", true, { currentBetType = "ပတ်"; if (tempNumber.length == 1) handleSpecial("ပတ်") }),
                            Triple("အပူး", false, { handleSpecial("အပူး") }),
                            Triple("ထိပ်", true, { currentBetType = "ထိပ်"; if (tempNumber.length == 1) handleSpecial("ထိပ်") }),
                            Triple("ပိတ်", true, { currentBetType = "ပိတ်"; if (tempNumber.length == 1) handleSpecial("ပိတ်") }),
                            Triple("ပါဝါ", false, { handleSpecial("ပါဝါ") }),
                            Triple("နက္ခတ်", false, { handleSpecial("နက္ခတ်") }),
                            Triple("ညီကို", false, { handleSpecial("ညီကို") }),
                            Triple("စုံစုံ", false, { handleSpecial("စုံစုံ") }),
                            Triple("မမ", false, { handleSpecial("မမ") }),
                            Triple("စုံမ", false, { handleSpecial("စုံမ") }),
                            Triple("မစုံ", false, { handleSpecial("မစုံ") }),
                            Triple("ဘရိတ်", true, { currentBetType = "ဘရိတ်"; if (tempNumber.length == 1) handleSpecial("ဘရိတ်") })
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(shortcuts.size) { i ->
                                val (label, isBetTypeChip, action) = shortcuts[i]
                                val isSelected = isBetTypeChip && currentBetType == label
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        action()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) CobaltPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSelected) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 9.dp)) {
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // ── 4x4 Modern Tactile Keypad ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Row 1: [ 1 ] [ 2 ] [ 3 ] [ R ]
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TactileKeypadButton("1", modifier = Modifier.weight(1f)) { appendText("1") }
                                TactileKeypadButton("2", modifier = Modifier.weight(1f)) { appendText("2") }
                                TactileKeypadButton("3", modifier = Modifier.weight(1f)) { appendText("3") }
                                TactileKeypadButton(
                                    text = "R",
                                    subtitle = "အပြန်",
                                    bgColor = BettingPrimary,
                                    contentColor = Color.White,
                                    borderColor = BettingPrimary,
                                    modifier = Modifier.weight(1f)
                                ) { handleSpecial("R") }
                            }

                            // Row 2: [ 4 ] [ 5 ] [ 6 ] [ အပူး ]
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TactileKeypadButton("4", modifier = Modifier.weight(1f)) { appendText("4") }
                                TactileKeypadButton("5", modifier = Modifier.weight(1f)) { appendText("5") }
                                TactileKeypadButton("6", modifier = Modifier.weight(1f)) { appendText("6") }
                                TactileKeypadButton(
                                    text = "အပူး",
                                    subtitle = "00-99",
                                    bgColor = Color(0xFF0891B2),
                                    contentColor = Color.White,
                                    borderColor = Color(0xFF0E7490),
                                    modifier = Modifier.weight(1f)
                                ) { handleSpecial("အပူး") }
                            }

                            // Row 3: [ 7 ] [ 8 ] [ 9 ] [ ⌫ ]
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TactileKeypadButton("7", modifier = Modifier.weight(1f)) { appendText("7") }
                                TactileKeypadButton("8", modifier = Modifier.weight(1f)) { appendText("8") }
                                TactileKeypadButton("9", modifier = Modifier.weight(1f)) { appendText("9") }
                                TactileKeypadButton(
                                    text = "⌫",
                                    subtitle = "ဖျက်",
                                    icon = Icons.AutoMirrored.Filled.Backspace,
                                    bgColor = Color(0xFFEF4444),
                                    contentColor = Color.White,
                                    borderColor = Color(0xFFB91C1C),
                                    modifier = Modifier.weight(1f)
                                ) { backspace() }
                            }

                            // Row 4: [ ရှင်း ] [ 0 ] [ 00 ] [ OK / ထည့် ]
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TactileKeypadButton(
                                    text = "ရှင်း",
                                    subtitle = "ဖျက်မည်",
                                    bgColor = Color(0xFFD97706),
                                    contentColor = Color.White,
                                    borderColor = Color(0xFF92400E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    handleSpecial("ရှင်းပါ")
                                }
                                TactileKeypadButton("0", modifier = Modifier.weight(1f)) { appendText("0") }
                                TactileKeypadButton("00", modifier = Modifier.weight(1f)) { appendText("00") }
                                TactileKeypadButton(
                                    text = "ထည့်မည်",
                                    subtitle = null,
                                    bgColor = BettingPrimary,
                                    contentColor = Color.White,
                                    borderColor = BettingPrimary,
                                    modifier = Modifier.weight(1f)
                                ) { submit() }
                            }
                        }
                    }
                }
            }
        }


        // --- QUICK PASTE DIALOG ---
        if (showPasteDialog) {
            val lineCount = pasteText.lines().count { it.isNotBlank() }
            AlertDialog(
                onDismissRequest = { if (!isParsing) showPasteDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⚡ အမြန်ထိုး စာရင်းထည့်သွင်းခြင်း", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (lineCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CobaltLight
                                ) {
                                    Text(
                                        "%,d မျဉ်း".format(lineCount),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 11.sp,
                                        color = CobaltPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { if (!isParsing) showPasteDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "စာကြောင်းအလိုက် 2D အမြန်ထိုး / ဘောင်ချာ စာရင်း",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // One-tap clipboard paste detection card
                        val detClip = detectedClipboardText
                        if (pasteText.isBlank() && !detClip.isNullOrBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CobaltLight.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Clipboard တွင် စာသားတွေ့ရှိပါသည်", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CobaltPrimary)
                                        Text(
                                            detClip.take(40) + if (detClip.length > 40) "..." else "",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { pasteText = detClip },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary)
                                    ) {
                                        Text("ကူးထည့်မည်", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Text input field
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            placeholder = { Text("ဥပမာ -\n25=1000\n12/34/56 - 500\n07R 1000\nအပူး 500\n7ထိပ် 1000", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 220.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (isParsing) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { parseProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CobaltPrimary
                                )
                                Text(parseStatus, fontSize = 12.sp, color = CobaltPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { addBetsFromPasteAsync(pasteText) },
                        enabled = pasteText.isNotBlank() && !isParsing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary)
                    ) {
                        Text(if (isParsing) "ထည့်နေသည်..." else "ထည့်သွင်းမည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { if (!isParsing) showPasteDialog = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("မလုပ်တော့")
                    }
                }
            )
        }

        // --- PASTE ERROR DECLINE ALERT DIALOG ---
        if (pasteErrors.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { pasteErrors = emptyList() },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "စာရင်းတွင် ပုံစံမမှန်သော အမှားများ ပါဝင်နေပါသည်",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFDC2626),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "အမှား ${pasteErrors.size} ခု တွေ့ရှိပါသည်",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF991B1B)
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "အောက်ပါ စာကြောင်းများသည် 2D ထိုးကြေးပုံစံနှင့် မကိုက်ညီပါ (ပြင်ဆင်ပေးပါ) :",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(pasteErrors) { err ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                "မျဉ်း ${err.lineNumber}: \"${err.rawLine}\"",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = Color(0xFFB91C1C),
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                err.reason,
                                                fontSize = 11.sp,
                                                color = Color(0xFF7F1D1D)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { pasteErrors = emptyList() },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ပြန်လည် ပြင်ဆင်မည်", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            )
        }

        // --- BET CONFIRMATION DIALOG (Screenshot 2 Match) ---
        if (showBetConfirmDialog) {
            val customerObj = customers.find { it.id == selectedCustomer }
            val customerName = customerObj?.name ?: "သတ်မှတ်မထားပါ"
            AlertDialog(
                onDismissRequest = {
                    showBetConfirmDialog = false
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BettingPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                },
                title = {
                    Text(
                        "ထိုးကြေး စာရင်းသွင်းရန် အတည်ပြုပါ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BettingPrimaryContainer.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ထိုးသူ :", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    Text(customerName, fontWeight = FontWeight.Bold, color = BettingPrimary, fontSize = 15.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ဂဏန်း အရေအတွက် :", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                    Text("${pendingBets.size} ကွက်", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("ကျသင့်ငွေ :", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "= %,d ကျပ်".format(totalAmount),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = BettingPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (tempRemark.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("မှတ်ချက် :", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        Text(tempRemark, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }

                        Text(
                            "မတော်တဆ ထိမိခြင်းမှ ကာကွယ်ရန် ထိုးကြေး စာရင်းသွင်းမှုကို အတည်ပြုပေးပါ။ အမှန်တကယ် ထိုးမည်ဆိုပါက 'အတည်ပြု ထိုးမည်' ကို နှိပ်ပါ။",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBetConfirmDialog = false
                            confirmAndSaveVoucher()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BettingPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("အတည်ပြု ထိုးမည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showBetConfirmDialog = false
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ဖျက်သိမ်းမည် (မထိုးပါ)")
                    }
                }
            )
        }
        // --- CONFIRM CLEAR ALL DIALOG ---
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "ထိုးထားသော စာရင်းများ အားလုံး ရှင်းလင်းမည်လား?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ဖျက်မည့် စာရင်း :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${pendingBets.size} ကွက်", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("စုစုပေါင်း ပမာဏ :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("%,d ကျပ်".format(totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                }
                            }
                        }
                        Text(
                            "စာရင်းသွင်းထားသော ထိုးကြေးဂဏန်းများ အားလုံး ပျက်သွားပါမည်။ အမှန်တကယ် ရှင်းလင်းမည်ဆိုပါက 'အားလုံး ရှင်းမည်' ကို နှိပ်ပါ။",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingBets.clear()
                            clearAll()
                            showClearConfirmDialog = false
                            android.widget.Toast.makeText(context, "စာရင်းများ အားလုံး ရှင်းလင်းပြီးပါပြီ", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("အားလုံး ရှင်းမည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = false },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("မရှင်းပါ (ဖျက်သိမ်း)")
                    }
                }
            )
        }
        // --- PASTE ERRORS DIALOG ---
        if (pasteErrors.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { pasteErrors = emptyList() },
                title = {
                    Text(
                        "စာကြောင်းအမှား (${pasteErrors.size}) ခု တွေ့ရှိပါသည်",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(pasteErrors) { err ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("မျဉ်း ${err.lineNumber}: ${err.rawLine}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    Text(err.reason, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { pasteErrors = emptyList() },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ပြန်ပြင်မည်", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- BANNED LIMIT REMOVAL NOTIFICATION DIALOG ---
        if (bannedRemovalsNotification.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { bannedRemovalsNotification = emptyList() },
                title = {
                    Text(
                        "ပိတ်ဂဏန်း / ကန့်သတ်ငွေကျော်လွန်မှု",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(bannedRemovalsNotification) { rem ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        rem.number,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        rem.reason,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { bannedRemovalsNotification = emptyList() },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("နားလည်ပါပြီ", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }

// ── Modern Minimalist Tactile Keypad Button ──────────────────────────────────
@Composable
fun TactileKeypadButton(
    text: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    bgColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    bevelColor: Color? = null,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetY = if (isPressed) 1.5.dp else 0.dp
    val rDimens = rememberResponsiveDimens()
    val buttonHeight = if (rDimens.isCompact) 42.dp else 46.dp

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = if (isPressed) bgColor.copy(alpha = 0.88f) else bgColor,
        border = BorderStroke(1.dp, if (isPressed) borderColor else borderColor.copy(alpha = 0.6f)),
        shadowElevation = if (isPressed) 0.dp else 1.dp,
        modifier = modifier
            .height(buttonHeight)
            .padding(1.dp)
            .offset(y = offsetY)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            } else {
                Text(
                    text = text,
                    fontSize = if (text.length > 2) 13.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontFamily = if (text.all { it.isDigit() }) FontFamily.Monospace else FontFamily.Default,
                    letterSpacing = if (text.all { it.isDigit() }) 1.sp else 0.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}