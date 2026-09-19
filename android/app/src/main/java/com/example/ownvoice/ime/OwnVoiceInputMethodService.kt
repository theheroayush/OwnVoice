package com.example.ownvoice.ime

import android.Manifest
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.audio.AudioRecordStreamer
import com.example.ownvoice.core.PersonalLexiconManager
import com.example.ownvoice.core.SnippetEngine
import com.example.ownvoice.core.TonePromptManager
import com.example.ownvoice.ime.model.ActionKeyType
import com.example.ownvoice.ime.model.ClipboardItem
import com.example.ownvoice.ime.model.KeyboardLayoutMode
import com.example.ownvoice.ime.model.KeystrokeEvent
import com.example.ownvoice.ime.model.KeystrokeType
import com.example.ownvoice.ime.ui.KeyboardState
import com.example.ownvoice.ime.ui.KeyboardView
import com.example.ownvoice.network.BridgeClient
import com.example.ownvoice.network.GeminiRestClient
import kotlinx.coroutines.*

class OwnVoiceInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        var instance: OwnVoiceInputMethodService? = null
            private set

        fun isKeyboardActive(): Boolean = instance?.currentInputConnection != null

        fun injectText(text: String): Boolean {
            val service = instance ?: return false
            val ic = service.currentInputConnection ?: return false
            return try {
                ic.commitText(text, 1)
            } catch (e: Exception) {
                false
            }
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var audioRecorder: AudioRecordStreamer
    private lateinit var geminiClient: GeminiRestClient
    private lateinit var snippetEngine: SnippetEngine
    private lateinit var bridgeClient: BridgeClient
    private lateinit var personalLexicon: PersonalLexiconManager
    private var vibrator: Vibrator? = null
    private var lastBackspaceHapticTime = 0L

    private var keyboardState by mutableStateOf(KeyboardState.IDLE)
    private var statusMessage by mutableStateOf("")
    private var currentAmplitude by mutableStateOf(0.0f)
    private var currentEffectiveTone by mutableStateOf("smart_flow")
    private var lastInjectedLength = 0
    private var lastInjectedText = ""
    private var isBridgeActive by mutableStateOf(false)
    private var isAutoVadActive by mutableStateOf(false)
    private var lastSpeechTime = 0L
    private var speechDetectedInSession = false
    private var dismissJob: Job? = null
    private var currentClipboardText by mutableStateOf("")

    private var currentLayoutMode by mutableStateOf(KeyboardLayoutMode.QWERTY)
    private var currentActionKeyType by mutableStateOf(ActionKeyType.ENTER)
    private val clipboardItems = mutableStateListOf<ClipboardItem>()

    private fun updateClipboardRingBuffer() {
        try {
            val clipboard = getSystemService(ClipboardManager::class.java)
            val clipData = clipboard?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0)?.coerceToText(this)?.toString()?.trim() ?: ""
                if (text.isNotBlank()) {
                    currentClipboardText = text
                    var isSensitive = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        isSensitive = clipData.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) ?: false
                    }
                    if (clipboardItems.none { it.text == text }) {
                        clipboardItems.add(0, ClipboardItem(text = text, isSensitive = isSensitive))
                        if (clipboardItems.size > 20) {
                            val unpinnedIdx = clipboardItems.indexOfLast { !it.isPinned }
                            if (unpinnedIdx != -1) clipboardItems.removeAt(unpinnedIdx)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnVoiceIME", "updateClipboardRingBuffer error", e)
        }
    }

    private fun pasteClipboard() {
        try {
            val clipboard = getSystemService(ClipboardManager::class.java)
            val clipData = clipboard?.primaryClip
            val clipText = if (clipData != null && clipData.itemCount > 0) {
                clipData.getItemAt(0)?.coerceToText(this)?.toString() ?: ""
            } else ""

            if (clipText.isNotBlank()) {
                currentInputConnection?.commitText(clipText, 1)
                lastInjectedLength = clipText.length
                lastInjectedText = clipText
                if (isBridgeActive) {
                    val app = application as OwnVoiceApplication
                    val pcIp = app.secureConfig.desktopBridgeIp
                    if (pcIp.isNotBlank()) {
                        serviceScope.launch {
                            bridgeClient.sendToDesktop(pcIp, clipText, token = app.secureConfig.desktopBridgeToken)
                        }
                    }
                }
                performHaptic()
                statusMessage = if (isBridgeActive) "Pasted to PC & Phone" else "Pasted"
            } else {
                statusMessage = "Clipboard is empty"
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnVoiceIME", "pasteClipboard error", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val app = application as OwnVoiceApplication
        audioRecorder = AudioRecordStreamer(16000)
        geminiClient = GeminiRestClient(app.secureConfig)
        snippetEngine = SnippetEngine(app.secureConfig)
        bridgeClient = BridgeClient(app.secureConfig, this)
        personalLexicon = PersonalLexiconManager(this)
        isBridgeActive = app.secureConfig.isUseForPcEnabled && app.secureConfig.desktopBridgeIp.isNotBlank()
        vibrator = getSystemService(Vibrator::class.java)

        // Load pinned clips from SecureConfig
        app.secureConfig.getPinnedClips().forEach { pinnedText ->
            clipboardItems.add(ClipboardItem(text = pinnedText, isPinned = true))
        }
        updateClipboardRingBuffer()

        // Register system clipboard primary clip listener
        try {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard?.addPrimaryClipChangedListener {
                updateClipboardRingBuffer()
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnVoiceIME", "Clipboard listener error", e)
        }

        // Observe amplitude flow for live waveform & Auto VAD
        serviceScope.launch {
            audioRecorder.amplitudeFlow.collect { amp ->
                currentAmplitude = amp
                if (isAutoVadActive && keyboardState == KeyboardState.RECORDING) {
                    val now = System.currentTimeMillis()
                    if (amp > 0.08f) {
                        speechDetectedInSession = true
                        lastSpeechTime = now
                    } else if (speechDetectedInSession && (now - lastSpeechTime > 1200)) {
                        speechDetectedInSession = false
                        stopRecordingAndTranscribe()
                    }
                }
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false
    override fun onEvaluateInputViewShown(): Boolean = true
    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean = true

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        val rootLayout = android.widget.FrameLayout(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewTreeLifecycleOwner(this@OwnVoiceInputMethodService)
            setViewTreeViewModelStoreOwner(this@OwnVoiceInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@OwnVoiceInputMethodService)
        }

        val composeView = ComposeView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@OwnVoiceInputMethodService))
            setViewTreeLifecycleOwner(this@OwnVoiceInputMethodService)
            setViewTreeViewModelStoreOwner(this@OwnVoiceInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@OwnVoiceInputMethodService)

            setContent {
                MaterialTheme {
                    val app = application as OwnVoiceApplication
                    KeyboardView(
                        state = keyboardState,
                        amplitude = currentAmplitude,
                        statusMessage = statusMessage,
                        activeTone = currentEffectiveTone,
                        snippets = app.secureConfig.getSnippets(),
                        lastInjectedText = lastInjectedText,
                        clipboardText = currentClipboardText,
                        clipboardItems = clipboardItems,
                        layoutMode = currentLayoutMode,
                        actionKeyType = currentActionKeyType,
                        isBridgeActive = isBridgeActive,
                        isAutoVadActive = isAutoVadActive,
                        desktopBridgeIp = app.secureConfig.desktopBridgeIp,
                        pcClipboardText = bridgeClient.latestPcClipboardText.collectAsState().value,
                        bridgeClient = bridgeClient,
                        onPastePcClipboard = { textToPaste ->
                            currentInputConnection?.commitText(textToPaste, 1)
                            performHaptic()
                        },
                        onPasteClick = { pasteClipboard() },
                        onToggleRecording = { 
                            try {
                                toggleRecording()
                            } catch (e: Exception) {
                                android.util.Log.e("OwnVoiceIME", "toggleRecording error", e)
                            }
                        },
                        onToneCycle = {
                            val toneList = listOf("smart_flow", "search", "translate_hindi", "chat", "formal_email", "code")
                            val currentIndex = toneList.indexOf(currentEffectiveTone)
                            currentEffectiveTone = if (currentIndex == -1 || currentIndex == toneList.lastIndex) {
                                toneList.first()
                            } else {
                                toneList[currentIndex + 1]
                            }
                            performHaptic()
                        },
                        onToneSelect = { selectedTone ->
                            currentEffectiveTone = selectedTone
                            performHaptic()
                        },
                        onClearClick = {
                            try {
                                val ic = currentInputConnection
                                val selected = ic?.getSelectedText(0)
                                if (!selected.isNullOrEmpty()) {
                                    ic.commitText("", 1)
                                } else {
                                    ic?.deleteSurroundingText(2000, 500)
                                }
                                lastInjectedLength = 0
                                lastInjectedText = ""
                                if (isBridgeActive && app.secureConfig.isPcAirTypingEnabled) {
                                    bridgeClient.enqueueKeystroke(KeystrokeEvent(KeystrokeType.CLEAR, ""))
                                }
                                performHaptic()
                            } catch (e: Exception) {
                                android.util.Log.e("OwnVoiceIME", "onClearClick error", e)
                            }
                        },
                        onNewLineClick = {
                            try {
                                currentInputConnection?.commitText("\n", 1)
                                if (isBridgeActive && app.secureConfig.isPcAirTypingEnabled) {
                                    bridgeClient.enqueueEnter()
                                }
                                performHaptic()
                            } catch (e: Exception) {
                                android.util.Log.e("OwnVoiceIME", "onNewLineClick error", e)
                            }
                        },
                        onToggleBridge = {
                            val pcIp = app.secureConfig.desktopBridgeIp
                            if (pcIp.isBlank()) {
                                statusMessage = "Searching PC on Wi-Fi..."
                                serviceScope.launch {
                                    val found = bridgeClient.discoverLocalDesktops(timeoutMs = 1500)
                                    if (found.isNotEmpty()) {
                                        val first = found.first()
                                        app.secureConfig.desktopBridgeIp = first.ip
                                        app.secureConfig.desktopBridgeName = first.name
                                        if (first.token.isNotBlank()) {
                                            app.secureConfig.desktopBridgeToken = first.token
                                        }
                                        app.secureConfig.isUseForPcEnabled = true
                                        isBridgeActive = true
                                        performHaptic()
                                        statusMessage = "Paired: ${first.name}!"
                                    } else {
                                        statusMessage = "No PC found. Open OwnVoice to link."
                                    }
                                }
                            } else {
                                isBridgeActive = !isBridgeActive
                                app.secureConfig.isUseForPcEnabled = isBridgeActive
                                performHaptic()
                                val displayName = app.secureConfig.desktopBridgeName.ifBlank { pcIp }
                                statusMessage = if (isBridgeActive) "PC Air-Typing ON: $displayName" else "PC Bridge OFF"
                            }
                        },
                        onToggleAutoVad = {
                            isAutoVadActive = !isAutoVadActive
                            performHaptic()
                            statusMessage = if (isAutoVadActive) "Auto VAD Mode ON" else "Auto VAD Mode OFF"
                        },
                        onSnippetClick = { trigger ->
                            try {
                                val expansion = snippetEngine.expand(trigger)
                                currentInputConnection?.commitText(expansion, 1)
                                lastInjectedText = expansion
                                lastInjectedLength = expansion.length
                                if (isBridgeActive) {
                                    val pcIp = app.secureConfig.desktopBridgeIp
                                    if (pcIp.isNotBlank()) {
                                        serviceScope.launch {
                                            bridgeClient.sendToDesktop(pcIp, expansion, token = app.secureConfig.desktopBridgeToken)
                                        }
                                    }
                                }
                                performHaptic()
                            } catch (e: Exception) {
                                android.util.Log.e("OwnVoiceIME", "onSnippetClick error", e)
                            }
                        },
                        onBackspaceClick = {
                            try {
                                val ic = currentInputConnection
                                if (ic != null) {
                                    val selected = ic.getSelectedText(0)
                                    if (!selected.isNullOrEmpty()) {
                                        ic.commitText("", 1)
                                    } else {
                                        val deleted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            ic.deleteSurroundingTextInCodePoints(1, 0)
                                        } else {
                                            ic.deleteSurroundingText(1, 0)
                                        }
                                        if (!deleted) {
                                            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                                        }
                                    }
                                } else {
                                    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                                }
                                if (lastInjectedLength > 0) lastInjectedLength--

                                if (isBridgeActive && app.secureConfig.isPcAirTypingEnabled) {
                                    bridgeClient.enqueueBackspace()
                                }

                                val now = SystemClock.uptimeMillis()
                                if (now - lastBackspaceHapticTime >= 70L) {
                                    performHaptic(light = true)
                                    lastBackspaceHapticTime = now
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("OwnVoiceIME", "onBackspaceClick error", e)
                            }
                        },
                        onEnterClick = {
                            sendAction()
                            if (isBridgeActive && app.secureConfig.isPcAirTypingEnabled) {
                                bridgeClient.enqueueEnter()
                            }
                        },
                        onSpaceClick = {
                            try {
                                currentInputConnection?.commitText(" ", 1)
                                if (isBridgeActive && app.secureConfig.isPcAirTypingEnabled) {
                                    bridgeClient.enqueueSpace()
                                }
                                performHaptic()
                            } catch (e: Exception) {
                                android.util.Log.e("OwnVoiceIME", "onSpaceClick error", e)
                            }
                        },
                        onSwitchKeyboardClick = {
                            try {
                                var switched = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    switched = switchToPreviousInputMethod()
                                }
                                if (!switched) {
                                    val imm = getSystemService(InputMethodManager::class.java)
                                    imm?.showInputMethodPicker()
                                }
                            } catch (e: Exception) {
                                val imm = getSystemService(InputMethodManager::class.java)
                                imm?.showInputMethodPicker()
                            }
                        },
                        onStateChange = { newState ->
                            dismissJob?.cancel()
                            keyboardState = newState
                        },
                        onShapeText = { transformType ->
                            shapeInjectedText(transformType)
                        },
                        onRetryClick = {
                            retryRecording()
                        },
                        onSendClick = {
                            sendAction()
                        },
                        onTypeChar = { char ->
                            try {
                                currentInputConnection?.commitText(char, 1)
                                if (isBridgeActive && app.secureConfig.isPcAirTypingEnabled) {
                                    bridgeClient.enqueueChar(char)
                                }
                                performHaptic()
                                if (keyboardState == KeyboardState.RESULT) {
                                    dismissJob?.cancel()
                                    keyboardState = KeyboardState.IDLE
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("OwnVoiceIME", "onTypeChar error", e)
                            }
                        },
                        onSpacebarGlide = { stepDelta ->
                            handleSpacebarGlide(stepDelta)
                        },
                        onBackspaceScrub = { wordCount ->
                            handleBackspaceScrub(wordCount)
                        },
                        onBackspaceScrubCommit = { wordCount ->
                            handleBackspaceScrubCommit(wordCount)
                        },
                        onWandTransform = { optionId ->
                            executeMagicWand(optionId)
                        },
                        onTogglePinClip = { clipId ->
                            val item = clipboardItems.find { it.id == clipId }
                            if (item != null) {
                                val idx = clipboardItems.indexOf(item)
                                clipboardItems[idx] = item.copy(isPinned = !item.isPinned)
                                val pinnedTexts = clipboardItems.filter { it.isPinned }.map { it.text }
                                app.secureConfig.savePinnedClips(pinnedTexts)
                                performHaptic()
                            }
                        },
                        onDeleteClip = { clipId ->
                            clipboardItems.removeAll { it.id == clipId }
                            val pinnedTexts = clipboardItems.filter { it.isPinned }.map { it.text }
                            app.secureConfig.savePinnedClips(pinnedTexts)
                            performHaptic()
                        },
                        onSendClipToPc = { clipText ->
                            if (isBridgeActive) {
                                val pcIp = app.secureConfig.desktopBridgeIp
                                if (pcIp.isNotBlank()) {
                                    serviceScope.launch {
                                        bridgeClient.sendToDesktop(pcIp, clipText, token = app.secureConfig.desktopBridgeToken)
                                    }
                                    performHaptic()
                                    statusMessage = "Sent clip to PC"
                                }
                            }
                        }
                    )
                }
            }
        }
        rootLayout.addView(composeView)
        return rootLayout
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        if (lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED ||
            lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val app = application as OwnVoiceApplication
        isBridgeActive = app.secureConfig.isUseForPcEnabled && app.secureConfig.desktopBridgeIp.isNotBlank()

        // Adaptive InputType Detection
        val inputType = info?.inputType ?: 0
        val inputClass = inputType and EditorInfo.TYPE_MASK_CLASS
        val inputVariation = inputType and EditorInfo.TYPE_MASK_VARIATION

        currentLayoutMode = when {
            inputClass == EditorInfo.TYPE_CLASS_NUMBER ||
            inputClass == EditorInfo.TYPE_CLASS_PHONE ||
            inputVariation == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD -> KeyboardLayoutMode.NUMERIC_PAD

            inputVariation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            inputVariation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> KeyboardLayoutMode.EMAIL

            else -> KeyboardLayoutMode.QWERTY
        }

        // Adaptive Action Key Detection
        val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        currentActionKeyType = when (action) {
            EditorInfo.IME_ACTION_SEARCH -> ActionKeyType.SEARCH
            EditorInfo.IME_ACTION_SEND -> ActionKeyType.SEND
            EditorInfo.IME_ACTION_GO -> ActionKeyType.GO
            EditorInfo.IME_ACTION_NEXT -> ActionKeyType.NEXT
            EditorInfo.IME_ACTION_DONE -> ActionKeyType.DONE
            else -> ActionKeyType.ENTER
        }

        val targetPkg = info?.packageName ?: ""
        currentEffectiveTone = if (app.secureConfig.isAutoContextToneEnabled && targetPkg.isNotBlank()) {
            TonePromptManager.detectModeForPackage(targetPkg)
        } else {
            app.secureConfig.dictationMode
        }

        dismissJob?.cancel()
        keyboardState = KeyboardState.IDLE
        statusMessage = ""
        lastInjectedLength = 0
        lastInjectedText = ""
        updateClipboardRingBuffer()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        dismissJob?.cancel()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        if (audioRecorder.isCurrentlyRecording()) {
            audioRecorder.cancelRecording()
            keyboardState = KeyboardState.IDLE
        }
    }

    private fun handleSpacebarGlide(stepDelta: Int) {
        try {
            val ic = currentInputConnection ?: return
            if (stepDelta < 0) {
                val textBefore = ic.getTextBeforeCursor(1000, 0)
                if (!textBefore.isNullOrEmpty()) {
                    val newPos = (textBefore.length + stepDelta).coerceAtLeast(0)
                    ic.setSelection(newPos, newPos)
                } else {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
                }
            } else if (stepDelta > 0) {
                val textBefore = ic.getTextBeforeCursor(1000, 0) ?: ""
                val textAfter = ic.getTextAfterCursor(1000, 0) ?: ""
                val totalLength = textBefore.length + textAfter.length
                val newPos = (textBefore.length + stepDelta).coerceAtMost(totalLength)
                ic.setSelection(newPos, newPos)
            }
            performHaptic(light = true)
        } catch (e: Exception) {
            if (stepDelta < 0) sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
            else sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
        }
    }

    private fun handleBackspaceScrub(wordCount: Int) {
        try {
            val ic = currentInputConnection ?: return
            val textBefore = ic.getTextBeforeCursor(600, 0)?.toString() ?: ""
            if (wordCount <= 0) {
                // Restore cursor to end
                ic.setSelection(textBefore.length, textBefore.length)
            } else {
                if (textBefore.isNotEmpty()) {
                    val tokens = textBefore.trimEnd().split(Regex("\\s+"))
                    val wordsToSelect = tokens.takeLast(wordCount).joinToString(" ")
                    val selectLen = minOf(textBefore.length, wordsToSelect.length + (textBefore.length - textBefore.trimEnd().length))
                    val startPos = textBefore.length - selectLen
                    ic.setSelection(startPos, textBefore.length)
                    performHaptic(light = true)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnVoiceIME", "handleBackspaceScrub error", e)
        }
    }

    private fun handleBackspaceScrubCommit(wordCount: Int) {
        try {
            val ic = currentInputConnection ?: return
            ic.commitText("", 1)
            performHaptic(light = false)
            if (isBridgeActive) {
                val app = application as OwnVoiceApplication
                if (app.secureConfig.isPcAirTypingEnabled) {
                    for (i in 0 until wordCount) {
                        bridgeClient.enqueueBackspace()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OwnVoiceIME", "handleBackspaceScrubCommit error", e)
        }
    }

    private fun executeMagicWand(optionId: String) {
        val ic = currentInputConnection
        val selectedText = ic?.getSelectedText(0)?.toString()
        val textBefore = ic?.getTextBeforeCursor(500, 0)?.toString()?.trim()

        val textToTransform = when {
            !selectedText.isNullOrBlank() -> selectedText
            !textBefore.isNullOrBlank() -> textBefore
            lastInjectedText.isNotBlank() -> lastInjectedText
            else -> ""
        }

        if (textToTransform.isBlank()) {
            statusMessage = "Type or select text first!"
            performHaptic()
            return
        }

        performHaptic()
        keyboardState = KeyboardState.PROCESSING
        statusMessage = "Polishing with Gemini…"

        val instruction = when (optionId) {
            "fix" -> "Fix any spelling, grammar, punctuation, and typographical mistakes while preserving the exact original meaning and tone."
            "executive" -> "Rewrite the following text into polished, confident, professional executive business language suitable for email or workplace communication."
            "shorter" -> "Condense and summarize the following text into a punchy, crisp, and concise version without losing essential information."
            "translate_hindi" -> "Translate the text accurately between Hindi and English, keeping natural conversational nuances."
            "bullets" -> "Convert the following text into clear, structured, well-formatted bullet points."
            "casual" -> "Rewrite the following text into a warm, natural, friendly conversational chat tone."
            else -> "Improve clarity, tone, and flow."
        }

        serviceScope.launch {
            try {
                val (newText, _) = geminiClient.reshapeText(textToTransform, instruction)
                if (newText.isNotBlank()) {
                    ic?.beginBatchEdit()
                    if (!selectedText.isNullOrBlank()) {
                        ic.commitText(newText, 1)
                    } else if (!textBefore.isNullOrBlank()) {
                        ic.deleteSurroundingText(textToTransform.length, 0)
                        ic.commitText(newText, 1)
                    } else {
                        ic.commitText(newText, 1)
                    }
                    ic?.endBatchEdit()

                    lastInjectedText = newText
                    lastInjectedLength = newText.length
                    personalLexicon.recordTextUsage(newText)

                    if (isBridgeActive) {
                        val app = application as OwnVoiceApplication
                        val pcIp = app.secureConfig.desktopBridgeIp
                        if (pcIp.isNotBlank()) {
                            serviceScope.launch {
                                bridgeClient.sendToDesktop(pcIp, newText, token = app.secureConfig.desktopBridgeToken)
                            }
                        }
                    }

                    performHaptic()
                    keyboardState = KeyboardState.IDLE
                    statusMessage = "Polished with Gemini ✨"
                    delay(2500)
                    if (statusMessage == "Polished with Gemini ✨") statusMessage = ""
                } else {
                    keyboardState = KeyboardState.IDLE
                }
            } catch (e: Exception) {
                keyboardState = KeyboardState.ERROR
                statusMessage = e.localizedMessage ?: "Wand error"
                delay(3000)
                keyboardState = KeyboardState.IDLE
            }
        }
    }

    private fun toggleRecording() {
        if (audioRecorder.isCurrentlyRecording()) {
            stopRecordingAndTranscribe()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        dismissJob?.cancel()
        val app = application as OwnVoiceApplication
        if (app.secureConfig.apiKey.isBlank()) {
            keyboardState = KeyboardState.ERROR
            statusMessage = "API Key not set! Open OwnVoice app to paste key."
            return
        }

        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            keyboardState = KeyboardState.ERROR
            statusMessage = "Mic permission needed! Open OwnVoice app to allow."
            return
        }

        val started = audioRecorder.startRecording()
        if (started) {
            keyboardState = KeyboardState.RECORDING
            statusMessage = "Listening…"
            performHaptic()
        } else {
            keyboardState = KeyboardState.ERROR
            statusMessage = "Microphone is busy or locked by another app"
        }
    }

    private fun stopRecordingAndTranscribe() {
        performHaptic()
        keyboardState = KeyboardState.PROCESSING
        statusMessage = "Transcribing with Gemini…"

        serviceScope.launch {
            val wavBytes = withContext(Dispatchers.IO) {
                audioRecorder.stopRecording()
            }

            if (wavBytes.isEmpty()) {
                keyboardState = KeyboardState.ERROR
                statusMessage = "No speech detected. Speak closer to mic."
                delay(2000)
                keyboardState = KeyboardState.IDLE
                return@launch
            }

            try {
                val (text, latency) = geminiClient.transcribeAudio(
                    wavBytes,
                    mode = currentEffectiveTone,
                    personalLexicon = personalLexicon.getTopVocabulary()
                )
                val rawTrimmed = text.trim()
                val normalizedCmd = rawTrimmed.lowercase().removeSuffix(".").removeSuffix("!")

                if (rawTrimmed == "[COMMAND:DELETE_LAST]" || normalizedCmd in listOf("scratch that", "delete that", "undo that", "erase that", "cancel that")) {
                    if (lastInjectedLength > 0) {
                        currentInputConnection?.deleteSurroundingText(lastInjectedLength, 0)
                        lastInjectedLength = 0
                        lastInjectedText = ""
                    } else {
                        currentInputConnection?.deleteSurroundingText(1, 0)
                    }
                    performHaptic()
                    keyboardState = KeyboardState.IDLE
                    statusMessage = "Erased last entry"
                    delay(1200)
                    if (keyboardState == KeyboardState.IDLE && statusMessage == "Erased last entry") {
                        statusMessage = ""
                    }
                } else if (rawTrimmed == "[COMMAND:CLEAR_ALL]" || normalizedCmd in listOf("clear all", "delete line", "clear line", "clear text")) {
                    currentInputConnection?.deleteSurroundingText(2000, 500)
                    lastInjectedLength = 0
                    lastInjectedText = ""
                    performHaptic()
                    keyboardState = KeyboardState.IDLE
                    statusMessage = "Cleared text"
                    delay(1200)
                    if (keyboardState == KeyboardState.IDLE && statusMessage == "Cleared text") {
                        statusMessage = ""
                    }
                } else if (rawTrimmed == "[COMMAND:PASTE]" || normalizedCmd in listOf("paste", "paste it", "paste that", "paste it here", "paste here")) {
                    pasteClipboard()
                    keyboardState = KeyboardState.IDLE
                    statusMessage = "Pasted from clipboard"
                    delay(1200)
                    if (keyboardState == KeyboardState.IDLE && statusMessage == "Pasted from clipboard") {
                        statusMessage = ""
                    }
                } else if (text.isNotBlank()) {
                    val expanded = snippetEngine.expand(text)
                    currentInputConnection?.commitText(expanded, 1)
                    lastInjectedLength = expanded.length
                    lastInjectedText = expanded
                    personalLexicon.recordTextUsage(expanded)

                    if (isBridgeActive) {
                        val app = application as OwnVoiceApplication
                        serviceScope.launch {
                            val ok = bridgeClient.sendToDesktop(
                                desktopIp = app.secureConfig.desktopBridgeIp,
                                text = expanded,
                                token = app.secureConfig.desktopBridgeToken
                            )
                            if (ok) {
                                statusMessage = "Typed to PC & Phone"
                            } else {
                                statusMessage = "PC unreachable (Check Wi-Fi)"
                            }
                        }
                    }

                    performHaptic()
                    keyboardState = KeyboardState.RESULT
                    statusMessage = if (isBridgeActive) "Typed to PC & Phone" else ""

                    dismissJob?.cancel()
                    dismissJob = serviceScope.launch {
                        delay(10000)
                        if (keyboardState == KeyboardState.RESULT) {
                            keyboardState = KeyboardState.IDLE
                        }
                    }
                } else {
                    keyboardState = KeyboardState.ERROR
                    statusMessage = "No words recognized (silence)"
                    delay(2000)
                    keyboardState = KeyboardState.IDLE
                }
            } catch (e: Exception) {
                keyboardState = KeyboardState.ERROR
                statusMessage = e.localizedMessage ?: "Transcription failed"
                delay(3500)
                keyboardState = KeyboardState.IDLE
            }
        }
    }

    private fun shapeInjectedText(transformType: String) {
        if (lastInjectedText.isBlank()) return
        performHaptic()
        val textToShape = lastInjectedText
        keyboardState = KeyboardState.PROCESSING
        statusMessage = "Reshaping with Gemini…"

        val instruction = when (transformType) {
            "shorter" -> "Make this text concise, punchy, and direct while preserving all essential details."
            "executive" -> "Rewrite this in an executive, crisp, professional business correspondence style."
            "casual" -> "Rewrite this in a warm, friendly, natural conversational chat tone."
            "translate" -> "Translate this text to Hindi (or to fluent English if it is already in Hindi)."
            "fix" -> "Fix grammar, spelling, punctuation, and capitalization without altering words."
            else -> "Improve clarity and flow."
        }

        serviceScope.launch {
            try {
                val (newText, _) = geminiClient.reshapeText(textToShape, instruction)
                if (newText.isNotBlank()) {
                    val ic = currentInputConnection
                    if (lastInjectedLength > 0) {
                        ic?.deleteSurroundingText(lastInjectedLength, 0)
                    }
                    ic?.commitText(newText, 1)
                    lastInjectedText = newText
                    lastInjectedLength = newText.length

                    if (isBridgeActive) {
                        val app = application as OwnVoiceApplication
                        val pcIp = app.secureConfig.desktopBridgeIp
                        if (pcIp.isNotBlank()) {
                            serviceScope.launch {
                                bridgeClient.sendToDesktop(pcIp, newText)
                            }
                        }
                    }

                    performHaptic()
                    keyboardState = KeyboardState.RESULT
                    statusMessage = "Reshaped ($transformType)"

                    dismissJob?.cancel()
                    dismissJob = serviceScope.launch {
                        delay(10000)
                        if (keyboardState == KeyboardState.RESULT) {
                            keyboardState = KeyboardState.IDLE
                        }
                    }
                } else {
                    keyboardState = KeyboardState.RESULT
                }
            } catch (e: Exception) {
                keyboardState = KeyboardState.ERROR
                statusMessage = e.localizedMessage ?: "Reshape failed"
                delay(2500)
                keyboardState = KeyboardState.RESULT
            }
        }
    }

    private fun retryRecording() {
        try {
            if (lastInjectedLength > 0) {
                currentInputConnection?.deleteSurroundingText(lastInjectedLength, 0)
                lastInjectedLength = 0
                lastInjectedText = ""
            }
            startRecording()
        } catch (e: Exception) {
            android.util.Log.e("OwnVoiceIME", "retryRecording error", e)
        }
    }

    private fun sendAction() {
        try {
            val ic = currentInputConnection
            val editorInfo = currentInputEditorInfo
            if (editorInfo != null && (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE) {
                ic?.performEditorAction(editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION)
            } else {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            }
            performHaptic()
            keyboardState = KeyboardState.IDLE
            statusMessage = ""
        } catch (e: Exception) {
            android.util.Log.e("OwnVoiceIME", "sendAction error", e)
        }
    }

    private fun performHaptic(light: Boolean = true) {
        try {
            val app = application as? OwnVoiceApplication
            if (app?.secureConfig?.isHapticFeedbackEnabled == false) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effectId = if (light) VibrationEffect.EFFECT_TICK else VibrationEffect.EFFECT_CLICK
                vibrator?.vibrate(VibrationEffect.createPredefined(effectId))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (light) 6L else 12L
                val amplitude = if (light) 20 else 50
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (light) 6L else 12L)
            }
        } catch (ignored: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        dismissJob?.cancel()
        serviceScope.cancel()
        audioRecorder.cancelRecording()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
