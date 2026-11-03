package com.meditime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.meditime.app.ui.AppRoot
import com.meditime.app.ui.theme.MediTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediTimeTheme {
                AppRoot()
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_TODAY = "extra_open_today"
    }
}
