package io.nativeplanet.launcher.domain

import io.nativeplanet.launcher.domain.model.*
import kotlinx.coroutines.flow.Flow

interface NativePlanetClient {
    fun observeRuntimeStatus(): Flow<RuntimeStatus>
    fun observeNetworkStatus(): Flow<NetworkStatus>
    fun observeBootPackageStatus(): Flow<BootPackageStatus>
    fun observeDiagnostics(): Flow<DiagnosticsSummary>

    suspend fun startRuntime(): ControlResult
    suspend fun stopRuntime(): ControlResult
    suspend fun restartRuntime(): ControlResult
    suspend fun provisionMoon(shipName: String, parentName: String, keyMaterial: String): ControlResult
    suspend fun pairWithPlanet(hostUrl: String, accessCode: String): ControlResult
}
