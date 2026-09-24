package com.twoDLedger

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("2D စာရင်း ဆော့ဝဲလ်", appName)
  }

  @Test
  fun `default batch of the app is 1`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.twoDLedger.data.AppDatabase.getDatabase(context)
    val repository = com.twoDLedger.data.LotteryRepository(database.lotteryDao())
    val prefs = context.getSharedPreferences("test_prefs_batch_1", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    val viewModel = com.twoDLedger.ui.MainViewModel(repository, prefs)
    assertEquals(1, viewModel.currentBatch.value)
  }
}


