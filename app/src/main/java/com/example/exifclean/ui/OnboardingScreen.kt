package com.example.exifclean.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guarantee & Threat Model") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Local-Only Privacy Tool",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Zero network access, zero analytics, local encryption only.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Guarantee Status Explanations
            item {
                Text(
                    text = "Operation Guarantee Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        GuaranteeRow("LOSSLESS_CLEANED", "Metadata segment structures (EXIF, XMP, IPTC, trailing data) removed directly from streams without altering image pixels.")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        GuaranteeRow("REENCODED_CLEANED", "Subsampled pixel decoding, EXIF orientation rotation applied to pixels, re-encoded. Strips all MakerNotes, XMP, and custom chunks.")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        GuaranteeRow("CRYPTO_SHREDDED", "Android Keystore key is permanently deleted. Ciphertext file remaining on disk is mathematically unrecoverable.")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        GuaranteeRow("OVERWRITTEN_BEST_EFFORT", "Single-pass throwaway keystream overwrite, forced buffer sync, truncation, and deletion for app-private storage.")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        GuaranteeRow("UNLINKED_ONLY", "Original file entry unlinked via MediaStore/SAF request. Unlinking frees directory entry but does not rewrite original block storage.")
                    }
                }
            }

            // Platform Limitations Disclosure
            item {
                Text(
                    text = "Storage Hardware & System Limitations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "What No Android App Can Guarantee",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Flash Memory FTL & Wear-Leveling: Flash storage controllers remap logical blocks. Overwriting a file in place writes to new NAND cells, leaving old physical wear-leveled blocks intact until garbage-collected.\n\n" +
                                    "• System & Gallery Thumbnails: Android's MediaStore or third-party gallery apps may retain cached thumbnails outside this app's sandbox.\n\n" +
                                    "• Cloud Copies & App Caches: Sync services (Google Photos, Drive) or chat app caches retain copies created prior to cleaning.\n\n" +
                                    "• Android File-Based Encryption (FBE): On modern Android devices, FBE already renders unallocated deleted storage blocks unreadable ciphertext to unauthorized parties.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I Understand")
                }
            }
        }
    }
}

@Composable
fun GuaranteeRow(statusName: String, description: String) {
    Column {
        Text(
            text = statusName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
