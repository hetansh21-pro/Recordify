package com.audiovault.app.core.service

/**
 * Represents the current operational state of AudioVault playback capture.
 */
sealed interface MonitoringState {
    data object Idle : MonitoringState
    data object Monitoring : MonitoringState
    data object WaitingForAudio : MonitoringState
    data class Recording(
        val durationMs: Long,
        val audioLevelDb: Float = -60f
    ) : MonitoringState
    data object Stopping : MonitoringState
    data class Error(val message: String) : MonitoringState
}
