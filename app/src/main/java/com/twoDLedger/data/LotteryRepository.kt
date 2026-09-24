package com.twoDLedger.data

import kotlinx.coroutines.flow.Flow

class LotteryRepository(private val lotteryDao: LotteryDao) {
    val allCustomers: Flow<List<Customer>> = lotteryDao.getAllCustomers()
    val archivedVouchers: Flow<List<VoucherWithCustomer>> = lotteryDao.getArchivedVouchersWithCustomer()
    val archivedBatchSummaries: Flow<List<ArchiveBatchSummary>> = lotteryDao.getArchivedBatchSummaries()

    val allVouchersWithCustomer: Flow<List<VoucherWithCustomer>> = lotteryDao.getAllVouchersWithCustomer()
    val allVouchersWithBets: Flow<List<VoucherWithBets>> = lotteryDao.getAllVouchersWithBets()
    val allBets: Flow<List<Bet>> = lotteryDao.getAllBets()
    val numberExposures: Flow<List<NumberExposure>> = lotteryDao.getNumberExposures()
    val allBannedNumbers: Flow<List<BannedNumber>> = lotteryDao.getAllBannedNumbers()
    val allExportRecords: Flow<List<ExportRecordWithNumbers>> = lotteryDao.getAllExportRecords()
    val winningHistory: Flow<List<WinningHistory>> = lotteryDao.getWinningHistory()

    fun getVouchersWithBetsBySession(session: String): Flow<List<VoucherWithBets>> {
        return lotteryDao.getVouchersWithBetsBySession(session)
    }

    fun getNumberExposuresBySession(session: String): Flow<List<NumberExposure>> {
        return lotteryDao.getNumberExposuresBySession(session)
    }

    fun getVouchersWithBetsByBatch(batchNumber: Int): Flow<List<VoucherWithBets>> =
        lotteryDao.getVouchersWithBetsByBatch(batchNumber)

    suspend fun updateCustomer(customer: Customer) {
        lotteryDao.updateCustomer(customer)
    }

    suspend fun insertCustomer(customer: Customer): Long {
        return lotteryDao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        lotteryDao.deleteCustomer(customer)
    }

    suspend fun insertVoucher(voucher: Voucher): Long {
        return lotteryDao.insertVoucher(voucher)
    }

    suspend fun insertBets(bets: List<Bet>) {
        lotteryDao.insertBets(bets)
    }

    suspend fun insertVoucherWithBets(voucher: Voucher, bets: List<Bet>) {
        val voucherId = lotteryDao.insertVoucher(voucher).toInt()
        bets.chunked(500).forEach { chunk ->
            val betsWithVoucherId = chunk.map { it.copy(voucherId = voucherId) }
            lotteryDao.insertBets(betsWithVoucherId)
        }
    }

    suspend fun insertExportRecord(record: ExportRecord): Long {
        return lotteryDao.insertExportRecord(record)
    }

    suspend fun insertExportedNumbers(numbers: List<ExportedNumber>) {
        lotteryDao.insertExportedNumbers(numbers)
    }

    suspend fun insertBannedNumber(bannedNumber: BannedNumber) {
        lotteryDao.insertBannedNumber(bannedNumber)
    }

    suspend fun updateBannedNumber(bannedNumber: BannedNumber) {
        lotteryDao.updateBannedNumber(bannedNumber)
    }

    suspend fun deleteBannedNumber(bannedNumber: BannedNumber) {
        lotteryDao.deleteBannedNumber(bannedNumber)
    }

    suspend fun archiveAndReset(thresholdBatch: Int) {
        lotteryDao.archiveAllVouchers()
        lotteryDao.archiveAllExportRecords()
        lotteryDao.deleteOldArchives(thresholdBatch)
        lotteryDao.deleteOldExportArchives(thresholdBatch)
        lotteryDao.deleteOrphanedBets()
        lotteryDao.deleteOrphanedExportedNumbers()
    }

    suspend fun getVoucherWithBets(voucherId: Int): VoucherWithBets? {
        return lotteryDao.getVoucherWithBets(voucherId)
    }

    suspend fun getVoucherDetails(voucherId: Int): VoucherWithBets? {
        return lotteryDao.getVoucherWithBets(voucherId)
    }

    suspend fun purgeOverflowArtifacts() {
        lotteryDao.purgeOverflowCustomers()
        lotteryDao.purgeOverflowVouchers()
        lotteryDao.deleteOrphanedBets()
    }

    suspend fun insertWinningHistory(items: List<WinningHistory>) {
        lotteryDao.insertWinningHistory(items)
    }
}
