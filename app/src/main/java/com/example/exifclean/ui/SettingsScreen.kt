package com.example.exifclean.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.exifclean.data.SanitizationMode

data class AppSettings(
    val defaultMode: SanitizationMode = SanitizationMode.LOSSLESS,
    val keepIcc: Boolean = true,
    val orientationOptionA: Boolean = true, // True = Option A minimal EXIF tag, False = Option B rotate pixels
    val saveToMediaStore: Boolean = true,
    val requireBiometrics: Boolean = false,
    val ttlHours: Int = 24
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onClearAllCaches: () -> Unit,
    onOpenOnboarding: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings & Privacy") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sanitization Defaults
            item {
                Text(
                    text = "Sanitization Defaults",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lossless Mode Default", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Removes EXIF/XMP metadata without re-encoding pixels", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.defaultMode == SanitizationMode.LOSSLESS,
                                onCheckedChange = { isLossless ->
                                    onSettingsChanged(
                                        settings.copy(
                                            defaultMode = if (isLossless) SanitizationMode.LOSSLESS else SanitizationMode.PARANOID
                                        )
                                    )
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Preserve ICC Color Profile", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Keep color space profile segment (APP2 / iCCP)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.keepIcc,
                                onCheckedChange = { keep -> onSettingsChanged(settings.copy(keepIcc = keep)) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Minimal EXIF Orientation Tag", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Option A: Write minimal Orientation tag (On) vs Option B: Rotate pixels (Off, forces Paranoid)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.orientationOptionA,
                                onCheckedChange = { optionA -> onSettingsChanged(settings.copy(orientationOptionA = optionA)) }
                            )
                        }
                    }
                }
            }

            // Storage & Vault Security
            item {
                Text(
                    text = "Storage & Vault Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Save Output to Pictures/Cleaned", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("MediaStore gallery vs App-Private storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.saveToMediaStore,
                                onCheckedChange = { save -> onSettingsChanged(settings.copy(saveToMediaStore = save)) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Require Biometric Prompt for Vault", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Gate Keystore key decryption behind biometric / PIN auth", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = settings.requireBiometrics,
                                onCheckedChange = { req -> onSettingsChanged(settings.copy(requireBiometrics = req)) }
                            )
                        }
                    }
                }
            }

            // Guarantee Model & Info
            item {
                Button(
                    onClick = onOpenOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Guarantee & Security Model")
                }
            }

            item {
                OutlinedButton(
                    onClick = onClearAllCaches,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All App Cache and Temp Intake Files")
                }
            }
        }
    }
}
