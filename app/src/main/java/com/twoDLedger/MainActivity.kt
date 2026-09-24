package com.twoDLedger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.twoDLedger.data.AppDatabase
import com.twoDLedger.data.LotteryRepository
import com.twoDLedger.ui.AppNavigation
import com.twoDLedger.ui.NotificationPermissionHandler
import com.twoDLedger.ui.UpdateDialogHandler
import com.twoDLedger.ui.MainViewModel
import com.twoDLedger.ui.MainViewModelFactory
import com.twoDLedger.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(this)
        val repository = LotteryRepository(database.lotteryDao())
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(repository, prefs))
                    
                    NotificationPermissionHandler()
                    UpdateDialogHandler(owner = "sayarkhant001", repo = "3d_Agent")
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

