package com.example.ownvoice.ime.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ownvoice.ime.model.ClipboardItem

@Composable
fun ClipboardHistoryDrawer(
    items: List<ClipboardItem>,
    isBridgeActive: Boolean,
    onPasteItem: (String) -> Unit,
    onSendToPc: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkCardBg = Color(0xFF161E30)
    val cardBorder = Color(0xFF26324D)
    val accentBlue = Color(0xFF3B82F6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📋 Clipboard History",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${items.size} clips",
                    color = Color(0xFF8A99B5),
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFFBAC7DE),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No copied text yet.\nCopy text on your phone or PC to see it here.",
                    color = Color(0xFF8A99B5),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = darkCardBg,
                        border = BorderStroke(1.dp, if (item.isPinned) accentBlue else cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Text Preview
                            Text(
                                text = item.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Action Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Insert / Paste Chip
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = accentBlue.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, accentBlue),
                                        modifier = Modifier.clickable { onPasteItem(item.text) }
                                    ) {
                                        Text(
                                            text = "📋 Insert",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Send to PC Chip
                                    if (isBridgeActive) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF00C7BE).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFF00C7BE)),
                                            modifier = Modifier.clickable { onSendToPc(item.text) }
                                        ) {
                                            Text(
                                                text = "💻 Send to PC",
                                                color = Color(0xFF00C7BE),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Pin toggle
                                    IconButton(
                                        onClick = { onTogglePin(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                            contentDescription = "Pin",
                                            tint = if (item.isPinned) accentBlue else Color(0xFF8A99B5),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Delete
                                    IconButton(
                                        onClick = { onDeleteItem(item.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color(0xFF8A99B5),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
