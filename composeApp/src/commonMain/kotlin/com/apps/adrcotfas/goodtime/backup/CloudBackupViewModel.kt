package com.apps.adrcotfas.goodtime.backup

import androidx.lifecycle.ViewModel


data class CloudBackupUiState(
    val isCloudConnected: Boolean,
    val autoBackupEnabled: Boolean,
    val isAutoBackupInProgress: Boolean,
    val cloudLastBackupTimestamp: Long,
    val isManualBackupInProgress: Boolean,
    val isRestoreInProgress: Boolean,
)

expect class CloudBackupViewModel : ViewModel