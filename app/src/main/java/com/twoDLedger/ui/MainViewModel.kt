package com.twoDLedger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twoDLedger.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class LedgerExposure(
    val number: String,
    val totalBetAmount: Int,
    val exportedAmount: Int,
    val netHeldAmount: Int,
    val overflowAmount: Int
)

class MainViewModel(private val repository: LotteryRepository, private val prefs: android.content.SharedPreferences) : ViewModel() {

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vouchersWithCustomer: StateFlow<List<VoucherWithCustomer>> = repository.allVouchersWithCustomer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val vouchersWithBets: StateFlow<List<VoucherWithBets>> = repository.allVouchersWithBets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBets: StateFlow<List<Bet>> = repository.allBets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val numberExposures: StateFlow<List<NumberExposure>> = repository.numberExposures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val bannedNumbers: StateFlow<List<BannedNumber>> = repository.allBannedNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedVouchers: StateFlow<List<VoucherWithCustomer>> = repository.archivedVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedBatchSummaries: StateFlow<List<com.twoDLedger.data.ArchiveBatchSummary>> = repository.archivedBatchSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExportRecords: StateFlow<List<ExportRecordWithNumbers>> = repository.allExportRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        var currentBatch = MutableStateFlow(prefs.getInt("currentBatch", 1))
    val currentSession = MutableStateFlow(prefs.getString("currentSession", "12:00 PM") ?: "12:00 PM")
    val indicator900 = MutableStateFlow("")
    val winningNumber1200 = MutableStateFlow("")
    val indicator1400 = MutableStateFlow("")
    val winningNumber1630 = MutableStateFlow("")
    val live2DData = MutableStateFlow<com.twoDLedger.network.TwoDLiveItem?>(null)
    val isFetchingLive = MutableStateFlow(false)
    val isFetchingHistory = MutableStateFlow(false)
    val winningHistory = repository.winningHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteVoucher(voucherId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVoucher(voucherId)
        }
    }

    fun setSession(session: String) {
        currentSession.value = session
        prefs.edit().putString("currentSession", session).apply()
        loadWinningNumber()
    }

    private var livePollingJob: kotlinx.coroutines.Job? = null

    fun startLivePolling() {
        if (livePollingJob?.isActive == true) return
        livePollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    fetchLive2DDirect()
                } catch (_: Exception) {}

                // Priority timing: fast polling around 12:00 PM and 4:30 PM draw intervals
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val min = cal.get(Calendar.MINUTE)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY

                val isPeakTime = isWeekday && (
                    (hour == 11 && min >= 57) || (hour == 12 && min <= 5) ||
                    (hour == 16 && min in 27..35)
                )
                val isMarketHours = isWeekday && hour in 9..17
                val delayMs = when {
                    isPeakTime -> 3_000L      // 3 seconds exact & fastest fetching during results draw
                    isMarketHours -> 15_000L  // 15 seconds during normal trading hours
                    else -> 60_000L           // 1 minute outside market hours
                }
                kotlinx.coroutines.delay(delayMs)
            }
        }
    }

    private suspend fun fetchLive2DDirect() {
        val resp = com.twoDLedger.network.TwoDApiClient.getLive()
        live2DData.value = resp.live
        resp.result.forEach { item ->
            when {
                item.openTime.startsWith("09") || item.openTime.startsWith("11") -> {
                    if (item.twod.isNotBlank() && item.twod != "--") indicator900.value = item.twod
                }
                item.openTime.startsWith("12") -> {
                    if (item.twod.isNotBlank() && item.twod != "--") {
                        winningNumber1200.value = item.twod
                        saveWinningNumber(item.twod, "12:00 PM")
                    }
                }
                item.openTime.startsWith("14") || item.openTime.startsWith("15") -> {
                    if (item.twod.isNotBlank() && item.twod != "--") indicator1400.value = item.twod
                }
                item.openTime.startsWith("16") -> {
                    if (item.twod.isNotBlank() && item.twod != "--") {
                        winningNumber1630.value = item.twod
                        saveWinningNumber(item.twod, "4:30 PM")
                    }
                }
            }
        }
    }

    fun fetchLive2D() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isFetchingLive.value = true
                fetchLive2DDirect()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingLive.value = false
            }
        }
    }

    fun fetch30DayHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isFetchingHistory.value = true
                val list = com.twoDLedger.network.TwoDApiClient.getHistory()
                val entities = list.take(30).map { day ->
                    var n9 = ""
                    var n12 = ""
                    var n14 = ""
                    var n16 = ""
                    var s12 = ""
                    var v12 = ""
                    var s16 = ""
                    var v16 = ""
                    day.child.forEach { c ->
                        when {
                            c.time.startsWith("09") || c.time.startsWith("11") -> n9 = c.twod
                            c.time.startsWith("12") -> { n12 = c.twod; s12 = c.set; v12 = c.value }
                            c.time.startsWith("14") || c.time.startsWith("15") -> n14 = c.twod
                            c.time.startsWith("16") -> { n16 = c.twod; s16 = c.set; v16 = c.value }
                        }
                    }
                    WinningHistory(
                        date = day.date,
                        num900 = n9,
                        num1200 = n12,
                        num1400 = n14,
                        num1630 = n16,
                        set1200 = s12,
                        val1200 = v12,
                        set1630 = s16,
                        val1630 = v16
                    )
                }
                repository.insertWinningHistory(entities)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingHistory.value = false
            }
        }
    }

    val appPassword = MutableStateFlow("")
    val voucherFooterText = MutableStateFlow("ထွက်လျော်မည်။")
    val printerSettings = MutableStateFlow("")
    val bannedNumberEvent = kotlinx.coroutines.flow.MutableSharedFlow<Boolean>()
    val bannedLimitNotificationEvent = kotlinx.coroutines.flow.MutableSharedFlow<List<com.twoDLedger.data.BannedLimitRemoval>>()
    var brakeLimit = MutableStateFlow(3000)

    // Winning number declared for the current batch (persisted per batch key)
    val winningNumber = MutableStateFlow("")

    init {
        loadWinningNumber()
        brakeLimit.value = prefs.getInt("brakeLimit", 3000)
        startLivePolling()
        viewModelScope.launch {
            currentBatch.collect { batch ->
                prefs.edit().putInt("currentBatch", batch).apply()
                loadWinningNumber()
            }
        }
        viewModelScope.launch {
            repository.purgeOverflowArtifacts()
            ensureDefaultCustomer()
        }
    }

    private suspend fun ensureDefaultCustomer() {
        try {
            val list = repository.allCustomers.first()
            val hasNormal = list.any { !it.name.contains("တင်ကွက်") && !it.name.contains("overflow", ignoreCase = true) }
            if (!hasNormal) {
                repository.insertCustomer(Customer(name = "မိမိ (ကိုယ်တိုင်)", commissionRate = 0.0, multiplier = 80))
            }
        } catch (_: Exception) {}
    }

    fun saveWinningNumber(number: String, session: String = currentSession.value, batch: Int = currentBatch.value) {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        if (session == "12:00 PM") {
            winningNumber1200.value = number
            prefs.edit().putString("winning1200_$today", number).putString("winning1200", number).apply()
        } else {
            winningNumber1630.value = number
            prefs.edit().putString("winning1630_$today", number).putString("winning1630", number).apply()
        }
        if (session == currentSession.value) {
            winningNumber.value = number
        }
        prefs.edit().putString("winningNumber_$batch", number).apply()
    }

    fun clearWinningNumber(session: String = currentSession.value, batch: Int = currentBatch.value) {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        if (session == "12:00 PM") {
            winningNumber1200.value = ""
            prefs.edit().remove("winning1200_$today").remove("winning1200").apply()
        } else {
            winningNumber1630.value = ""
            prefs.edit().remove("winning1630_$today").remove("winning1630").apply()
        }
        if (session == currentSession.value) {
            winningNumber.value = ""
        }
        prefs.edit().remove("winningNumber_$batch").apply()
    }

    fun clearWinningNumber(batch: Int) {
        clearWinningNumber(currentSession.value, batch)
    }

    fun isSessionDeclared(session: String = currentSession.value): Boolean {
        val num = if (session == "12:00 PM") winningNumber1200.value else winningNumber1630.value
        return num.length == 2
    }

    fun isBatchDeclared(batch: Int = currentBatch.value): Boolean = isSessionDeclared()

    fun loadWinningNumber() {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val saved1200 = prefs.getString("winning1200_$today", "") ?: prefs.getString("winning1200", "") ?: ""
        val saved1630 = prefs.getString("winning1630_$today", "") ?: prefs.getString("winning1630", "") ?: ""
        if (saved1200.isNotBlank() && winningNumber1200.value.isBlank()) winningNumber1200.value = saved1200
        if (saved1630.isNotBlank() && winningNumber1630.value.isBlank()) winningNumber1630.value = saved1630

        winningNumber.value = if (currentSession.value == "12:00 PM") {
            winningNumber1200.value.ifBlank { saved1200 }
        } else {
            winningNumber1630.value.ifBlank { saved1630 }
        }
    }

    fun saveBrakeLimit(value: Int) {
        brakeLimit.value = value
        prefs.edit().putInt("brakeLimit", value).apply()
    }

    // ── Per-batch multipliers (saved when ပေါက်သီး is declared) ──────────────
    val savedExactMult = MutableStateFlow(80.0)
    val savedPermMult  = MutableStateFlow(10.0)
    val savedNearMult  = MutableStateFlow(10.0)

    fun saveMultipliers(exact: Double, tuwt: Double, near: Double = tuwt, batch: Int = currentBatch.value) {
        if (batch == currentBatch.value) {
            savedExactMult.value = exact
            savedPermMult.value  = tuwt
            savedNearMult.value  = near
        }
        prefs.edit()
            .putFloat("exactMult_$batch", exact.toFloat())
            .putFloat("permMult_$batch",  tuwt.toFloat())
            .putFloat("nearMult_$batch",  near.toFloat())
            .apply()
    }

    fun getWinningNumberForBatch(batch: Int): String =
        prefs.getString("winningNumber_$batch", "") ?: ""

    fun getMultipliersForBatch(batch: Int): Triple<Double, Double, Double> = Triple(
        prefs.getFloat("exactMult_$batch", 80f).toDouble(),
        prefs.getFloat("permMult_$batch",  10f).toDouble(),
        prefs.getFloat("nearMult_$batch",  10f).toDouble()
    )

    // ── Per-customer per-batch paid amount (persisted) ────────────────────────
    fun getPaidForBatch(customerId: Int, batchNumber: Int): Double =
        prefs.getFloat("paid_${customerId}_$batchNumber", 0f).toDouble()

    fun setPaidForBatch(customerId: Int, batchNumber: Int, amount: Double) {
        prefs.edit().putFloat("paid_${customerId}_$batchNumber", amount.toFloat()).apply()
    }

    /** Flow of all vouchers (incl. archived) for a specific batch number */
    fun getVouchersWithBetsByBatch(batchNumber: Int): kotlinx.coroutines.flow.Flow<List<com.twoDLedger.data.VoucherWithBets>> =
        repository.getVouchersWithBetsByBatch(batchNumber)


    val ledgerExposures: StateFlow<List<LedgerExposure>> = kotlinx.coroutines.flow.combine(
        vouchersWithBets,
        allExportRecords,
        currentSession,
        brakeLimit
    ) { vouchers, exports, session, brake ->
        val sessionVouchers = vouchers.filter {
            it.voucher.session == session || (it.voucher.session.isBlank() && session == "12:00 PM")
        }
        val sessionExports = exports.filter {
            it.record.session == session || (it.record.session.isBlank() && session == "12:00 PM")
        }

        // Total bets per number (gross)
        val betMap = mutableMapOf<String, Int>()
        sessionVouchers.forEach { vb ->
            vb.bets.forEach { bet ->
                betMap[bet.number] = (betMap[bet.number] ?: 0) + bet.amount
            }
        }

        // All exported amounts per number (both overflow and under-brake exports)
        val exportMap = mutableMapOf<String, Int>()
        sessionExports.forEach { eb ->
            eb.numbers.forEach { num ->
                exportMap[num.number] = (exportMap[num.number] ?: 0) + num.amount
            }
        }

        // Overflow-specific exported amounts per number
        val overflowExportMap = mutableMapOf<String, Int>()
        sessionExports
            .filter { it.record.type.contains("Overflow", ignoreCase = true) || it.record.type.contains("ဘရိတ်ကျော်") || it.record.type.contains("တင်ကွက်") }
            .forEach { eb ->
                eb.numbers.forEach { num ->
                    overflowExportMap[num.number] = (overflowExportMap[num.number] ?: 0) + num.amount
                }
            }

        val results = mutableListOf<LedgerExposure>()
        betMap.forEach { (number, grossAmount) ->
            val exported = exportMap[number] ?: 0
            val netHeld = grossAmount - exported
            val alreadyExportedOverflow = overflowExportMap[number] ?: 0
            val rawOverflow = if (grossAmount > brake) grossAmount - brake else 0
            val overflow = maxOf(0, rawOverflow - alreadyExportedOverflow)
            if (grossAmount > 0) {
                results.add(LedgerExposure(number, grossAmount, exported, netHeld, overflow))
            }
        }
        results.sortedByDescending { it.netHeldAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportOverflow(onComplete: ((Int) -> Unit)? = null) {
        val currentExposures = ledgerExposures.value
        val toExport = currentExposures.filter { it.overflowAmount > 0 }
        if (toExport.isEmpty()) return

        viewModelScope.launch {
            val totalAmount = toExport.sumOf { it.overflowAmount }
            val record = ExportRecord(
                batchNumber = currentBatch.value,
                session = currentSession.value,
                type = "ဘရိတ်ကျော် တင်ကွက်",
                totalAmount = totalAmount
            )
            val recordId = repository.insertExportRecord(record).toInt()

            val exportNumbers = toExport.map {
                ExportedNumber(exportRecordId = recordId, number = it.number, amount = it.overflowAmount)
            }
            repository.insertExportedNumbers(exportNumbers)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(recordId)
            }
            // NOTE: Do NOT insert a Voucher here — that would inflate betMap and
            // prevent overflowAmount from clearing after export.
        }
    }


    init {
        viewModelScope.launch {
            repository.purgeOverflowArtifacts()
        }
        appPassword.value = prefs.getString("appPassword", "") ?: ""
        voucherFooterText.value = prefs.getString("voucherFooterText", "ထွက်လျော်မည်။") ?: "ထွက်လျော်မည်။"
        printerSettings.value = prefs.getString("printerSettings", "") ?: ""
        brakeLimit.value = prefs.getInt("brakeLimit", 3000)
        winningNumber.value = prefs.getString("winningNumber_${currentBatch.value}", "") ?: ""
    }

    fun updateAppPassword(password: String) {
        appPassword.value = password
        prefs.edit().putString("appPassword", password).apply()
    }

    fun updateVoucherFooterText(text: String) {
        voucherFooterText.value = text
        prefs.edit().putString("voucherFooterText", text).apply()
    }

    fun updatePrinterSettings(text: String) {
        printerSettings.value = text
        prefs.edit().putString("printerSettings", text).apply()
    }

    fun addBannedNumber(number: String, amountLimit: Int = 0) {
        viewModelScope.launch {
            val existing = bannedNumbers.value.find { it.number == number }
            if (existing != null) {
                repository.updateBannedNumber(existing.copy(amountLimit = amountLimit))
            } else {
                repository.insertBannedNumber(BannedNumber(number = number, amountLimit = amountLimit))
            }
        }
    }

    fun updateBannedNumber(bannedNumber: BannedNumber) {
        viewModelScope.launch {
            repository.updateBannedNumber(bannedNumber)
        }
    }

    fun deleteBannedNumber(bannedNumber: BannedNumber) {
        viewModelScope.launch {
            repository.deleteBannedNumber(bannedNumber)
        }
    }
    fun resetAndArchive() {
        viewModelScope.launch {
            repository.archiveAndReset(currentBatch.value - 2)
            currentBatch.value = currentBatch.value + 1
        }
    }

    fun exportUnderBrake() {
        val currentExposures = ledgerExposures.value
        val toExport = currentExposures.filter { (it.netHeldAmount - it.overflowAmount) > 0 }
        if (toExport.isEmpty()) return

        viewModelScope.launch {
            val totalAmount = toExport.sumOf { it.netHeldAmount - it.overflowAmount }
            val record = ExportRecord(batchNumber = currentBatch.value, type = "ဘရိတ်အောက်ငွေ (Under-Brake)", totalAmount = totalAmount)
            val recordId = repository.insertExportRecord(record).toInt()
            
            val exportNumbers = toExport.map { 
                ExportedNumber(exportRecordId = recordId, number = it.number, amount = it.netHeldAmount - it.overflowAmount)
            }
            repository.insertExportedNumbers(exportNumbers)
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun addCustomer(name: String, commissionRate: Double, multiplier: Int) {
        viewModelScope.launch {
            repository.insertCustomer(Customer(name = name, commissionRate = commissionRate, multiplier = multiplier))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun getActiveBatchGrossBetsMap(): Map<String, Int> {
        val batch = currentBatch.value
        val map = mutableMapOf<String, Int>()
        vouchersWithBets.value
            .filter { it.voucher.batchNumber == batch && !it.voucher.isArchived }
            .forEach { vb ->
                vb.bets.forEach { b ->
                    map[b.number] = (map[b.number] ?: 0) + b.amount
                }
            }
        return map
    }

    fun validateAndFilterBetsWithBannedLimits(
        incomingBets: List<Bet>,
        pendingSessionAmounts: Map<String, Int> = emptyMap()
    ): Pair<List<Bet>, List<com.twoDLedger.data.BannedLimitRemoval>> {
        val bannedMap = bannedNumbers.value.associateBy { it.number }
        if (bannedMap.isEmpty()) {
            return Pair(incomingBets, emptyList())
        }

        val dbTotals = getActiveBatchGrossBetsMap()
        val accumulatedAmounts = dbTotals.toMutableMap()
        pendingSessionAmounts.forEach { (num, amt) ->
            accumulatedAmounts[num] = (accumulatedAmounts[num] ?: 0) + amt
        }

        val validBets = mutableListOf<Bet>()
        val removals = mutableListOf<com.twoDLedger.data.BannedLimitRemoval>()

        for (bet in incomingBets) {
            val banned = bannedMap[bet.number]
            if (banned == null) {
                validBets.add(bet)
                accumulatedAmounts[bet.number] = (accumulatedAmounts[bet.number] ?: 0) + bet.amount
                continue
            }

            val limit = banned.amountLimit
            val currentAccum = accumulatedAmounts[bet.number] ?: 0

            if (limit <= 0) {
                // Case 1: Completely Banned (0 Ks allowed)
                removals.add(
                    com.twoDLedger.data.BannedLimitRemoval(
                        number = bet.number,
                        attemptedAmount = bet.amount,
                        limitAmount = 0,
                        currentBetTotal = currentAccum,
                        acceptedAmount = 0,
                        removedAmount = bet.amount,
                        reason = "လုံးဝပိတ်ထားသော ဂဏန်းဖြစ်ပါသည်"
                    )
                )
            } else {
                // Case 2: Capped with Limit Amount
                val remainingAllowed = (limit - currentAccum).coerceAtLeast(0)
                if (remainingAllowed <= 0) {
                    // Limit already reached or exceeded
                    removals.add(
                        com.twoDLedger.data.BannedLimitRemoval(
                            number = bet.number,
                            attemptedAmount = bet.amount,
                            limitAmount = limit,
                            currentBetTotal = currentAccum,
                            acceptedAmount = 0,
                            removedAmount = bet.amount,
                            reason = "ကန့်သတ်ငွေ %,d ကျပ် ပြည့်သွားပါသည် (လက်ရှိ: %,d ကျပ်)".format(limit, currentAccum)
                        )
                    )
                } else if (bet.amount <= remainingAllowed) {
                    validBets.add(bet)
                    accumulatedAmounts[bet.number] = currentAccum + bet.amount
                } else {
                    val excess = bet.amount - remainingAllowed
                    validBets.add(bet.copy(amount = remainingAllowed))
                    accumulatedAmounts[bet.number] = currentAccum + remainingAllowed
                    removals.add(
                        com.twoDLedger.data.BannedLimitRemoval(
                            number = bet.number,
                            attemptedAmount = bet.amount,
                            limitAmount = limit,
                            currentBetTotal = currentAccum,
                            acceptedAmount = remainingAllowed,
                            removedAmount = excess,
                            reason = "ကန့်သတ်ငွေ %,d ကျပ် ပြည့်ရန် %,d ကျပ်သာ လက်ခံပြီး ပိုငွေ %,d ကျပ် ဖယ်ထုတ်လိုက်ပါသည်".format(
                                limit, remainingAllowed, excess
                            )
                        )
                    )
                }
            }
        }

        return Pair(validBets, removals)
    }

    fun addVoucherAndBets(customerId: Int, time: String, rawInput: String, remark: String = "") {
        viewModelScope.launch {
            val bets = parseBets(rawInput)
            val (validBets, removals) = validateAndFilterBetsWithBannedLimits(bets)
            if (removals.isNotEmpty()) {
                bannedNumberEvent.emit(true)
                bannedLimitNotificationEvent.emit(removals)
            }
            if (validBets.isNotEmpty()) {
                val totalAmount = validBets.sumOf { it.amount }
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = dateFormat.format(Date())
                val voucher = Voucher(customerId = customerId, batchNumber = currentBatch.value, session = currentSession.value, date = date, time = time, totalAmount = totalAmount, remark = remark)
                repository.insertVoucherWithBets(voucher, validBets)
            }
        }
    }


    fun addVoucherWithBetList(customerId: Int, time: String, bets: List<Bet>, remark: String = "") {
        viewModelScope.launch {
            val (validBets, removals) = validateAndFilterBetsWithBannedLimits(bets)
            if (removals.isNotEmpty()) {
                bannedNumberEvent.emit(true)
                bannedLimitNotificationEvent.emit(removals)
            }
            if (validBets.isNotEmpty()) {
                val totalAmount = validBets.sumOf { it.amount }
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = dateFormat.format(Date())
                val voucher = Voucher(customerId = customerId, batchNumber = currentBatch.value, session = currentSession.value, date = date, time = time, totalAmount = totalAmount, remark = remark)
                repository.insertVoucherWithBets(voucher, validBets)
            }
        }
    }


    private fun convertBurmeseToEnglishDigits(input: String): String { return input.map { char -> when (char) { '၀' -> '0'; '၁' -> '1'; '၂' -> '2'; '၃' -> '3'; '၄' -> '4'; '၅' -> '5'; '၆' -> '6'; '၇' -> '7'; '၈' -> '8'; '၉' -> '9'; else -> char } }.joinToString("") }

private val VM_KS_REGEX                = Regex("(?i)ks")
private val VM_SPACES_SEPARATORS_REGEX = Regex("\\s*([.,/+\\-_=:])\\s*")
private val VM_SPACES_R_REGEX          = Regex("\\s*(?i)r\\s*")
private val VM_SPACES_SPLIT_REGEX      = Regex("\\s+")
private val VM_BLOCK_TAIL_REGEX        = Regex("([-:/.,_=]+)?(\\d+)(?:R(\\d+))?$")
private val VM_NUMBER_CHUNKS_REGEX     = Regex("[.,/+\\-_:]+")

    fun parseBets(input: String): List<Bet> {
        val lines = input.lines().filter { it.isNotBlank() }
        val bets = mutableListOf<Bet>()
        for (line in lines) {
            val pairs = com.twoDLedger.logic.TwoDBetParser.parseLine(line)
            for ((num, amt) in pairs) {
                if (amt > 0) {
                    bets.add(Bet(voucherId = 0, number = num, amount = amt))
                }
            }
        }
        return bets
    }

}


