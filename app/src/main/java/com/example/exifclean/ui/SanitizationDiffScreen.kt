package com.example.exifclean.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exifclean.data.SanitizationResult
import com.example.exifclean.core.shred.ResultStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanitizationDiffScreen(
    result: SanitizationResult,
    onBack: () -> Unit,
    onSaveToGallery: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sanitization Summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            // Status Card
            item {
                StatusCard(status = result.status)
            }

            // Before vs After Risk Badge Comparison
            item {
                RiskComparisonCard(
                    beforeRisk = result.beforeReport.overallRisk,
                    afterRisk = result.afterReport?.overallRisk
                )
            }

            // Tag Diff Summary
            item {
                Text(
                    text = "Removed Metadata Tags (${result.beforeReport.tags.size} -> ${result.afterReport?.tags?.size ?: 0})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val removedTags = result.beforeReport.tags
            if (removedTags.isEmpty()) {
                item {
                    Text(
                        text = "No metadata was present in source file",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(removedTags) { tag ->
                    RemovedTagItemCard(tag = tag)
                }
            }

            if (result.status !is ResultStatus.FAILED && onSaveToGallery != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSaveToGallery,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Cleaned Copy to Pictures/Cleaned")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(status: ResultStatus) {
    val isSuccess = status !is ResultStatus.FAILED
    val (bgColor, iconColor, titleText, descText) = if (isSuccess) {
        Quadruple(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32),
            "Sanitization Succeeded",
            "Result: ${status.name}"
        )
    } else {
        Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            "Sanitization Failed",
            status.name
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = iconColor
                )
            }
        }
    }
}

@Composable
fun RiskComparisonCard(
    beforeRisk: com.example.exifclean.core.exif.RiskLevel,
    afterRisk: com.example.exifclean.core.exif.RiskLevel?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BEFORE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                RiskTagBadge(riskLevel = beforeRisk)
            }

            Text("➔", style = MaterialTheme.typography.titleMedium)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AFTER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                if (afterRisk != null) {
                    RiskTagBadge(riskLevel = afterRisk)
                } else {
                    Text("N/A", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun RemovedTagItemCard(tag: com.example.exifclean.core.exif.MetadataTag) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${tag.category}: ${tag.key}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stripped: ${tag.displayValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            RiskTagBadge(riskLevel = tag.riskLevel)
        }
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
