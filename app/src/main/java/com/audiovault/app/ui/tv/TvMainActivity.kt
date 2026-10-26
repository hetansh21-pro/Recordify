package com.audiovault.app.ui.tv

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.audiovault.app.ui.recordings.RecordingsViewModel
import com.audiovault.app.ui.splash.RecordifySplashScreen
import com.audiovault.app.ui.theme.AudioVaultTheme

/**
 * Dedicated 10-foot Television Activity for Recordify TV.
 * Optimized for landscape orientation, Leanback launcher, and TV remote D-Pad control.
 */
class TvMainActivity : ComponentActivity() {

    private val viewModel: RecordingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            AudioVaultTheme {
                var showSplash by remember { mutableStateOf(true) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        RecordifySplashScreen(
                            isTv = true,
                            durationMs = 1200L,
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        RecordifyTvScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                viewModel.togglePlayback()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                viewModel.resumePlayback()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                viewModel.pausePlayback()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                viewModel.playNextTrack()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                viewModel.playPreviousTrack()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                viewModel.seekForward()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                viewModel.seekBackward()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            com.audiovault.app.core.service.PlaybackNotificationManager.activeInstance?.cancelNotification()
        }
    }
}
