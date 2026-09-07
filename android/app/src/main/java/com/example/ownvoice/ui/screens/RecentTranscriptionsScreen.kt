package com.example.ownvoice.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.OwnVoiceApplication
import com.example.ownvoice.core.HistoryItem
import com.example.ownvoice.network.BridgeClient
import com.example.ownvoice.theme.DesignTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentTranscriptionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication
    val scope = rememberCoroutineScope()
    val bridgeClient = remember { BridgeClient(app.secureConfig, context) }

    var historyItems by remember { mutableStateOf(app.secureConfig.getHistory()) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var speech: TextToSpeech? = null
        speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speech?.language = Locale.US
                isTtsReady = true
            }
        }
        tts = speech
        onDispose {
            speech?.stop()
            speech?.shutdown()
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OwnVoice Transcription", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val filteredItems = if (searchQuery.isBlank()) {
        historyItems
    } else {
        historyItems.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    // Group items into Today, Yesterday, and older dates
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    val todayItems = filteredItems.filter { now - it.timestamp < dayMs }
    val yesterdayItems = filteredItems.filter { it.timestamp in (now - 2 * dayMs)..(now - dayMs) }
    val olderItems = filteredItems.filter { it.timestamp < now - 2 * dayMs }

    Scaffold(
        containerColor = DesignTokens.Colors.BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onBack)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DesignTokens.ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        color = DesignTokens.ElectricBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "OwnVoice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.TextPrimary
                    )
                    Text(
                        text = "Your voice, everywhere",
                        fontSize = 10.sp,
                        color = DesignTokens.Colors.TextSubtle
                    )
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DesignTokens.Colors.BackgroundDark)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Delete button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B1D22))
                        .clickable { showClearDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All",
                        tint = DesignTokens.Colors.StatusRecording,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Load More Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DesignTokens.Colors.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "All transcriptions loaded", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Load More", fontSize = 12.sp, color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Load More", tint = DesignTokens.Colors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                // Count
                Text(
                    text = "Total ${filteredItems.size} items",
                    fontSize = 12.sp,
                    color = DesignTokens.Colors.TextSubtle
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recent Transcriptions",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesignTokens.Colors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your voice history, always with you.",
                    fontSize = 13.sp,
                    color = DesignTokens.Colors.TextMuted
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search your transcriptions...",
                            fontSize = 13.sp,
                            color = DesignTokens.Colors.TextSubtle
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = DesignTokens.Colors.TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = DesignTokens.Colors.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
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

            // Section 1: Today
            if (todayItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Today", count = "${todayItems.size} items")
                }
                items(todayItems) { item ->
                    TranscriptionCard(
                        item = item,
                        onPlay = {
                            if (isTtsReady && tts != null) {
                                tts?.speak(item.text, TextToSpeech.QUEUE_FLUSH, null, item.id)
                            } else {
                                Toast.makeText(context, "Playing audio...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCopy = { copyToClipboard(item.text) },
                        onDelete = {
                            app.secureConfig.deleteHistoryItem(item.id)
                            historyItems = app.secureConfig.getHistory()
                        }
                    )
                }
            }

            // Section 2: Yesterday
            if (yesterdayItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Yesterday", count = "${yesterdayItems.size} items")
                }
                items(yesterdayItems) { item ->
                    TranscriptionCard(
                        item = item,
                        onPlay = {
                            if (isTtsReady && tts != null) {
                                tts?.speak(item.text, TextToSpeech.QUEUE_FLUSH, null, item.id)
                            }
                        },
                        onCopy = { copyToClipboard(item.text) },
                        onDelete = {
                            app.secureConfig.deleteHistoryItem(item.id)
                            historyItems = app.secureConfig.getHistory()
                        }
                    )
                }
            }

            // Section 3: Older Dates
            if (olderItems.isNotEmpty()) {
                item {
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val sampleDate = sdf.format(Date(olderItems.first().timestamp))
                    SectionHeader(title = sampleDate, count = "${olderItems.size} items")
                }
                items(olderItems) { item ->
                    TranscriptionCard(
                        item = item,
                        onPlay = {
                            if (isTtsReady && tts != null) {
                                tts?.speak(item.text, TextToSpeech.QUEUE_FLUSH, null, item.id)
                            }
                        },
                        onCopy = { copyToClipboard(item.text) },
                        onDelete = {
                            app.secureConfig.deleteHistoryItem(item.id)
                            historyItems = app.secureConfig.getHistory()
                        }
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DesignTokens.Colors.CardSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No transcriptions found.", color = DesignTokens.Colors.TextSubtle, fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Transcriptions?", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all stored local dictations.", color = DesignTokens.Colors.TextMuted, fontSize = 13.sp) },
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

@Composable
private fun SectionHeader(title: String, count: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.TextPrimary)
        Text(count, fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
    }
}

@Composable
private fun TranscriptionCard(
    item: HistoryItem,
    onPlay: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = sdfTime.format(Date(item.timestamp))

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DesignTokens.Colors.CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Text quote
            Text(
                text = "\"${item.text}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DesignTokens.Colors.TextPrimary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle info & action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(timeStr, fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("•", fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.target,
                            fontSize = 11.sp,
                            color = DesignTokens.Colors.TextMuted
                        )
                    }

                    if (item.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.tags.forEach { tag ->
                                TagPill(tag = tag)
                            }
                        }
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Play
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .clickable(onClick = onPlay)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    // Copy
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .clickable(onClick = onCopy)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(14.dp))
                    }

                    // Delete
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .clickable(onClick = onDelete)
                    ) {
                        Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "More", tint = DesignTokens.Colors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TagPill(tag: String) {
    val bg = when (tag.lowercase()) {
        "work" -> Color(0xFF1E3A8A).copy(alpha = 0.4f)
        "meeting" -> Color(0xFF581C87).copy(alpha = 0.4f)
        "personal" -> Color(0xFF065F46).copy(alpha = 0.4f)
        "research" -> Color(0xFF312E81).copy(alpha = 0.4f)
        "ideas" -> Color(0xFF0E7490).copy(alpha = 0.4f)
        "content" -> Color(0xFF9A3412).copy(alpha = 0.4f)
        else -> Color(0xFF0F766E).copy(alpha = 0.4f)
    }

    val textCol = when (tag.lowercase()) {
        "work" -> DesignTokens.ElectricBlue
        "meeting" -> DesignTokens.PurpleAccent
        "personal" -> DesignTokens.StatusGreen
        "research" -> Color(0xFFA5B4FC)
        "ideas" -> Color(0xFF67E8F9)
        "content" -> Color(0xFFFDBA74)
        else -> Color(0xFF5EEAD4)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = tag,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textCol,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
