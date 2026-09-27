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
    var showManualKeypad by remember { mutableStateOf(true) }
    var currentBetType by remember { mutableStateOf("ဒဲ့") }
    var focusedField by remember { mutableStateOf(FocusField.NUMBER) }
    var tempNumber by remember { mutableStateOf("") }
    var tempAmount by remember { mutableStateOf("1000") }
    var tempRemark by remember { mutableStateOf("") }

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
        val amount = tempAmount.toIntOrNull() ?: 0
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
        focusedField = FocusField.NUMBER
    }

    fun appendText(txt: String) {
        if (focusedField == FocusField.NUMBER) {
            if (tempNumber.length < 2) {
                tempNumber += txt
            }
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

    fun submit() {
        val digits = tempNumber.trim()
        val num = digits.toIntOrNull()
        if (digits.isEmpty()) return

        when (currentBetType) {
            "ဒဲ့" -> {
                if (digits.length == 2) {
                    addBets(listOf(digits))
                } else if (digits.length == 1) {
                    addBets(listOf("0$digits"))
                }
            }
            "R" -> {
                if (digits.length == 2) {
                    addBets(TwoDNumberGenerator.reverse(digits))
                }
            }
            "ထိပ်" -> {
                if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.head(num))
            }
            "ပိတ်", "နောက်" -> {
                if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.tail(num))
            }
            "ပတ်" -> {
                if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.roll(num))
            }
            "ဘရိတ်" -> {
                if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.breakNum(num))
            }
            else -> {
                if (digits.length == 2) addBets(listOf(digits))
            }
        }
    }

    fun handleSpecial(cmd: String) {
        val digits = tempNumber.trim()
        val num = digits.toIntOrNull()
        when (cmd) {
            "R" -> {
                if (digits.length == 2) {
                    addBets(TwoDNumberGenerator.reverse(digits))
                } else {
                    currentBetType = "R"
                }
            }
            "အပူး" -> addBets(TwoDNumberGenerator.doubleNumbers())
            "ပါဝါ" -> addBets(TwoDNumberGenerator.power())
            "နက္ခတ်" -> addBets(TwoDNumberGenerator.natkhat())
            "ညီကို" -> addBets(TwoDNumberGenerator.brothers())
            "စုံစုံ" -> addBets(TwoDNumberGenerator.evenEven())
            "မမ" -> addBets(TwoDNumberGenerator.oddOdd())
            "စုံမ" -> addBets(TwoDNumberGenerator.evenOdd())
            "မစုံ" -> addBets(TwoDNumberGenerator.oddEven())
            "ပတ်" -> {
                currentBetType = "ပတ်"
                if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.roll(num))
            }
            "ထိပ်" -> {
                currentBetType = "ထိပ်"
                if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.head(num))
            }
            "ပိတ်", "နောက်" -> {
                currentBetType = "ပိတ်"
                if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.tail(num))
            }
            "ဘရိတ်" -> {
                currentBetType = "ဘရိတ်"
                if (num != null && digits.length == 1) addBets(TwoDNumberGenerator.breakNum(num))
            }
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

    fun addBetsFromPasteAsync(text: String) {
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
        showBetConfirmDialog = false
        android.widget.Toast.makeText(context, "ဘောင်ချာ သိမ်းဆည်းပြီးပါပြီ", android.widget.Toast.LENGTH_SHORT).show()
    }

    val totalAmount = pendingBets.sumOf { it.amount }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        // 2D Session Selector Chip (Minimalist)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
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
                                    text = if (currentSession == "12:00 PM") "☀️ ၁၂:၀၀" else "🌙 ၄:၃၀",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        checkClipboardForBets()
                        showPasteDialog = true
                    }) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = "Quick Paste",
                            tint = CobaltPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Customer Selector Bar (Minimalist Neat Card)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.clickable { expandedCustomer = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = CobaltPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ထိုးသူ : ",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = customers.find { it.id == selectedCustomer }?.name ?: "ထိုးသူ ရွေးပါ ▾",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (selectedCustomer != null) CobaltPrimary else MaterialTheme.colorScheme.error
                            )
                        }

                        DropdownMenu(
                            expanded = expandedCustomer,
                            onDismissRequest = { expandedCustomer = false }
                        ) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedCustomer = c.id
                                        expandedCustomer = false
                                    }
                                )
                            }
                        }
                    }

                    // Customer Vouchers Shortcut Button
                    if (selectedCustomer != null) {
                        Surface(
                            onClick = { onNavigateToCustomerVouchers(selectedCustomer!!) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                "ဘောင်ချာများ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // PRO CASHIER VOUCHER ACTION BAR (From 3d_Agent adapted to 2D)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                val actionBarHeight = if (rDimens.isCompact) 42.dp else 46.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Summary Badge (Count + Total Amount Ks)
                    Surface(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .height(actionBarHeight),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "${pendingBets.size} ကွက်",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = CobaltPrimary,
                                maxLines = 1
                            )
                            Text(
                                "= %,d Ks".format(totalAmount),
                                fontWeight = FontWeight.Black,
                                fontSize = if (totalAmount >= 10_000_000) 11.sp else 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Right: Quick Bet (အမြန်ထိုး), Keypad Toggle (ဝှက်/ကီးပက်), and Bet (ထိုးမည်)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Bet button
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                checkClipboardForBets()
                                pasteText = ""
                                showPasteDialog = true
                            },
                            shape = RoundedCornerShape(9.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(actionBarHeight)
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = "Quick Bet", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("အမြန်ထိုး", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        // Keypad Toggle: easily collapse keypad to inspect bets or edit
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showManualKeypad = !showManualKeypad
                            },
                            shape = RoundedCornerShape(9.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (showManualKeypad) CobaltPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (showManualKeypad) CobaltPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        // Submit Voucher Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                submitVoucher()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pendingBets.isNotEmpty()) CobaltPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (pendingBets.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(9.dp),
                            enabled = pendingBets.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(actionBarHeight)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("ထိုးမည်", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            // Pending Bets List
            Box(modifier = Modifier.weight(1f)) {
                if (pendingBets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "ထိုးဂဏန်းများ ထည့်သွင်းပါ (သို့) အမြန်ထိုး သုံးပါ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(pendingBets.reversed(), key = { "${it.number}_${it.amount}_${pendingBets.indexOf(it)}" }) { bet ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().animateItem(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = bet.number,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.width(42.dp)
                                        )
                                        Text(
                                            "=",
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${String.format("%,d", bet.amount)} Ks",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CobaltPrimary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { pendingBets.remove(bet) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Area & Number Pad (Collapsible via toggle)
            AnimatedVisibility(
                visible = showManualKeypad,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        // Dual Display: Number & Amount + Bet Type Toggle Box
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
                                color = if (isNumFocused) CobaltLight.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    width = if (isNumFocused) 2.dp else 1.dp,
                                    color = if (isNumFocused) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (tempNumber.isEmpty()) "ဂဏန်းရိုက်" else tempNumber,
                                        color = if (tempNumber.isEmpty()) MaterialTheme.colorScheme.outline else CobaltPrimary,
                                        fontSize = if (tempNumber.isEmpty()) 12.sp else 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = if (tempNumber.isEmpty()) 0.sp else 2.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Interactive Bet Type Toggle Box
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val cycleList = listOf("ဒဲ့", "R", "ပတ်", "အပူး", "ထိပ်", "ပိတ်", "ပါဝါ", "နက္ခတ်", "ညီကို", "ဘရိတ်")
                                    val nextIdx = (cycleList.indexOf(currentBetType) + 1) % cycleList.size
                                    currentBetType = cycleList[nextIdx]
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentBetType,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = CobaltPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Focused Amount Box (Ks)
                            val isAmtFocused = focusedField == FocusField.AMOUNT
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    focusedField = FocusField.AMOUNT
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAmtFocused) CobaltLight.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    width = if (isAmtFocused) 2.dp else 1.dp,
                                    color = if (isAmtFocused) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = tempAmount,
                                        color = if (isAmtFocused) CobaltPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "Ks",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Compact Remark Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (tempRemark.isEmpty()) {
                                Text(
                                    "မှတ်ချက် (မထည့်လည်းရသည်)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) CobaltPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSel) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
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
                                .padding(vertical = 3.dp),
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
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) CobaltPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, if (isSelected) CobaltPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
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

                        // ── 4x4 Tactile Keypad (Ultra-Realistic 3D Buttons Aligned with 2D) ──
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
                                    bgColor = CobaltPrimary,
                                    contentColor = Color.White,
                                    bevelColor = Color(0xFF1E3A8A),
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
                                    bevelColor = Color(0xFF0E7490),
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
                                    bevelColor = Color(0xFFB91C1C),
                                    modifier = Modifier.weight(1f)
                                ) { backspace() }
                            }

                            // Row 4: [ ရှင်း ] [ 0 ] [ 00 ] [ OK / ထည့် ]
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TactileKeypadButton(
                                    text = "ရှင်း",
                                    subtitle = "Clear",
                                    bgColor = Color(0xFFD97706),
                                    contentColor = Color.White,
                                    bevelColor = Color(0xFF92400E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    handleSpecial("ရှင်းပါ")
                                }
                                TactileKeypadButton("0", modifier = Modifier.weight(1f)) { appendText("0") }
                                TactileKeypadButton("00", modifier = Modifier.weight(1f)) { appendText("00") }
                                TactileKeypadButton(
                                    text = "OK",
                                    subtitle = "ထည့်မည်",
                                    bgColor = CobaltPrimary,
                                    contentColor = Color.White,
                                    bevelColor = Color(0xFF1E3A8A),
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
                                        Text("📋 Clipboard တွင် စာသားတွေ့ရှိပါသည်", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CobaltPrimary)
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
                        Text("✏️ ပြန်လည် ပြင်ဆင်မည်", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            )
        }

        // --- CONFIRM BET DIALOG ---
        if (showBetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showBetConfirmDialog = false },
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "ဘောင်ချာ သိမ်းဆည်းမည်လား?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CobaltLight.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ထိုးသူ :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(customers.find { it.id == selectedCustomer }?.name ?: "", fontWeight = FontWeight.Bold, color = CobaltPrimary, fontSize = 14.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ကွက်ရေ :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${pendingBets.size} ကွက်", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("စုစုပေါင်း ငွေပမာဏ :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("%,d Ks".format(totalAmount), fontWeight = FontWeight.Black, fontSize = 15.sp, color = CobaltPrimary, fontFamily = FontFamily.Monospace)
                                }
                                if (tempRemark.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("မှတ်ချက် :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(tempRemark, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { confirmAndSaveVoucher() },
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ဘောင်ချာ သိမ်းမည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showBetConfirmDialog = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("မလုပ်တော့")
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
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        "ထိုးထားသော စာရင်းများ အားလုံး ရှင်းလင်းမည်လား?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        "လက်ရှိ ရိုက်ထည့်ထားသော ${pendingBets.size} ကွက် (%,d Ks) အားလုံး ပျက်သွားပါမည်။".format(totalAmount),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingBets.clear()
                            clearAll()
                            showClearConfirmDialog = false
                            android.widget.Toast.makeText(context, "စာရင်းများ အားလုံး ရှင်းလင်းပြီးပါပြီ", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("အားလုံး ရှင်းမည်", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("မလုပ်တော့")
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
                        "⚠️ စာကြောင်းအမှား (${pasteErrors.size}) ခု တွေ့ရှိပါသည်",
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
                        "⚠️ ပိတ်ဂဏန်း / ကန့်သတ်ငွေကျော်လွန်မှု",
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
}

// ── Ultra-Realistic Tactile Keypad Button ─────────────────────────────────────
@Composable
fun TactileKeypadButton(
    text: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    bgColor: Color = Color.White,
    contentColor: Color = if (bgColor == Color.White) Color(0xFF0F172A) else Color.White,
    bevelColor: Color = if (bgColor == Color.White) Color(0xFFE2E8F0) else bgColor.copy(alpha = 0.85f),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetY = if (isPressed) 1.5.dp else 0.dp
    val elevation = if (isPressed) 1.dp else 2.dp
    val rDimens = rememberResponsiveDimens()
    val buttonHeight = if (rDimens.isCompact) 42.dp else 46.dp

    Box(
        modifier = modifier
            .height(buttonHeight)
            .padding(horizontal = 1.dp, vertical = 0.5.dp)
            .offset(y = offsetY)
            .shadow(elevation, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColor, bevelColor)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isPressed) 0.15f else 0.5f),
                        Color.Black.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            } else {
                Text(
                    text = text,
                    fontSize = if (text.length > 2) 12.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = contentColor,
                    fontFamily = if (text.all { it.isDigit() }) FontFamily.Monospace else FontFamily.Default,
                    letterSpacing = if (text.all { it.isDigit() }) 1.sp else 0.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}