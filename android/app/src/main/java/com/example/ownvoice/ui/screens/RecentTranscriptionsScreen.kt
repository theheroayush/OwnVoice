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
import com.example.ownvoice.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentTranscriptionsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as OwnVoiceApplication

    var historyItems by remember { mutableStateOf(app.secureConfig.getHistory()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "PC", "Phone"
    var showClearAllDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<HistoryItem?>(null) }

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

    val filteredItems = historyItems
        .filter { item ->
            if (selectedFilter == "All") true
            else item.target.equals(selectedFilter, ignoreCase = true)
        }
        .filter { item ->
            if (searchQuery.isBlank()) true
            else item.text.contains(searchQuery, ignoreCase = true)
        }

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
                        text = "Settings",
                        color = DesignTokens.ElectricBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (historyItems.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        color = DesignTokens.Colors.StatusRecording,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { showClearAllDialog = true }
                    )
                }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recent Transcriptions",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = DesignTokens.Colors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total ${filteredItems.size} transcriptions",
                            fontSize = 13.sp,
                            color = DesignTokens.Colors.TextMuted
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search past dictations...",
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

            // Filter Pills (All, PC, Phone)
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "PC", "Phone").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) DesignTokens.Colors.PrimaryBlue else DesignTokens.Colors.CardSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) DesignTokens.Colors.PrimaryBlue else DesignTokens.Colors.BorderSubtle
                            ),
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else DesignTokens.Colors.TextMuted,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Section 1: Today
            if (todayItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Today", count = "${todayItems.size} items")
                }
                items(todayItems, key = { it.id }) { item ->
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
                        onDelete = { itemToDelete = item }
                    )
                }
            }

            // Section 2: Yesterday
            if (yesterdayItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Yesterday", count = "${yesterdayItems.size} items")
                }
                items(yesterdayItems, key = { it.id }) { item ->
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
                        onDelete = { itemToDelete = item }
                    )
                }
            }

            // Section 3: Older Dates
            if (olderItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Older", count = "${olderItems.size} items")
                }
                items(olderItems, key = { it.id }) { item ->
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
                        onDelete = { itemToDelete = item }
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
                        Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching transcriptions found." else "No transcriptions yet. Use the Voice Assistant to dictate!",
                                color = DesignTokens.Colors.TextSubtle,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation dialog for deleting single item
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Transcription?", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete this transcription?\n\n\"${target.text.take(60)}...\"",
                    color = DesignTokens.Colors.TextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        app.secureConfig.deleteHistoryItem(target.id)
                        historyItems = app.secureConfig.getHistory()
                        itemToDelete = null
                        Toast.makeText(context, "Transcription deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.StatusRecording)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = DesignTokens.Colors.TextMuted)
                }
            },
            containerColor = DesignTokens.Colors.CardSurface
        )
    }

    // Confirmation dialog for Clear All
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Transcriptions?", color = DesignTokens.Colors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all stored local dictations. This action cannot be undone.", color = DesignTokens.Colors.TextMuted, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        app.secureConfig.clearHistory()
                        historyItems = emptyList()
                        showClearAllDialog = false
                        Toast.makeText(context, "All transcriptions cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.StatusRecording)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
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
            Text(
                text = "\"${item.text}\"",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DesignTokens.Colors.TextPrimary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(timeStr, fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("•", fontSize = 11.sp, color = DesignTokens.Colors.TextSubtle)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.target.equals("pc", ignoreCase = true)) DesignTokens.ElectricBlue.copy(alpha = 0.15f) else Color(0xFF0F766E).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = item.target,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.target.equals("pc", ignoreCase = true)) DesignTokens.ElectricBlue else Color(0xFF2DD4BF),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Play
                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    // Copy
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(14.dp))
                    }

                    // Delete (Trash icon with confirmation)
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B1D22))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DesignTokens.Colors.StatusRecording, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
