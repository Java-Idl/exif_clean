package com.example.exifclean.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exifclean.core.exif.MetadataReport
import com.example.exifclean.core.exif.MetadataTag
import com.example.exifclean.core.exif.RiskLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataDetailScreen(
    report: MetadataReport,
    onBack: () -> Unit,
    onRequestLocationPermission: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metadata Details") },
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
            // Overall Risk Badge
            item {
                OverallRiskCard(riskLevel = report.overallRisk)
            }

            // GPS Section
            item {
                GpsSectionCard(
                    report = report,
                    onRequestLocationPermission = onRequestLocationPermission
                )
            }

            // Trailing Data / Motion Photo Warning
            if (report.trailingData.hasTrailingData) {
                item {
                    TrailingDataCard(info = report.trailingData)
                }
            }

            // Grouped Tags
            val grouped = report.tags.groupBy { it.category }
            if (grouped.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No metadata tags detected in this file",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(grouped.keys.toList()) { category ->
                    val categoryTags = grouped[category] ?: emptyList()
                    CategoryCard(category = category, tags = categoryTags)
                }
            }
        }
    }
}

@Composable
fun OverallRiskCard(riskLevel: RiskLevel) {
    val (bgColor, textColor, label) = when (riskLevel) {
        RiskLevel.HIGH -> Triple(Color(0xFFFFD2D2), Color(0xFF900000), "HIGH RISK")
        RiskLevel.MEDIUM -> Triple(Color(0xFFFFE0B2), Color(0xFFE65100), "MEDIUM RISK")
        RiskLevel.LOW -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), "LOW RISK")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Overall Risk Assessment",
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
            Box(
                modifier = Modifier
                    .background(textColor, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = riskLevel.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun GpsSectionCard(
    report: MetadataReport,
    onRequestLocationPermission: (() -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GPS Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (report.gpsDisplayString != null) {
                Text(
                    text = report.gpsDisplayString,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = report.gpsExplanation ?: "No location info",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (report.gpsExplanation?.contains("Redacted") == true && onRequestLocationPermission != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRequestLocationPermission) {
                        Text("Read Location Permission")
                    }
                }
            }
        }
    }
}

@Composable
fun TrailingDataCard(info: com.example.exifclean.core.exif.TrailingDataInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (info.isMotionPhoto) "Motion Photo Detected" else "Trailing Data Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (info.isMotionPhoto) {
                    "Embedded MP4 video payload size: ${info.embeddedMp4Size} bytes"
                } else {
                    "Extra bytes found after JPEG EOI marker: ${info.trailingDataSize} bytes"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun CategoryCard(category: String, tags: List<MetadataTag>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = category,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            tags.forEachIndexed { index, tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tag.key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tag.displayValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RiskTagBadge(riskLevel = tag.riskLevel)
                }
                if (index < tags.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun RiskTagBadge(riskLevel: RiskLevel) {
    val (bgColor, textColor) = when (riskLevel) {
        RiskLevel.HIGH -> Pair(Color(0xFFFFD2D2), Color(0xFF900000))
        RiskLevel.MEDIUM -> Pair(Color(0xFFFFE0B2), Color(0xFFE65100))
        RiskLevel.LOW -> Pair(Color(0xFFE8F5E9), Color(0xFF1B5E20))
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = riskLevel.name,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
