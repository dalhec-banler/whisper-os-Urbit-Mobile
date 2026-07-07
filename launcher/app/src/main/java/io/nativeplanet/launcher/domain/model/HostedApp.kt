package io.nativeplanet.launcher.domain.model

data class HostedApp(
    val id: String,
    val desk: String,
    val title: String,
    val info: String,
    val launchMode: String,
    val basePath: String?,
    val startUrl: String?,
    val sourceUrl: String?,
    val imageUrl: String?,
    val version: String?,
    val website: String?,
    val availability: String,
    val androidPackage: String?,
    val pwaManifestUrl: String?,
    val recommended: Boolean,
    val hidden: Boolean,
    val mobileMetadata: Boolean
) {
    val isLaunchable: Boolean
        get() = launchMode == "local_webview" && !basePath.isNullOrBlank()
}

