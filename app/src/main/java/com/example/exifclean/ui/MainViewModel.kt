package com.example.exifclean.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.exifclean.core.exif.MetadataReport
import com.example.exifclean.core.shred.ResultStatus
import com.example.exifclean.core.vault.VaultItem
import com.example.exifclean.core.vault.VaultRepository
import com.example.exifclean.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val intakeCopier = IntakeCopier(application)
    private val metadataReader = MetadataReader(application)
    private val sanitizerEngine = SanitizerEngine(application)
    private val vaultRepository = VaultRepository(application)
    private val outputWriter = OutputWriter(application)

    private val _intakeState = MutableStateFlow<IntakeUiState>(IntakeUiState.Idle)
    val intakeState: StateFlow<IntakeUiState> = _intakeState.asStateFlow()

    private val _vaultItems = MutableStateFlow<List<VaultItem>>(emptyList())
    val vaultItems: StateFlow<List<VaultItem>> = _vaultItems.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        refreshVault()
    }

    fun processUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _intakeState.value = IntakeUiState.Loading
            try {
                val currentItems = when (val state = _intakeState.value) {
                    is IntakeUiState.Success -> state.items
                    else -> emptyList()
                }
                val newItems = intakeCopier.copyUrisToIntakeCache(uris)
                val combined = currentItems + newItems
                _intakeState.value = IntakeUiState.Success(combined)
            } catch (e: Exception) {
                _intakeState.value = IntakeUiState.Error(e.localizedMessage ?: "Failed to import images")
            }
        }
    }

    fun removeItem(item: IntakeItem) {
        val currentItems = when (val state = _intakeState.value) {
            is IntakeUiState.Success -> state.items
            else -> return
        }
        intakeCopier.deleteItem(item)
        val updated = currentItems.filterNot { it.id == item.id }
        _intakeState.value = if (updated.isEmpty()) IntakeUiState.Idle else IntakeUiState.Success(updated)
    }

    fun clearAllIntake() {
        intakeCopier.clearIntakeCache()
        _intakeState.value = IntakeUiState.Idle
    }

    suspend fun inspectMetadata(item: IntakeItem): MetadataReport {
        return metadataReader.readMetadata(item)
    }

    suspend fun sanitizeItem(item: IntakeItem): SanitizationResult {
        val currentSettings = _settings.value
        val options = SanitizationOptions(
            mode = currentSettings.defaultMode,
            keepIcc = currentSettings.keepIcc,
            cutTrailingData = true,
            orientationValue = if (currentSettings.orientationOptionA) 1 else null
        )
        val tempDir = File(getApplication<Application>().cacheDir, "sanitized").apply { if (!exists()) mkdirs() }
        val result = sanitizerEngine.sanitize(item, tempDir, options)

        if (result.status !is ResultStatus.FAILED && result.outputFile != null) {
            if (currentSettings.saveToMediaStore) {
                outputWriter.saveToMediaStore(result.outputFile, item.originalMimeType)
            } else {
                outputWriter.saveToAppPrivate(result.outputFile, item.originalMimeType)
            }
        }
        return result
    }

    fun refreshVault() {
        viewModelScope.launch {
            _vaultItems.value = vaultRepository.getVaultItems()
        }
    }

    fun importItemToVault(item: IntakeItem) {
        viewModelScope.launch {
            vaultRepository.importFileToVault(
                inputFile = item.cachedFile,
                displayName = "Cleaned_${item.id.take(8)}",
                mimeType = item.originalMimeType,
                requireUserAuth = _settings.value.requireBiometrics
            )
            refreshVault()
        }
    }

    fun exportVaultItem(item: VaultItem, targetFile: File) {
        viewModelScope.launch {
            targetFile.outputStream().use { outStream ->
                vaultRepository.exportFileFromVault(item, outStream)
            }
        }
    }

    fun shredVaultItem(item: VaultItem) {
        viewModelScope.launch {
            vaultRepository.shredVaultItem(item)
            refreshVault()
        }
    }

    fun shredEntireVault() {
        viewModelScope.launch {
            vaultRepository.shredEntireVault()
            refreshVault()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
    }

    override fun onCleared() {
        super.onCleared()
        intakeCopier.clearIntakeCache()
    }
}
