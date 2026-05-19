package io.nativeplanet.controller

import java.io.File

/**
 * NativePlanet Controller Implementation
 *
 * Controls the nativeplanet_vere init service via system properties.
 * Manages BootPackage files and reads runtime logs.
 *
 * Note: This is a stub implementation for Gate 3.
 * Full implementation requires Android system permissions.
 */
class NativePlanetControllerImpl : INativePlanetController {

    companion object {
        private const val BOOTPACKAGE_PATH = "/data/nativeplanet/boot-package.json"
        private const val LOG_PATH = "/data/nativeplanet/logs/nativeplanet-vere-launch.log"
        private const val SHIPS_DIR = "/data/nativeplanet/ships"

        private const val PROP_VERE_ENABLED = "nativeplanet.vere.enabled"
        private const val PROP_SERVICE_STATE = "init.svc.nativeplanet_vere"
    }

    override fun runtimeStatus(): RuntimeStatus {
        // TODO: Read from getprop init.svc.nativeplanet_vere
        // Possible values: running, stopped, stopping, starting, restarting
        val state = getSystemProperty(PROP_SERVICE_STATE)
        return when (state) {
            "running" -> RuntimeStatus.RUNNING
            "stopped" -> RuntimeStatus.STOPPED
            "starting" -> RuntimeStatus.STARTING
            "stopping" -> RuntimeStatus.STOPPING
            "restarting" -> RuntimeStatus.STARTING
            else -> RuntimeStatus.UNKNOWN
        }
    }

    override fun activeShip(): String? {
        if (runtimeStatus() != RuntimeStatus.RUNNING) return null

        // Read ship name from BootPackage
        return readBootPackageShip()
    }

    override fun prepareBootPackage(pkg: BootPackage): BootPackageResult {
        val errors = validateBootPackage(pkg)
        if (errors.isNotEmpty()) {
            return BootPackageResult.ValidationError(errors)
        }

        return try {
            val json = serializeBootPackage(pkg)
            File(BOOTPACKAGE_PATH).writeText(json)
            BootPackageResult.Success(BOOTPACKAGE_PATH)
        } catch (e: Exception) {
            BootPackageResult.WriteError(e.message ?: "Unknown error")
        }
    }

    override fun startRuntime(): RuntimeStartResult {
        val status = runtimeStatus()
        if (status == RuntimeStatus.RUNNING) {
            val ship = activeShip() ?: "unknown"
            return RuntimeStartResult.AlreadyRunning(ship)
        }

        if (!File(BOOTPACKAGE_PATH).exists()) {
            return RuntimeStartResult.NoBootPackage
        }

        return try {
            setSystemProperty(PROP_VERE_ENABLED, "1")
            RuntimeStartResult.Success
        } catch (e: Exception) {
            RuntimeStartResult.StartFailed(e.message ?: "Unknown error")
        }
    }

    override fun stopRuntime(): RuntimeStopResult {
        val status = runtimeStatus()
        if (status == RuntimeStatus.STOPPED) {
            return RuntimeStopResult.AlreadyStopped
        }

        return try {
            setSystemProperty(PROP_VERE_ENABLED, "0")
            RuntimeStopResult.Success
        } catch (e: Exception) {
            RuntimeStopResult.StopFailed(e.message ?: "Unknown error")
        }
    }

    override fun restartRuntime(): RuntimeStartResult {
        val stopResult = stopRuntime()
        if (stopResult is RuntimeStopResult.StopFailed) {
            return RuntimeStartResult.StartFailed("Stop failed: ${stopResult.reason}")
        }

        // Brief delay for clean shutdown
        Thread.sleep(2000)

        return startRuntime()
    }

