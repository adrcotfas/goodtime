/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.goodtime.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.TimerProfile
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.ui.lightPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddEditLabelUiState(
    val isLoading: Boolean = true,
    val isPro: Boolean = true,
    val activeLabelName: String = Label.DEFAULT_LABEL_NAME,
    val labels: List<Label> = emptyList(),
    val timerProfiles: List<TimerProfile> = emptyList(),
    val defaultLabelDisplayName: String = "",
    val labelToEdit: Label? = null, // this does not change after initialization
    val tmpLabel: Label = Label.newLabelWithRandomColorIndex(lightPalette.lastIndex),
)

val AddEditLabelUiState.existingLabelNames: List<String>
    get() = labels.map { label -> label.name }

fun AddEditLabelUiState.labelNameIsValid(): Boolean {
    val name = tmpLabel.name.trim()
    return name.isNotEmpty() &&
        !existingLabelNames
            .map { labels -> labels.lowercase() }
            .minus(labelToEdit?.name?.lowercase())
            .contains(name.lowercase()) &&
        name.lowercase() != defaultLabelDisplayName.lowercase()
}

class AddEditLabelViewModel(
    private val repo: LocalDataRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditLabelUiState())
    val uiState = _uiState.asStateFlow()

    fun init(
        labelToEditName: String? = null,
        defaultLabelDisplayName: String,
    ) {
        viewModelScope.launch {
            combine(
                settingsRepository.settings
                    .distinctUntilChanged { old, new ->
                        old.labelName == new.labelName &&
                            old.isPro == new.isPro
                    },
                repo.selectAllLabels(),
                repo.selectAllTimerProfiles(),
            ) { settings, labels, timerProfiles ->
                Triple(settings, labels, timerProfiles)
            }.collect { (settings, labels, timerProfiles) ->
                val labelToEdit =
                    labelToEditName?.let { name ->
                        labels.find { label -> label.name == name }
                    }
                val defaultLabel =
                    labels.find { label -> label.name == Label.DEFAULT_LABEL_NAME }
                        ?: Label.defaultLabel()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isPro = settings.isPro,
                        defaultLabelDisplayName = defaultLabelDisplayName,
                        labelToEdit = labelToEdit,
                        tmpLabel =
                            labelToEdit
                                ?: Label
                                    .newLabelWithRandomColorIndex(lightPalette.lastIndex)
                                    .copy(timerProfile = defaultLabel.timerProfile),
                        activeLabelName = settings.labelName,
                        labels = labels,
                        timerProfiles = timerProfiles,
                    )
                }
            }
        }
    }

    fun addLabel(label: Label) {
        viewModelScope.launch {
            repo.insertLabel(label.copy(name = label.name.trim()))
        }
    }

    fun updateLabel(
        labelName: String,
        label: Label,
    ) {
        viewModelScope.launch {
            repo.updateLabel(labelName, label.copy(name = label.name.trim()))
            val isRenamingActiveLabel =
                labelName == _uiState.value.activeLabelName && labelName != label.name
            if (isRenamingActiveLabel) {
                settingsRepository.activateLabelWithName(label.name)
            }
            _uiState.update {
                it.copy(labelToEdit = label)
            }
        }
    }

    fun updateTmpLabel(
        newLabel: Label,
        resetProfile: Boolean = true,
    ) {
        _uiState.update {
            it.copy(
                tmpLabel =
                    if (resetProfile) {
                        newLabel.copy(
                            timerProfile =
                                newLabel.timerProfile.copy(
                                    name = null,
                                ),
                        )
                    } else {
                        newLabel
                    },
            )
        }
    }
}
