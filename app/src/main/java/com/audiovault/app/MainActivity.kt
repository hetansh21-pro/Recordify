package com.audiovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.audiovault.app.ui.navigation.AudioVaultNavGraph
import com.audiovault.app.ui.splash.RecordifySplashScreen
import com.audiovault.app.ui.theme.AudioVaultTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Automatically route to 10-foot TV experience on Android TV / Google TV
        if (com.audiovault.app.ui.tv.isTvDevice(this)) {
            startActivity(android.content.Intent(this, com.audiovault.app.ui.tv.TvMainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            AudioVaultTheme {
                var showSplash by remember { mutableStateOf(true) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        RecordifySplashScreen(
                            isTv = false,
                            durationMs = 1100L,
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        AudioVaultNavGraph()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            com.audiovault.app.core.service.PlaybackNotificationManager.activeInstance?.cancelNotification()
        }
    }
}
