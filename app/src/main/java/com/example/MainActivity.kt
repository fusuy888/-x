package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import com.example.ui.MainViewModel
import com.example.ui.NotificationApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle notification click intent on fresh launch
        handleIntent(intent)

        setContent {
            val currentTheme = viewModel.themeMode.collectAsState()
            MyApplicationTheme(themeMode = currentTheme.value) {
                NotificationApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle notification click intent on active/warm background resume
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("notification_text")?.let { text ->
            viewModel.showAlertDetails(text)
        }
    }
}
