package com.twoDLedger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.twoDLedger.data.ExportRecordWithNumbers
import com.twoDLedger.data.VoucherWithBets
import com.twoDLedger.ui.theme.CobaltPrimary
import com.twoDLedger.ui.theme.CobaltLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SessionHoldCalc(
    val sessionName: String,
    val winningNumber: String,
    val isDeclared: Boolean,
    val totalBetPlaced: Int,
    val totalExported: Int,
    val totalHeld: Int,
    val winningBetsCount: Int,
    val winningBetAmount: Int,
    val winningPayout: Long,
    val netProfit: Long
)

@Composable
fun TwoDDailyHoldDialog(
    allVouchersWithBets: List<VoucherWithBets>,
    allExportRecords: List<ExportRecordWithNumbers>,
    brakeLimit: Int,
    win1200: String,
    win1630: String,
    currentBatch: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val todayStr = remember {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val dw = com.twoDLedger.logic.TwoDMarketCalendar.getBurmeseDayOfWeek(Date())
        "$d ($dw)"
    }

    // Helper to calculate hold metrics for a specific session
    fun computeSession(sessionKey: String, sessionName: String, winNum: String): SessionHoldCalc {
        val sVouchers = allVouchersWithBets.filter {
            (it.voucher.session == sessionKey || (it.voucher.session.isBlank() && sessionKey == "12:00 PM")) &&
            it.voucher.batchNumber == currentBatch &&
            !it.voucher.isArchived &&
            !it.voucher.remark.contains("တင်ကွက်") && !it.voucher.remark.contains("overflow", ignoreCase = true)
        }
        val sExports = allExportRecords.filter {
            (it.record.session == sessionKey || (it.record.session.isBlank() && sessionKey == "12:00 PM")) &&
            it.record.batchNumber == currentBatch &&
            !it.record.isArchived
        }

        val betMap = mutableMapOf<String, Int>()
        sVouchers.forEach { vb ->
            vb.bets.forEach { b ->
                betMap[b.number] = (betMap[b.number] ?: 0) + b.amount
            }
        }

        val exportMap = mutableMapOf<String, Int>()
        sExports.forEach { eb ->
            eb.numbers.forEach { n ->
                exportMap[n.number] = (exportMap[n.number] ?: 0) + n.amount
            }
        }

        var totalBetPlaced = 0
        var totalExported = 0
        var totalHeld = 0
        var winCount = 0
        var winBetAmt = 0

        betMap.forEach { (num, gross) ->
            totalBetPlaced += gross
            val exp = exportMap[num] ?: 0
            totalExported += exp
            val remaining = maxOf(0, gross - exp)
            val held = if (brakeLimit > 0) minOf(remaining, brakeLimit) else remaining
            totalHeld += held

            if (winNum.length == 2 && num == winNum) {
                winBetAmt += held
                winCount++
            }
        }

        val isDeclared = winNum.isNotBlank() && winNum.length == 2 && winNum != "--"
        val payout = if (isDeclared) winBetAmt.toLong() * 80L else 0L
        val netProfit = if (isDeclared) totalHeld.toLong() - payout else totalHeld.toLong()

        return SessionHoldCalc(
            sessionName = sessionName,
            winningNumber = if (isDeclared) winNum else "-",
            isDeclared = isDeclared,
            totalBetPlaced = totalBetPlaced,
            totalExported = totalExported,
            totalHeld = totalHeld,
            winningBetsCount = winCount,
            winningBetAmount = winBetAmt,
            winningPayout = payout,
            netProfit = netProfit
        )
    }

    val morning = remember(allVouchersWithBets, allExportRecords, brakeLimit, win1200, currentBatch) {
        computeSession("12:00 PM", "မနက်ပိုင်း (၁၂:၀၀)", win1200)
    }
    val evening = remember(allVouchersWithBets, allExportRecords, brakeLimit, win1630, currentBatch) {
        computeSession("4:30 PM", "ညနေပိုင်း (၄:၃၀)", win1630)
    }

    val totalHeldDay = morning.totalHeld + evening.totalHeld
    val totalPayoutDay = morning.winningPayout + evening.winningPayout
    val totalNetProfitDay = morning.netProfit + evening.netProfit
    val bothDeclared = morning.isDeclared && evening.isDeclared

    val summarySlipText = buildString {
        appendLine("===== တစ်နေ့တာ သိမ်းငွေ ရက်ချုပ် စာရင်း =====")
        appendLine("နေ့စွဲ: $todayStr (အကြိမ်: $currentBatch)")
        appendLine("ဘရိတ်ကန့်သတ်: %,d ကျပ်".format(brakeLimit))
        appendLine("----------------------------------")
        appendLine("မနက်ပိုင်း (၁၂:၀၀)")
        appendLine("  ပေါက်ဂဏန်း: ${morning.winningNumber}")
        appendLine("  ထိုးကြေး စုစုပေါင်း: %,d ကျပ်".format(morning.totalBetPlaced))
        appendLine("  ဒိုင်တင်ငွေ: %,d ကျပ်".format(morning.totalExported))
        appendLine("  ကိုယ့်လက်ထဲ သိမ်းငွေ: %,d ကျပ်".format(morning.totalHeld))
        if (morning.isDeclared) {
            appendLine("  ပေါက်သီး လျော်ကြေး: %,d ကျပ်".format(morning.winningPayout))
            appendLine("  မနက် အသားတင်: %s%,d ကျပ်".format(if (morning.netProfit >= 0) "+" else "", morning.netProfit))
        } else {
            appendLine("  အခြေအနေ: ပေါက်ဂဏန်း မထွက်သေးပါ")
        }
        appendLine("----------------------------------")
        appendLine("ညနေပိုင်း (၄:၃၀)")
        appendLine("  ပေါက်ဂဏန်း: ${evening.winningNumber}")
        appendLine("  ထိုးကြေး စုစုပေါင်း: %,d ကျပ်".format(evening.totalBetPlaced))
        appendLine("  ဒိုင်တင်ငွေ: %,d ကျပ်".format(evening.totalExported))
        appendLine("  ကိုယ့်လက်ထဲ သိမ်းငွေ: %,d ကျပ်".format(evening.totalHeld))
        if (evening.isDeclared) {
            appendLine("  ပေါက်သီး လျော်ကြေး: %,d ကျပ်".format(evening.winningPayout))
            appendLine("  ညနေ အသားတင်: %s%,d ကျပ်".format(if (evening.netProfit >= 0) "+" else "", evening.netProfit))
        } else {
            appendLine("  အခြေအနေ: ပေါက်ဂဏန်း မထွက်သေးပါ")
        }
        appendLine("==================================")
        appendLine("တစ်နေ့တာ စုစုပေါင်း ရက်ချုပ်")
        appendLine("  စုစုပေါင်း သိမ်းငွေ: %,d ကျပ်".format(totalHeldDay))
        appendLine("  စုစုပေါင်း လျော်ကြေး: %,d ကျပ်".format(totalPayoutDay))
        appendLine("  တစ်နေ့တာ အသားတင်: %s%,d ကျပ်".format(if (totalNetProfitDay >= 0) "+" else "", totalNetProfitDay))
        appendLine("==================================")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ရက်ချုပ် (သိမ်းငွေ စာရင်းချုပ်)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = CobaltPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "$todayStr • အကြိမ်: $currentBatch",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "ပိတ်မည်", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)

                // Morning Session Card
                SessionCard(calc = morning)

                // Evening Session Card
                SessionCard(calc = evening)

                // Whole Day Grand Summary
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CobaltLight,
                    border = BorderStroke(1.dp, CobaltPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("တစ်နေ့တာ သိမ်းငွေ စုစုပေါင်း", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CobaltPrimary)
                            Text("%,d ကျပ်".format(totalHeldDay), fontWeight = FontWeight.Black, fontSize = 14.sp, color = CobaltPrimary)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("တစ်နေ့တာ ပေါက်သီး လျော်ကြေး", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFDC2626))
                            Text("%,d ကျပ်".format(totalPayoutDay), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFDC2626))
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = CobaltPrimary.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 2.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("တစ်နေ့တာ အသားတင် အမြတ်/အရှုံး", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            val isWin = totalNetProfitDay >= 0
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isWin) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "%s%,d ကျပ်".format(if (isWin) "+" else "", totalNetProfitDay),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (isWin) Color(0xFF047857) else Color(0xFFB91C1C),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Actions: Copy & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(summarySlipText))
                            android.widget.Toast.makeText(context, "ရက်ချုပ် စာရင်း ကော်ပီကူးပြီးပါပြီ", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CobaltPrimary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ကော်ပီကူးမည်", color = CobaltPrimary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ပိတ်မည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(calc: SessionHoldCalc) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(calc.sessionName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (calc.isDeclared) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (calc.isDeclared) "ပေါက်ဂဏန်း: ${calc.winningNumber}" else "ပေါက်ဂဏန်း မထွက်သေး",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (calc.isDeclared) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ထိုးကြေး စုစုပေါင်း", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("%,d ကျပ်".format(calc.totalBetPlaced), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ဒိုင်တင်ငွေ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("%,d ကျပ်".format(calc.totalExported), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ကိုယ့်လက်ထဲ သိမ်းငွေ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CobaltPrimary)
                Text("%,d ကျပ်".format(calc.totalHeld), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = CobaltPrimary)
            }

            if (calc.isDeclared) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ပေါက်သီး လျော်ကြေး (80ဆ)", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
                    Text("%,d ကျပ်".format(calc.winningPayout), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("အသားတင် အမြတ်/အရှုံး", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val isWin = calc.netProfit >= 0
                    Text(
                        text = "%s%,d ကျပ်".format(if (isWin) "+" else "", calc.netProfit),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWin) Color(0xFF047857) else Color(0xFFB91C1C)
                    )
                }
            }
        }
    }
}
