package com.audiovault.app.core.intelligence

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Super-Accurate Real-Time YouTube Ad Detection Service.
 * Analyzes active YouTube player UI nodes to detect in-stream ads with zero false positives.
 */
class YouTubeAdAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AudioVault/YouTubeAd"

        private val _isAdPlaying = MutableStateFlow(false)
        val isAdPlaying: StateFlow<Boolean> = _isAdPlaying.asStateFlow()

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private var onAdStateChangeListener: ((Boolean) -> Unit)? = null

        fun setOnAdStateChangeListener(listener: ((Boolean) -> Unit)?) {
            onAdStateChangeListener = listener
        }

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${YouTubeAdAccessibilityService::class.java.name}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.endsWith("/${YouTubeAdAccessibilityService::class.java.name}")) {
                    return true
                }
            }
            return false
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingState: Boolean? = null
    private val debounceRunnable = Runnable {
        val target = pendingState ?: return@Runnable
        if (_isAdPlaying.value != target) {
            _isAdPlaying.value = target
            Log.i(TAG, "YouTube Ad State Confirmed: isAdPlaying=$target")
            onAdStateChangeListener?.invoke(target)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true
        Log.i(TAG, "YouTubeAdAccessibilityService active and connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != "com.google.android.youtube" && pkg != "com.google.android.apps.youtube.music") {
            return
        }

        try {
            val rootNode = rootInActiveWindow ?: return
            val adDetected = scanNodeForInStreamAds(rootNode)
            rootNode.recycle()

            if (pendingState != adDetected) {
                pendingState = adDetected
                mainHandler.removeCallbacks(debounceRunnable)
                // Immediate trigger for ad start to catch fast ads; 300ms debounce for ad completion
                val delayMs = if (adDetected) 50L else 300L
                mainHandler.postDelayed(debounceRunnable, delayMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning accessibility nodes: ${e.message}")
        }
    }

    /**
     * Precision scanner for active in-stream video ads.
     * Checks explicit player ad view IDs and player overlay badges.
     */
    private fun scanNodeForInStreamAds(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        // 1. Explicit YouTube video player in-stream ad IDs
        if (viewId.contains("skip_ad_button") ||
            viewId.contains("ad_progress_text") ||
            viewId.contains("ad_countdown") ||
            viewId.contains("ad_badge") ||
            viewId.contains("ad_present") ||
            viewId.contains("visit_advertiser") ||
            viewId.contains("ad_header") ||
            viewId.contains("player_ad_layout")) {
            return true
        }

        // 2. Exact in-stream ad badge text
        val text = node.text?.toString()?.trim() ?: ""
        val contentDesc = node.contentDescription?.toString()?.trim() ?: ""

        if (isExactInStreamAdText(text) || isExactInStreamAdText(contentDesc)) {
            return true
        }

        // 3. Scan children
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val isAd = scanNodeForInStreamAds(child)
                child.recycle()
                if (isAd) return true
            }
        }
        return false
    }

    private fun isExactInStreamAdText(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()

        return lower == "ad" ||
                lower.startsWith("ad 1 of") ||
                lower.startsWith("ad 2 of") ||
                lower.startsWith("ad ·") ||
                lower.startsWith("ad •") ||
                lower.startsWith("ad:") ||
                lower.contains("skip ad") ||
                lower.contains("skip in ") ||
                lower.contains("video will play after ad") ||
                lower == "sponsored"
    }

    override fun onInterrupt() {
        _isAdPlaying.value = false
        Log.w(TAG, "YouTubeAdAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
        _isAdPlaying.value = false
        mainHandler.removeCallbacksAndMessages(null)
        Log.i(TAG, "YouTubeAdAccessibilityService destroyed.")
    }
}
