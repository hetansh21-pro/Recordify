package com.audiovault.app.core.intelligence

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-performance Accessibility Service that analyzes YouTube and YouTube Music UI in real time
 * to detect in-stream ads, sponsored breaks, and skip-ad buttons.
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true
        Log.i(TAG, "YouTubeAdAccessibilityService connected and actively monitoring.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != "com.google.android.youtube" && pkg != "com.google.android.apps.youtube.music") {
            return
        }

        try {
            val rootNode = rootInActiveWindow ?: return
            val adDetected = scanNodeForAds(rootNode)
            rootNode.recycle()

            if (_isAdPlaying.value != adDetected) {
                _isAdPlaying.value = adDetected
                Log.d(TAG, "YouTube Ad State Changed: isAdPlaying=$adDetected")
                onAdStateChangeListener?.invoke(adDetected)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning accessibility nodes: ${e.message}")
        }
    }

    private fun scanNodeForAds(node: AccessibilityNodeInfo): Boolean {
        // 1. Check View Resource ID
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("skip_ad") || 
            viewId.contains("ad_badge") || 
            viewId.contains("ad_countdown") ||
            viewId.contains("ad_present") ||
            viewId.contains("ad_break") ||
            viewId.contains("visit_advertiser") ||
            viewId.contains("ad_header")) {
            return true
        }

        // 2. Check Text / Content Description
        val text = node.text?.toString()?.trim() ?: ""
        val contentDesc = node.contentDescription?.toString()?.trim() ?: ""

        if (isAdTextMatch(text) || isAdTextMatch(contentDesc)) {
            return true
        }

        // 3. Recursively check child nodes
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = scanNodeForAds(child)
                child.recycle()
                if (found) return true
            }
        }
        return false
    }

    private fun isAdTextMatch(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()

        if (lower == "ad" || 
            lower.startsWith("ad ") || 
            lower.startsWith("ad ·") || 
            lower.startsWith("ad •") || 
            lower.startsWith("ad:") || 
            lower.contains("skip ad") || 
            lower.contains("skip in ") || 
            lower.contains("visit advertiser") || 
            lower.contains("sponsored") ||
            lower.contains("promoted")) {
            return true
        }
        return false
    }

    override fun onInterrupt() {
        _isAdPlaying.value = false
        Log.w(TAG, "YouTubeAdAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
        _isAdPlaying.value = false
        Log.i(TAG, "YouTubeAdAccessibilityService destroyed.")
    }
}
