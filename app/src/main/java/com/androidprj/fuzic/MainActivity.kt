package com.androidprj.fuzic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.androidprj.fuzic.ui.navigation.FuzicApp
import com.androidprj.fuzic.ui.theme.FuzicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuzicTheme {
                FuzicApp()
            }
        }
    }
}