    override fun logs(maxLines: Int): List<RuntimeLogLine> {
        val logFile = File(LOG_PATH)
        if (!logFile.exists()) return emptyList()

        return try {
            logFile.readLines()
                .takeLast(maxLines)
                .mapNotNull { parseLogLine(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun health(): RuntimeHealth {
        val status = runtimeStatus()
        val ship = activeShip()
        val bootPackageValid = File(BOOTPACKAGE_PATH).exists()
        val pierExists = ship?.let { File("$SHIPS_DIR/$it/.urb").exists() } ?: false

        val lastError = logs(50)
            .filter { it.level == "ERROR" }
            .lastOrNull()
            ?.message

        return RuntimeHealth(
            status = status,
            ship = ship,
            pierExists = pierExists,
            bootPackageValid = bootPackageValid,
            lastError = lastError,
            uptimeSeconds = null // TODO: Track start time
        )
    }

    // --- Private helpers ---

    private fun validateBootPackage(pkg: BootPackage): List<String> {
        val errors = mutableListOf<String>()

        if (pkg.ship.isBlank()) {
            errors.add("ship name is required")
        }

        if (pkg.pillPath.isBlank()) {
            errors.add("pillPath is required")
        } else if (!File(pkg.pillPath).exists()) {
            errors.add("pill not found: ${pkg.pillPath}")
        }

        if (pkg.pierPath.isBlank()) {
            errors.add("pierPath is required")
        }

        if (pkg.bootMode == BootMode.MOON && pkg.parent.isNullOrBlank()) {
            errors.add("parent is required for MOON boot mode")
        }

        if (pkg.bootMode == BootMode.MOON) {
            errors.add("MOON boot mode not implemented in v0")
        }

        if (pkg.packageVersion != 1) {
            errors.add("unsupported packageVersion: ${pkg.packageVersion}")
        }

        return errors
    }

    private fun serializeBootPackage(pkg: BootPackage): String {
        // Simple JSON serialization without external dependencies
        return buildString {
            appendLine("{")
            appendLine("""  "ship": "${pkg.ship}",""")
            appendLine("""  "parent": ${if (pkg.parent != null) "\"${pkg.parent}\"" else "null"},""")
            appendLine("""  "pillPath": "${pkg.pillPath}",""")
            appendLine("""  "pierPath": "${pkg.pierPath}",""")
            appendLine("""  "bootMode": "${pkg.bootMode.name}",""")
            appendLine("""  "keyMaterialRef": "${pkg.keyMaterialRef}",""")
            appendLine("""  "networkConfig": {},""")
            appendLine("""  "delegationConfig": {""")
            appendLine("""    "canReadNotifications": ${pkg.delegationConfig.canReadNotifications},""")
            appendLine("""    "canMirrorMessages": ${pkg.delegationConfig.canMirrorMessages},""")
            appendLine("""    "canRequestPosts": ${pkg.delegationConfig.canRequestPosts},""")
            appendLine("""    "canRequestReplies": ${pkg.delegationConfig.canRequestReplies},""")
            appendLine("""    "canAccessFiles": ${pkg.delegationConfig.canAccessFiles},""")
            appendLine("""    "revocationEpoch": ${pkg.delegationConfig.revocationEpoch}""")
            appendLine("""  },""")
            appendLine("""  "createdAtMs": ${pkg.createdAtMs},""")
            appendLine("""  "packageVersion": ${pkg.packageVersion}""")
            append("}")
        }
    }

    private fun readBootPackageShip(): String? {
        val file = File(BOOTPACKAGE_PATH)
        if (!file.exists()) return null

        return try {
            val content = file.readText()
            // Simple extraction - find "ship": "value"
            val regex = """"ship"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(content)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLogLine(line: String): RuntimeLogLine? {
        // Format: [2026-05-19 10:30:45] [INFO] message
        val regex = """\[([^\]]+)\]\s*\[([^\]]+)\]\s*(.*)""".toRegex()
        val match = regex.find(line) ?: return null
        return RuntimeLogLine(
            timestamp = match.groupValues[1],
            level = match.groupValues[2],
            message = match.groupValues[3]
        )
    }

    private fun getSystemProperty(name: String): String {
        // TODO: Use Android SystemProperties or ProcessBuilder for getprop
        return try {
            val process = ProcessBuilder("getprop", name).start()
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun setSystemProperty(name: String, value: String) {
        // TODO: Use Android SystemProperties or ProcessBuilder for setprop
        // Note: Requires system permissions
        ProcessBuilder("setprop", name, value).start().waitFor()
    }
}
