package io.nativeplanet.controller

/**
 * NativePlanet Controller Interface
 *
 * Provides control over the Urbit runtime from Android applications.
 * Used by Whisper Launcher to manage the nativeplanet_vere init service.
 */
interface INativePlanetController {

    /**
     * Get current runtime status.
     */
    fun runtimeStatus(): RuntimeStatus

    /**
     * Get the active ship name, or null if no ship is running.
     */
    fun activeShip(): String?

    /**
     * Prepare and validate a BootPackage.
     * Writes the package to /data/nativeplanet/boot-package.json if valid.
     *
     * @param pkg The BootPackage to prepare
     * @return Result indicating success or validation errors
     */
    fun prepareBootPackage(pkg: BootPackage): BootPackageResult

    /**
     * Start the runtime.
     * Requires a valid BootPackage to be prepared first.
     *
     * @return Result indicating success or failure reason
     */
    fun startRuntime(): RuntimeStartResult

    /**
     * Stop the runtime gracefully.
     *
     * @return Result indicating success or failure reason
     */
    fun stopRuntime(): RuntimeStopResult

    /**
     * Restart the runtime.
     * Equivalent to stop + start with a brief delay.
     *
     * @return Result indicating success or failure reason
     */
    fun restartRuntime(): RuntimeStartResult

    /**
     * Get recent runtime logs.
     *
     * @param maxLines Maximum number of log lines to return
     * @return List of log lines, most recent last
     */
    fun logs(maxLines: Int): List<RuntimeLogLine>

    /**
     * Get runtime health status.
     * Includes service state, pier existence, and basic health checks.
     *
     * @return Current health status
     */
    fun health(): RuntimeHealth
}
