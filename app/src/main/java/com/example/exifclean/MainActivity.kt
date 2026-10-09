package com.example.exifclean

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.exifclean.ui.AppNavigation
import com.example.exifclean.ui.MainViewModel
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // HARDENING: FLAG_SECURE on every screen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Handle incoming share or panic intents
        handleIntent(intent)

        setContent {
            MaterialTheme {
                Surface {
                    val intakeState by mainViewModel.intakeState.collectAsState()
                    val vaultItems by mainViewModel.vaultItems.collectAsState()
                    val settings by mainViewModel.settings.collectAsState()

                    AppNavigation(
                        intakeState = intakeState,
                        vaultItems = vaultItems,
                        settings = settings,
                        onUrisPicked = { uris -> mainViewModel.processUris(uris) },
                        onRemoveIntakeItem = { item -> mainViewModel.removeItem(item) },
                        onClearIntakeCache = { mainViewModel.clearAllIntake() },
                        onInspectMetadata = { item -> runBlocking { mainViewModel.inspectMetadata(item) } },
                        onSanitizeItem = { item -> runBlocking { mainViewModel.sanitizeItem(item) } },
                        onImportToVault = {
                            val state = mainViewModel.intakeState.value
                            if (state is com.example.exifclean.ui.IntakeUiState.Success && state.items.isNotEmpty()) {
                                mainViewModel.importItemToVault(state.items.first())
                            }
                        },
                        onExportVaultItem = { item ->
                            val exportFile = java.io.File(cacheDir, "${item.displayName}.${if (item.mimeType.contains("png")) "png" else "jpg"}")
                            mainViewModel.exportVaultItem(item, exportFile)
                        },
                        onShredVaultItem = { item -> mainViewModel.shredVaultItem(item) },
                        onShredEntireVault = { mainViewModel.shredEntireVault() },
                        onSettingsChanged = { newSettings -> mainViewModel.updateSettings(newSettings) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type
        val extraAction = intent.getStringExtra("action")

        if (extraAction == "panic_shred") {
            mainViewModel.shredEntireVault()
            return
        }

        if (type?.startsWith("image/") == true) {
            when (action) {
                Intent.ACTION_SEND -> {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    if (uri != null) {
                        mainViewModel.processUris(listOf(uri))
                    }
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    }
                    if (!uris.isNullOrEmpty()) {
                        mainViewModel.processUris(uris)
                    }
                }
            }
        }
    }
}
