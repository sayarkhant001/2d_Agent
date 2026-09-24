package com.twoDLedger.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TwoDLiveResponse(
    val serverTime: String = "",
    val live: TwoDLiveItem? = null,
    val result: List<TwoDResultItem> = emptyList()
)

data class TwoDLiveItem(
    val set: String = "",
    val value: String = "",
    val time: String = "",
    val twod: String = "",
    val date: String = ""
)

data class TwoDResultItem(
    val set: String = "",
    val value: String = "",
    val openTime: String = "",
    val twod: String = "",
    val stockDate: String = ""
)

data class TwoDHistoryDay(
    val date: String = "",
    val child: List<TwoDHistoryChild> = emptyList()
)

data class TwoDHistoryChild(
    val time: String = "",
    val set: String = "",
    val value: String = "",
    val twod: String = ""
)

object TwoDApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getLive(): TwoDLiveResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.thaistock2d.com/live")
            .header("User-Agent", "2D-Ledger-Android/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext TwoDLiveResponse()
            val body = response.body?.string() ?: return@withContext TwoDLiveResponse()
            val json = JSONObject(body)

            var liveItem: TwoDLiveItem? = null
            if (json.has("live") && !json.isNull("live")) {
                val lj = json.getJSONObject("live")
                liveItem = TwoDLiveItem(
                    set = lj.optString("set", ""),
                    value = lj.optString("value", ""),
                    time = lj.optString("time", ""),
                    twod = lj.optString("twod", ""),
                    date = lj.optString("date", "")
                )
            }

            val results = mutableListOf<TwoDResultItem>()
            if (json.has("result") && !json.isNull("result")) {
                val rArr = json.getJSONArray("result")
                for (i in 0 until rArr.length()) {
                    val rObj = rArr.getJSONObject(i)
                    results.add(
                        TwoDResultItem(
                            set = rObj.optString("set", ""),
                            value = rObj.optString("value", ""),
                            openTime = rObj.optString("open_time", ""),
                            twod = rObj.optString("twod", ""),
                            stockDate = rObj.optString("stock_date", "")
                        )
                    )
                }
            }

            TwoDLiveResponse(
                serverTime = json.optString("server_time", ""),
                live = liveItem,
                result = results
            )
        }
    }

    suspend fun getHistory(): List<TwoDHistoryDay> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.thaistock2d.com/2d_result")
            .header("User-Agent", "2D-Ledger-Android/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            val body = response.body?.string() ?: return@withContext emptyList()
            val arr = JSONArray(body)
            val history = mutableListOf<TwoDHistoryDay>()

            for (i in 0 until arr.length()) {
                val dayObj = arr.getJSONObject(i)
                val date = dayObj.optString("date", "")
                val children = mutableListOf<TwoDHistoryChild>()

                if (dayObj.has("child") && !dayObj.isNull("child")) {
                    val cArr = dayObj.getJSONArray("child")
                    for (j in 0 until cArr.length()) {
                        val cObj = cArr.getJSONObject(j)
                        children.add(
                            TwoDHistoryChild(
                                time = cObj.optString("time", ""),
                                set = cObj.optString("set", ""),
                                value = cObj.optString("value", ""),
                                twod = cObj.optString("twod", "")
                            )
                        )
                    }
                }
                history.add(TwoDHistoryDay(date = date, child = children))
            }
            history
        }
    }
}
