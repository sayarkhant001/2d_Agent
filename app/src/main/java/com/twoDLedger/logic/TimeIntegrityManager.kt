package com.twoDLedger.logic

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class TimeIntegrityManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("time_integrity_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_LAST_SERVER_TIME = "last_server_time"
        private const val PREF_LAST_ELAPSED_REALTIME = "last_elapsed_realtime"
        private const val PREF_MAX_OBSERVED_WALL_TIME = "max_observed_wall_time"
        private const val PREF_CLOCK_TAMPERED = "clock_tampered"
        private const val PREF_TAMPER_REASON = "tamper_reason"
        private const val PREF_LAST_SYNC_MMT = "last_sync_mmt"

        val MMT_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Yangon")
    }

    /**
     * Records an authenticated server timestamp in milliseconds UTC.
     * Anchors the monotonic elapsedRealtime to prevent local clock changes from altering expiry.
     */
    fun recordServerTime(serverTimeMs: Long) {
        val nowElapsed = SystemClock.elapsedRealtime()
        val currentMax = prefs.getLong(PREF_MAX_OBSERVED_WALL_TIME, 0L)
        val newMax = maxOf(currentMax, serverTimeMs)

        val mmtFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply {
            timeZone = MMT_ZONE
        }
        val syncStr = mmtFormat.format(Date(serverTimeMs))

        prefs.edit()
            .putLong(PREF_LAST_SERVER_TIME, serverTimeMs)
            .putLong(PREF_LAST_ELAPSED_REALTIME, nowElapsed)
            .putLong(PREF_MAX_OBSERVED_WALL_TIME, newMax)
            .putBoolean(PREF_CLOCK_TAMPERED, false)
            .remove(PREF_TAMPER_REASON)
            .putString(PREF_LAST_SYNC_MMT, syncStr)
            .apply()
    }

    /**
     * Checks if the device clock has been drawn back (clock rollback).
     * If the current wall clock is in the past compared to the highest observed time,
     * it flags tampering and blocks access until internet sync restores real MMT.
     */
    fun isClockTampered(): Boolean {
        if (prefs.getBoolean(PREF_CLOCK_TAMPERED, false)) return true

        val maxObserved = prefs.getLong(PREF_MAX_OBSERVED_WALL_TIME, 0L)
        if (maxObserved > 0L) {
            val currentWallTime = System.currentTimeMillis()
            // If current clock has been rolled back by more than 2 minutes
            if (currentWallTime < maxObserved - 120_000L) {
                flagClockTampered("ဖုန်း၏ နေ့စွဲနှင့် အချိန် နောက်ပြန်ဆုတ်ထားသည်ကို စစ်ဆေးတွေ့ရှိရပါသည် (Clock Rollback Detected)")
                return true
            }
        }
        return false
    }

    fun flagClockTampered(reason: String) {
        prefs.edit()
            .putBoolean(PREF_CLOCK_TAMPERED, true)
            .putString(PREF_TAMPER_REASON, reason)
            .apply()
    }

    fun getTamperReason(): String? = prefs.getString(
        PREF_TAMPER_REASON,
        "ဖုန်း၏ နေ့စွဲနှင့် အချိန် နောက်ပြန်ဆုတ်ထားသည်ကို စစ်ဆေးတွေ့ရှိရပါသည် (Clock Rollback Detected)။"
    )

    /**
     * Computes the current trusted time in milliseconds.
     * Offline: advances monotonically using SystemClock.elapsedRealtime() from anchor,
     * completely unaffected by manual user date/time adjustments in settings.
     */
    fun getCurrentTrustedTimeMs(): Long {
        val lastServerTime = prefs.getLong(PREF_LAST_SERVER_TIME, 0L)
        val lastElapsed = prefs.getLong(PREF_LAST_ELAPSED_REALTIME, 0L)
        val currentElapsed = SystemClock.elapsedRealtime()

        val trustedMs = if (lastServerTime > 0L && currentElapsed >= lastElapsed) {
            // Anchor monotonic advance: user clock tampering has zero effect!
            lastServerTime + (currentElapsed - lastElapsed)
        } else {
            val wallTime = System.currentTimeMillis()
            val maxObserved = prefs.getLong(PREF_MAX_OBSERVED_WALL_TIME, 0L)
            if (maxObserved > 0L && wallTime < maxObserved - 120_000L) {
                flagClockTampered("ဖုန်း၏ နေ့စွဲနှင့် အချိန် နောက်ပြန်ဆုတ်ထားသည်ကို စစ်ဆေးတွေ့ရှိရပါသည် (Clock Rollback Detected)")
                maxObserved
            } else {
                wallTime
            }
        }

        val maxObserved = prefs.getLong(PREF_MAX_OBSERVED_WALL_TIME, 0L)
        if (trustedMs > maxObserved) {
            prefs.edit().putLong(PREF_MAX_OBSERVED_WALL_TIME, trustedMs).apply()
        }

        return trustedMs
    }

    fun getLastSyncMmt(): String? = prefs.getString(PREF_LAST_SYNC_MMT, null)

    /**
     * Syncs real Myanmar Time (MMT) with the server over internet.
     * Clears tampering block upon verification.
     */
    suspend fun syncWithServer(): Boolean = withContext(Dispatchers.IO) {
        // 1. Try 2D live API endpoint
        try {
            val req = Request.Builder()
                .url("https://api.thaistock2d.com/live")
                .header("User-Agent", "2D-Ledger-Android/1.0")
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .build()

            client.newCall(req).execute().use { response ->
                val dateHeader = response.headers.getDate("Date")
                if (dateHeader != null) {
                    recordServerTime(dateHeader.time)
                    return@withContext true
                }
                val body = response.body?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    val serverTimeStr = json.optString("server_time", "")
                    val dtStr = if (serverTimeStr.isNotEmpty()) serverTimeStr else json.optJSONObject("live")?.optString("time", "")
                    if (!dtStr.isNullOrBlank()) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).apply {
                            timeZone = MMT_ZONE
                        }
                        val parsed = sdf.parse(dtStr)
                        if (parsed != null) {
                            recordServerTime(parsed.time)
                            return@withContext true
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. HTTP Date fallback
        try {
            val req = Request.Builder()
                .url("https://www.google.com/generate_204")
                .header("User-Agent", "2D-Ledger-Android/1.0")
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()

            client.newCall(req).execute().use { response ->
                val dateHeader = response.headers.getDate("Date")
                if (dateHeader != null) {
                    recordServerTime(dateHeader.time)
                    return@withContext true
                }
            }
        } catch (_: Exception) {}

        false
    }
}
