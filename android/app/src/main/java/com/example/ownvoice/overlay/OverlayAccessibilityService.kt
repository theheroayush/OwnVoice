package com.example.ownvoice.overlay

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

class OverlayAccessibilityService : AccessibilityService() {

    enum class InjectionOutcome {
        PASTED,
        TEXT_SET,
        CLIPBOARD_ONLY,
        FAILED
    }

    companion object {
        var instance: OverlayAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun getActiveAppPackage(): String = instance?.currentForegroundPackage ?: ""

        fun injectText(text: String): InjectionOutcome {
            val service = instance ?: return InjectionOutcome.FAILED
            return service.performInjection(text)
        }
    }

    private var lastFocusedNode: AccessibilityNodeInfo? = null
    var currentForegroundPackage: String = ""
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: ""
                if (pkg.isNotBlank() && !pkg.contains("ownvoice", ignoreCase = true)) {
                    currentForegroundPackage = pkg
                }
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                try {
                    val pkg = event.packageName?.toString() ?: ""
                    if (pkg.isNotBlank() && !pkg.contains("ownvoice", ignoreCase = true)) {
                        currentForegroundPackage = pkg
                    }
                    val source = event.source ?: return
                    if (source.isEditable || source.isFocused) {
                        lastFocusedNode = AccessibilityNodeInfo.obtain(source)
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    override fun onInterrupt() {
        // Called on accessibility interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        try {
            lastFocusedNode?.recycle()
        } catch (ignored: Exception) {}
        lastFocusedNode = null
    }

    fun performInjection(text: String): InjectionOutcome {
        // Always copy to system clipboard as universal fallback
        try {
            val clipMgr = getSystemService(ClipboardManager::class.java)
            clipMgr?.setPrimaryClip(ClipData.newPlainText("OwnVoice", text))
        } catch (ignored: Exception) {}

        val targetNode = findEditableNode() ?: return InjectionOutcome.CLIPBOARD_ONLY

        try {
            // Strategy 1: Paste via ACTION_PASTE (Preserves existing text, cursor position, works in WhatsApp/Chrome/Notes)
            val pasted = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (pasted) {
                targetNode.recycle()
                return InjectionOutcome.PASTED
            }

            // Strategy 2: ACTION_SET_TEXT (with smart append if text already exists)
            val existingText = targetNode.text?.toString() ?: ""
            val textToInject = if (existingText.isNotBlank()) {
                "$existingText $text"
            } else {
                text
            }

            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToInject)
            }
            val setTextSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            targetNode.recycle()

            if (setTextSuccess) {
                return InjectionOutcome.TEXT_SET
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnVoiceA11y", "Injection error", e)
        }

        return InjectionOutcome.CLIPBOARD_ONLY
    }

    private fun findEditableNode(): AccessibilityNodeInfo? {
        // 1. Try rootInActiveWindow first
        try {
            rootInActiveWindow?.let { root ->
                val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focused != null && (focused.isEditable || focused.isFocused)) {
                    return focused
                }
                val anyFocused = findFocusedEditableInTree(root)
                if (anyFocused != null) return anyFocused
            }
        } catch (ignored: Exception) {}

        // 2. Iterate through all windows on screen (flagRetrieveInteractiveWindows)
        try {
            val currentWindows = windows
            val sortedWindows = currentWindows.sortedByDescending { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            for (window in sortedWindows) {
                val root = window.root ?: continue
                val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focused != null && (focused.isEditable || focused.isFocused)) {
                    return focused
                }
                val anyFocused = findFocusedEditableInTree(root)
                if (anyFocused != null) return anyFocused
            }
        } catch (ignored: Exception) {}

        // 3. Fallback to cached lastFocusedNode if still valid
        try {
            lastFocusedNode?.let { cached ->
                cached.refresh()
                if (cached.isEditable && cached.isVisibleToUser) {
                    return AccessibilityNodeInfo.obtain(cached)
                }
            }
        } catch (ignored: Exception) {}

        // 4. Fallback: Search all application windows for ANY editable node
        try {
            for (window in windows) {
                if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                    val root = window.root ?: continue
                    val firstEditable = findAnyEditableInTree(root)
                    if (firstEditable != null) return firstEditable
                }
            }
        } catch (ignored: Exception) {}

        return null
    }

    private fun findFocusedEditableInTree(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if ((node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true) && (node.isFocused || node.isSelected)) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFocusedEditableInTree(child)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun findAnyEditableInTree(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findAnyEditableInTree(child)
            if (found != null) {
                return found
            }
        }
        return null
    }
}
