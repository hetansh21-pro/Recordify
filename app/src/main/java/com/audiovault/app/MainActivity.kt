package com.audiovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.audiovault.app.ui.navigation.AudioVaultNavGraph
import com.audiovault.app.ui.theme.AudioVaultTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AudioVaultTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AudioVaultNavGraph()
                }
            }
        }
    }
}
