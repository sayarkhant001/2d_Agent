package com.twoDLedger.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twoDLedger.data.VoucherWithBets
import com.twoDLedger.logic.BluetoothPrinter
import com.twoDLedger.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchersScreen(
    viewModel: MainViewModel,
    initialCustomerId: Int? = null,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val dimens = rememberResponsiveDimens()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val vouchers by viewModel.vouchersWithCustomer.collectAsStateWithLifecycle()
    val allVouchersWithBets by viewModel.vouchersWithBets.collectAsStateWithLifecycle()
    val footerText by viewModel.voucherFooterText.collectAsStateWithLifecycle()
    val winningNumber by viewModel.winningNumber.collectAsStateWithLifecycle()

    var selectedSessionFilter by remember { mutableStateOf("ALL") } // "ALL", "12:00 PM", "4:30 PM"
    var searchQuery by remember { mutableStateOf("") }
    var voucherToDelete by remember { mutableStateOf<VoucherWithBets?>(null) }

    // Filter normal vouchers (exclude overflow/upper internal records)
    val baseVouchers = remember(allVouchersWithBets, initialCustomerId) {
        (if (initialCustomerId != null) {
            allVouchersWithBets.filter { it.voucher.customerId == initialCustomerId }
        } else {
            allVouchersWithBets
        }).filter {
            !it.voucher.remark.contains("တင်ကွက်") &&
            !it.voucher.remark.contains("overflow", ignoreCase = true)
        }
    }

    // Apply session and search filtering
    val displayedVouchers = remember(baseVouchers, selectedSessionFilter, searchQuery, vouchers) {
        baseVouchers.filter { vwb ->
            val matchSession = when (selectedSessionFilter) {
                "12:00 PM" -> vwb.voucher.session == "12:00 PM"
                "4:30 PM" -> vwb.voucher.session == "4:30 PM"
                else -> true
            }
            if (!matchSession) return@filter false

            if (searchQuery.isBlank()) return@filter true
            val query = searchQuery.trim().lowercase()
            val custName = vouchers.find { it.voucher.id == vwb.voucher.id }?.customer?.name?.lowercase() ?: ""
            val voucherIdStr = vwb.voucher.id.toString()
            val containsBet = vwb.bets.any { it.number.contains(query) }

            custName.contains(query) || voucherIdStr.contains(query) || containsBet
        }
    }

    // Summary statistics
    val totalVoucherCount = displayedVouchers.size
    val totalSalesAmount = displayedVouchers.sumOf { vwb -> vwb.bets.sumOf { it.amount.toLong() } }
    val totalBetsCount = displayedVouchers.sumOf { it.bets.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "2D ဘောင်ချာများ စာရင်း",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "ရောင်းရငွေနှင့် ထိုးကြေး ပြေစာများ ပြန်လည်စစ်ဆေးရန်",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ── Search & Session Filter Header Bar ───────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ထိုးသူအမည်၊ ဘောင်ချာနံပါတ် သို့မဟုတ် ဂဏန်း ရှာရန်...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    // Session Filter Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSessionFilter == "ALL",
                            onClick = { selectedSessionFilter = "ALL" },
                            label = { Text("အားလုံး ($totalVoucherCount)", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = selectedSessionFilter == "12:00 PM",
                            onClick = { selectedSessionFilter = "12:00 PM" },
                            label = { Text("☀️ နေ့လယ် ၁၂:၀၀", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEF3C7),
                                selectedLabelColor = Color(0xFF92400E)
                            )
                        )
                        FilterChip(
                            selected = selectedSessionFilter == "4:30 PM",
                            onClick = { selectedSessionFilter = "4:30 PM" },
                            label = { Text("🌙 ညနေ ၄:၃၀", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDBEAFE),
                                selectedLabelColor = Color(0xFF1E40AF)
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Streamlined Metrics Summary Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ဘောင်ချာ", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalVoucherCount စောင်", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ရောင်းရငွေ စုစုပေါင်း", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%,d", totalSalesAmount)} Ks", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF059669))
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ထိုးကြေးအကွက်", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalBetsCount ကွက်", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // ── Vouchers List ────────────────────────────────────────────────
            if (displayedVouchers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "ရှာဖွေမှုနှင့် ကိုက်ညီသော ဘောင်ချာ မရှိပါ" else "သိမ်းဆည်းထားသော 2D ဘောင်ချာ မရှိသေးပါ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = dimens.responsiveDp(10.dp, 14.dp, 18.dp), vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedVouchers, key = { it.voucher.id }) { voucherWithBets ->
                        val customerName = vouchers.find { it.voucher.id == voucherWithBets.voucher.id }?.customer?.name ?: "မိမိ"
                        val voucherTotal = voucherWithBets.bets.sumOf { it.amount }
                        val isWon = winningNumber.length == 2 && voucherWithBets.bets.any { it.number == winningNumber }
                        val wonAmount = if (isWon) {
                            voucherWithBets.bets.filter { it.number == winningNumber }.sumOf { it.amount } * 80L
                        } else 0L

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                if (isWon) 1.5.dp else 1.dp,
                                if (isWon) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Voucher Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = customerName,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val dateStr = SimpleDateFormat("yyyy-MM-dd • HH:mm", Locale.getDefault()).format(Date(voucherWithBets.voucher.timestamp))
                                            Text(
                                                text = dateStr,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Voucher ID and Session Badge
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (voucherWithBets.voucher.session == "12:00 PM") Color(0xFFFEF3C7) else Color(0xFFDBEAFE)
                                        ) {
                                            Text(
                                                text = voucherWithBets.voucher.session,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp,
                                                color = if (voucherWithBets.voucher.session == "12:00 PM") Color(0xFF92400E) else Color(0xFF1E40AF),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "#${voucherWithBets.voucher.id}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Winner highlight banner
                                if (isWon) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                                Text("🎉 ပေါက်ဂဏန်း ($winningNumber) ပါဝင်ပါသည်", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF92400E))
                                            }
                                            Text("လျော်: ${String.format("%,d", wonAmount)} Ks", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFFB45309))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Bets Table
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Header
                                        Surface(color = Color(0xFFDCFCE7), modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("စဉ်   ၂ လုံးဂဏန်း", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF065F46))
                                                Text("ထိုးကြေး (Ks)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF065F46))
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFF6EE7B7), thickness = 0.8.dp)

                                        // Bet lines
                                        voucherWithBets.bets.forEachIndexed { idx, bet ->
                                            val isBetWinner = winningNumber.length == 2 && bet.number == winningNumber
                                            val bg = if (isBetWinner) Color(0xFFFEF3C7) else if (idx % 2 == 0) Color.White else Color(0xFFF0FDF4)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(bg)
                                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${idx + 1}.",
                                                        fontSize = 11.5.sp,
                                                        color = Color(0xFF64748B),
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.width(26.dp)
                                                    )
                                                    Text(
                                                        text = bet.number,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (isBetWinner) Color(0xFFB45309) else Color(0xFF0F172A)
                                                    )
                                                    if (isBetWinner) {
                                                        Spacer(Modifier.width(6.dp))
                                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFDE68A)) {
                                                            Text("ပေါက်", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${String.format("%,d", bet.amount)} Ks",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isBetWinner) Color(0xFFB45309) else Color(0xFF059669)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFF6EE7B7), thickness = 0.8.dp)

                                        // Total Footer
                                        Surface(color = Color(0xFFF8FAFC), modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("စုစုပေါင်း (${voucherWithBets.bets.size} ကွက်):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text(
                                                    text = "${String.format("%,d", voucherTotal)} Ks",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF059669)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (footerText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = footerText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons Row: Copy, Print, Share, Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Copy
                                    IconButton(
                                        onClick = {
                                            val text = buildVoucherText(voucherWithBets, customerName, footerText)
                                            clipboardManager.setText(AnnotatedString(text))
                                            Toast.makeText(context, "ဘောင်ချာ ကူးယူပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    // Bluetooth Print
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val success = BluetoothPrinter.printVoucher(context, voucherWithBets, customerName, footerText)
                                                if (success) {
                                                    Toast.makeText(context, "ပရင့် ထုတ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "ပရင်တာ ချိတ်ဆက်မှု မအောင်မြင်ပါ", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    // Share
                                    IconButton(
                                        onClick = {
                                            val text = buildVoucherText(voucherWithBets, customerName, footerText)
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, text)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "ဘောင်ချာ ပေးပို့ရန်"))
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = { voucherToDelete = voucherWithBets },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (voucherToDelete != null) {
            AlertDialog(
                onDismissRequest = { voucherToDelete = null },
                title = { Text("ဘောင်ချာ ဖျက်ရန် သေချာပါသလား?", fontWeight = FontWeight.Bold) },
                text = { Text("ဘောင်ချာ #${voucherToDelete!!.voucher.id} နှင့် ၎င်းတွင် ပါဝင်သော ထိုးကြေးများကို အပြီးတိုင် ဖျက်ပစ်ပါမည်။") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteVoucher(voucherToDelete!!.voucher.id)
                            voucherToDelete = null
                            Toast.makeText(context, "ဘောင်ချာ ဖျက်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("ဖျက်မည်")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { voucherToDelete = null }) {
                        Text("မလုပ်တော့ပါ")
                    }
                }
            )
        }
    }
}

private fun buildVoucherText(
    vwb: VoucherWithBets,
    customerName: String,
    footer: String
): String = buildString {
    appendLine("========================")
    appendLine("      2D ဘောင်ချာ      ")
    appendLine("========================")
    appendLine("ဘောင်ချာအမှတ်-#${vwb.voucher.id}  အချိန် : ${vwb.voucher.session}")
    appendLine("အမည်       : $customerName")
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(vwb.voucher.timestamp))
    appendLine("ရက်စွဲ      : $dateStr")
    appendLine("------------------------")
    appendLine("စဉ်   ဂဏန်း    ထိုးငွေ")
    appendLine("------------------------")
    vwb.bets.forEachIndexed { i, bet ->
        val idx = "${i + 1}.".padEnd(4)
        val num = bet.number.padEnd(6)
        val amt = "${bet.amount}".padStart(8)
        appendLine("$idx $num $amt Ks")
    }
    appendLine("------------------------")
    val total = vwb.bets.sumOf { it.amount }
    appendLine("စုစုပေါင်း : ${total} Ks (${vwb.bets.size} ကွက်)")
    appendLine("========================")
    if (footer.isNotBlank()) {
        appendLine(footer)
        appendLine("========================")
    }
}
