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
package com.apps.adrcotfas.goodtime.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.di.injectLogger
import com.apps.adrcotfas.goodtime.onboarding.MainViewModel
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent

open class GoodtimeMainActivity :
    ComponentActivity(),
    KoinComponent {
    internal val viewModel: MainViewModel by viewModel<MainViewModel>()
    val log: Logger by injectLogger("GoodtimeMainActivity")

    private var availableVersionCode: Int = 0

    private val appUpdateResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { activityResult ->
            handleUpdateResult(activityResult.resultCode)
        }

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    fun triggerAppUpdate() {
        lifecycleScope.launch {
            appUpdateManager
                .requestUpdateFlow()
                .catch { emit(AppUpdateResult.NotAvailable) }
                .collectLatest { result ->
                    when (result) {
                        is AppUpdateResult.Available -> {
                            result.startImmediateUpdate(
                                appUpdateResultLauncher,
                            )
                        }
                        else -> {
                            log.i { "No update available" }
                        }
                    }
                }
        }
    }

    private fun handleUpdateResult(resultCode: Int) {
        when (resultCode) {
            RESULT_OK -> {
                log.i { "Update successful" }
                viewModel.setLastDismissedUpdateVersionCode(0)
                viewModel.setUpdateAvailable(false)
            }
            RESULT_CANCELED -> {
                log.i { "Update dismissed, version code: $availableVersionCode" }
                viewModel.setLastDismissedUpdateVersionCode(availableVersionCode.toLong())
                viewModel.setUpdateAvailable(true)
            }
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> log.e { "Update Failed" }
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            appUpdateManager
                .requestUpdateFlow()
                .catch { emit(AppUpdateResult.NotAvailable) }
                .collectLatest { result ->
                    when (result) {
                        is AppUpdateResult.Available -> {
                            log.i { "Update available: ${result.updateInfo.availableVersionCode()}" }
                            availableVersionCode = result.updateInfo.availableVersionCode()
                            val lastDismissedVersionCode = viewModel.uiState.first().lastDismissedUpdateVersionCode
                            if (lastDismissedVersionCode != availableVersionCode.toLong()) {
                                result.startImmediateUpdate(
                                    appUpdateResultLauncher,
                                )
                            } else {
                                viewModel.setUpdateAvailable(true)
                            }
                        }
                        else -> Unit
                    }
                }
        }
        lifecycleScope.launch {
            viewModel.uiState.map { it.shouldAskForReview }.collect { askForFeedback ->
                if (askForFeedback) {
                    showFeedbackDialog()
                }
            }
        }
    }

    private fun showFeedbackDialog() {
        val manager = ReviewManagerFactory.create(this.applicationContext)
        manager.requestReviewFlow()
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val result = manager.launchReviewFlow(this, reviewInfo)
                result.addOnCompleteListener {
                    log.i { "Review flow complete" }
                    viewModel.resetShouldAskForReview()
                }
            } else {
                log.e(task.exception) { "There was some problem" }
            }
        }
    }
}
