package io.nativeplanet.launcher.data.file

import io.nativeplanet.launcher.data.stub.StubNativePlanetClient
import io.nativeplanet.launcher.domain.NativePlanetClient
import io.nativeplanet.launcher.domain.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileNativePlanetClient @Inject constructor(
    private val networkStateReader: NetworkStateReader,
    private val stubClient: StubNativePlanetClient
) : NativePlanetClient {

    override fun observeRuntimeStatus(): Flow<RuntimeStatus> {
        return stubClient.observeRuntimeStatus()
    }

    override fun observeNetworkStatus(): Flow<NetworkStatus> {
        return networkStateReader.observeNetworkState()
    }

    override fun observeBootPackageStatus(): Flow<BootPackageStatus> {
        return stubClient.observeBootPackageStatus()
    }

    override fun observeDiagnostics(): Flow<DiagnosticsSummary> {
        return stubClient.observeDiagnostics()
    }

    override suspend fun startRuntime(): ControlResult {
        return stubClient.startRuntime()
    }

    override suspend fun stopRuntime(): ControlResult {
        return stubClient.stopRuntime()
    }

    override suspend fun restartRuntime(): ControlResult {
        return stubClient.restartRuntime()
    }

    override suspend fun provisionMoon(
        shipName: String,
        parentName: String,
        keyMaterial: String
    ): ControlResult {
        return stubClient.provisionMoon(shipName, parentName, keyMaterial)
    }

    override suspend fun pairWithPlanet(hostUrl: String, accessCode: String): ControlResult {
        return stubClient.pairWithPlanet(hostUrl, accessCode)
    }
}
