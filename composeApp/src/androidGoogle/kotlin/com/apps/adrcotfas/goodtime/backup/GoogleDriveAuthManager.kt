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
package com.apps.adrcotfas.goodtime.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/**
 * Result of an authorization attempt.
 */
sealed class GoogleDriveAuthResult {
    data class Success(
        val authResult: AuthorizationResult,
    ) : GoogleDriveAuthResult()

    data class NeedsUserConsent(
        val pendingIntent: PendingIntent,
    ) : GoogleDriveAuthResult()

    data class Error(
        val exception: Exception,
    ) : GoogleDriveAuthResult()
}

/**
 * Manages Google Drive authorization using the modern AuthorizationClient API.
 *
 * This uses only AuthorizationClient (no Credential Manager sign-in) which means:
 * - No Web Client ID needed
 * - Only Android OAuth client configured in Google Cloud Console
 * - User sees Google account picker + permission dialog on first auth
 * - Subsequent auth is silent (if permission was already granted)
 */
class GoogleDriveAuthManager(
    private val context: Context,
    private val logger: Logger,
) {
    private val authorizationClient = Identity.getAuthorizationClient(context)

    /**
     * Attempt to authorize Google Drive access.
     *
     * @return [GoogleDriveAuthResult.Success] if already authorized (with access token),
     *         [GoogleDriveAuthResult.NeedsUserConsent] if user needs to grant permission,
     *         [GoogleDriveAuthResult.Error] if authorization failed
     */
    suspend fun authorize(): GoogleDriveAuthResult {
        logger.d { "authorize() - requesting Drive appDataFolder scope" }

        return suspendCancellableCoroutine { continuation ->
            val request =
                AuthorizationRequest
                    .builder()
                    .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA)))
                    .build()

            authorizationClient
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        // User needs to grant permission
                        val pendingIntent = result.pendingIntent
                        if (pendingIntent != null) {
                            logger.d { "authorize() - needs user consent" }
                            continuation.resume(GoogleDriveAuthResult.NeedsUserConsent(pendingIntent))
                        } else {
                            logger.e { "authorize() - hasResolution but no pendingIntent" }
                            continuation.resume(
                                GoogleDriveAuthResult.Error(
                                    IllegalStateException("Authorization requires consent but no pending intent available"),
                                ),
                            )
                        }
                    } else {
                        // Already authorized
                        val token = result.accessToken
                        val grantedScopes = result.grantedScopes
                        logger.d { "authorize() - grantedScopes: $grantedScopes" }
                        if (token != null) {
                            // Check if Drive scope is granted
                            val hasDriveScope =
                                grantedScopes.any { scope ->
                                    scope.toString().contains(DriveScopes.DRIVE_APPDATA)
                                }
                            logger.d { "authorize() - hasDriveScope: $hasDriveScope" }
                            if (hasDriveScope) {
                                logger.d { "authorize() - success, got access token with Drive scope" }
                                continuation.resume(GoogleDriveAuthResult.Success(result))
                            } else {
                                // Token doesn't have Drive scope - treat as needing consent
                                logger.w { "authorize() - token missing Drive scope, needs user consent" }
                                val pendingIntent = result.pendingIntent
                                if (pendingIntent != null) {
                                    continuation.resume(
                                        GoogleDriveAuthResult.NeedsUserConsent(
                                            pendingIntent,
                                        ),
                                    )
                                } else {
                                    continuation.resume(
                                        GoogleDriveAuthResult.Error(
                                            IllegalStateException("Token missing Drive scope and no pending intent"),
                                        ),
                                    )
                                }
                            }
                        } else {
                            logger.e { "authorize() - no resolution needed but no token" }
                            continuation.resume(
                                GoogleDriveAuthResult.Error(
                                    IllegalStateException("Authorization succeeded but no access token returned"),
                                ),
                            )
                        }
                    }
                }.addOnFailureListener { exception ->
                    logger.e(exception) { "authorize() - failed" }
                    continuation.resume(GoogleDriveAuthResult.Error(exception))
                }
        }
    }

    /**
     * Process the result from the authorization consent UI.
     *
     * Call this from your activity's onActivityResult when the user completes
     * the Google authorization consent flow.
     *
     * @param data The Intent data from the activity result
     * @return The access token if successful, null otherwise
     */
    fun getAuthorizationResultFromIntent(data: Intent?): String? {
        if (data == null) {
            logger.w { "getAuthorizationResultFromIntent() - data is null" }
            return null
        }

        return try {
            val result: AuthorizationResult =
                authorizationClient.getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            val grantedScopes = result.grantedScopes

            logger.d { "getAuthorizationResultFromIntent() - grantedScopes: $grantedScopes" }

            if (token != null) {
                logger.d { "getAuthorizationResultFromIntent() - got access token (length=${token.length})" }
            } else {
                logger.w { "getAuthorizationResultFromIntent() - no access token in result" }
            }
            token
        } catch (e: Exception) {
            logger.e(e) { "getAuthorizationResultFromIntent() - failed to parse result" }
            null
        }
    }
}
