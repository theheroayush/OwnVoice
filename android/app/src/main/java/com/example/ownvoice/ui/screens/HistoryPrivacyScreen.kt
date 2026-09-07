package com.example.ownvoice.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.core.HistoryItem
import com.example.ownvoice.core.SecureConfig
import com.example.ownvoice.network.ApiKeyValidator
import com.example.ownvoice.network.GistSyncManager
import com.example.ownvoice.theme.DesignTokens
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun HistoryPrivacyScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication
    val scope = rememberCoroutineScope()
    val gistManager = remember { GistSyncManager() }

    var historyItems by remember { mutableStateOf(app.secureConfig.getHistory()) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    var gistToken by remember { mutableStateOf(app.secureConfig.gistToken) }
    var gistId by remember { mutableStateOf(app.secureConfig.gistId) }
    var syncStatusMessage by remember { mutableStateOf("") }

    var apiKeyInput by remember { mutableStateOf(app.secureConfig.apiKey) }
    var apiStatusMessage by remember { mutableStateOf("") }
    var isVerifyingKey by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OwnVoice Transcription", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun formatTimestamp(ts: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(ts))
    }

    val filteredItems = if (searchQuery.isBlank()) {
        historyItems
    } else {
        historyItems.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = DesignTokens.Colors.BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = DesignTokens.Colors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "History & Configuration",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // History Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRANSCRIPTION HISTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextSubtle,
                        letterSpacing = 0.8.sp
                    )
                    if (historyItems.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DesignTokens.Colors.StatusRecording,
                            modifier = Modifier.clickable {
                                showClearDialog = true
                            }
                        )
                    }
                }
            }

            // Search History
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search past dictations...", fontSize = 13.sp, color = DesignTokens.Colors.TextSubtle) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DesignTokens.Colors.TextMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = DesignTokens.Colors.TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                        unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                        focusedContainerColor = DesignTokens.Colors.CardSurface,
                        unfocusedContainerColor = DesignTokens.Colors.CardSurface,
                        focusedTextColor = DesignTokens.Colors.TextPrimary,
                        unfocusedTextColor = DesignTokens.Colors.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // History Items List
            if (filteredItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isBlank()) "No transcription history yet." else "No matches found.",
                                color = DesignTokens.Colors.TextSubtle,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredItems) { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DesignTokens.Colors.PrimaryBlue.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = item.target,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DesignTokens.Colors.PrimaryLightBlue,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatTimestamp(item.timestamp),
                                        fontSize = 11.sp,
                                        color = DesignTokens.Colors.TextSubtle
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { copyToClipboard(item.text) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = DesignTokens.Colors.TextMuted, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            app.secureConfig.deleteHistoryItem(item.id)
                                            historyItems = app.secureConfig.getHistory()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DesignTokens.Colors.StatusRecording, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = item.text,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = DesignTokens.Colors.TextPrimary
                            )
                        }
                    }
                }
            }

            // GitHub Gist Cloud Sync Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "GITHUB GIST CLOUD SYNC",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextSubtle,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sync vocabulary & custom snippets between devices via GitHub Gist:",
                            fontSize = 12.sp,
                            color = DesignTokens.Colors.TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = gistToken,
                            onValueChange = {
                                gistToken = it
                                app.secureConfig.gistToken = it
                            },
                            label = { Text("GitHub Personal Access Token (PAT)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                                unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                                focusedTextColor = DesignTokens.Colors.TextPrimary,
                                unfocusedTextColor = DesignTokens.Colors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = gistId,
                            onValueChange = {
                                gistId = it
                                app.secureConfig.gistId = it
                            },
                            label = { Text("Gist ID (leave blank to create new)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                                unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                                focusedTextColor = DesignTokens.Colors.TextPrimary,
                                unfocusedTextColor = DesignTokens.Colors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (syncStatusMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(syncStatusMessage, fontSize = 12.sp, color = DesignTokens.Colors.PrimaryLightBlue)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        syncStatusMessage = "Pushing to GitHub Gist..."
                                        val payload = JSONObject().apply {
                                            put("vocabulary", JSONArray(app.secureConfig.getVocabulary()))
                                            val snipJson = JSONObject()
                                            app.secureConfig.getSnippets().forEach { (k, v) -> snipJson.put(k, v) }
                                            put("snippets", snipJson)
                                            put("dictation_mode", app.secureConfig.dictationMode)
                                        }
                                        val res = gistManager.syncToGist(gistToken, payload, gistId.ifBlank { null })
                                        syncStatusMessage = res.message
                                        if (res.success && res.gistId != null) {
                                            gistId = res.gistId
                                            app.secureConfig.gistId = res.gistId
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Push to Cloud")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        syncStatusMessage = "Pulling from GitHub Gist..."
                                        val res = gistManager.syncFromGist(gistToken, gistId)
                                        syncStatusMessage = res.message
                                        if (res.success && res.payload != null) {
                                            val p = res.payload
                                            val vocabArr = p.optJSONArray("vocabulary")
                                            if (vocabArr != null) {
                                                for (i in 0 until vocabArr.length()) {
                                                    app.secureConfig.addVocabularyTerm(vocabArr.getString(i))
                                                }
                                            }
                                            val snipObj = p.optJSONObject("snippets")
                                            if (snipObj != null) {
                                                val existing = app.secureConfig.getSnippets().toMutableMap()
                                                val keys = snipObj.keys()
                                                while (keys.hasNext()) {
                                                    val k = keys.next()
                                                    existing[k] = snipObj.getString(k)
                                                }
                                                app.secureConfig.saveSnippets(existing)
                                            }
                                        }
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pull from Cloud", color = DesignTokens.Colors.TextPrimary)
                            }
                        }
                    }
                }
            }

            // Gemini API Key & Model Configuration
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "GEMINI 3.6 FLASH API KEY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextSubtle,
                    letterSpacing = 0.8.sp
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Provide your own Google AI Studio API key for unlimited ultra-fast dictation:",
                            fontSize = 12.sp,
                            color = DesignTokens.Colors.TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                app.secureConfig.apiKey = it
                            },
                            label = { Text("Google API Key") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DesignTokens.Colors.PrimaryBlue,
                                unfocusedBorderColor = DesignTokens.Colors.BorderSubtle,
                                focusedTextColor = DesignTokens.Colors.TextPrimary,
                                unfocusedTextColor = DesignTokens.Colors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (apiStatusMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = apiStatusMessage,
                                fontSize = 12.sp,
                                color = if (apiStatusMessage.startsWith("Valid")) DesignTokens.Colors.StatusReady else DesignTokens.Colors.StatusRecording
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    isVerifyingKey = true
                                    apiStatusMessage = "Validating key with Google AI..."
                                    scope.launch {
                                        val (isValid, msg) = ApiKeyValidator.validateKey(apiKeyInput)
                                        isVerifyingKey = false
                                        apiStatusMessage = msg
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.PrimaryBlue),
                                modifier = Modifier.weight(1f),
                                enabled = !isVerifyingKey
                            ) {
                                Text(if (isVerifyingKey) "Testing..." else "Test Key")
                            }

                            OutlinedButton(
                                onClick = {
                                    apiKeyInput = SecureConfig.DEFAULT_API_KEY
                                    app.secureConfig.apiKey = SecureConfig.DEFAULT_API_KEY
                                    apiStatusMessage = "Reset to default key."
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset Default", color = DesignTokens.Colors.TextPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Clear History Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will permanently remove all stored local dictations. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = DesignTokens.Colors.TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        app.secureConfig.clearHistory()
                        historyItems = emptyList()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.StatusRecording)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }
}
