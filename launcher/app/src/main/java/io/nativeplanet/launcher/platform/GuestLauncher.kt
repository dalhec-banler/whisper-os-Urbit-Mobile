package io.nativeplanet.launcher.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

data class LauncherAppInfo(
    val label: String,
    val packageName: String,
    val className: String
)

object GuestLauncher {
    fun installedApps(context: Context): List<LauncherAppInfo> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager
            .queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val packageName = activity.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null

                LauncherAppInfo(
                    label = info.loadLabel(context.packageManager).toString(),
                    packageName = packageName,
                    className = activity.name
                )
            }
            .distinctBy { "${it.packageName}/${it.className}" }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    fun launchApp(context: Context, app: LauncherAppInfo): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            ?: Intent().setClassName(app.packageName, app.className)
        return start(context, intent)
    }

    fun launchDialer(context: Context): Boolean {
        return start(context, Intent(Intent.ACTION_DIAL))
    }

    fun launchBrowser(context: Context, url: String? = null): Boolean {
        val intent = if (url == null) {
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        return start(context, intent)
    }

    fun launchMessaging(context: Context): Boolean {
        return start(context, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
    }

    fun launchCamera(context: Context): Boolean {
        return start(context, Intent("android.media.action.STILL_IMAGE_CAMERA"))
    }

    fun launchSettings(context: Context): Boolean {
        return start(context, Intent(Settings.ACTION_SETTINGS))
    }

    private fun start(context: Context, intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
