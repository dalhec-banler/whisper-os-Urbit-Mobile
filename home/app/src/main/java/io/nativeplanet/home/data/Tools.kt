package io.nativeplanet.home.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import io.nativeplanet.home.BuildConfig
import io.nativeplanet.home.model.HostedApp
import io.nativeplanet.home.model.Tool

/**
 * Every app on the phone in one list, Urbit-hosted and Android alike. The only
 * visible difference is `sendable`. Hosted apps open through the ROM's own
 * hosted path so they get the fixed one-task-per-app WebView.
 */
class Tools(private val context: Context) {
    companion object {
        private const val TAG = "WhisperHome.Tools"
        const val ACTION_OPEN_URBIT_APP = "io.nativeplanet.action.OPEN_URBIT_APP"
        const val ROM_LAUNCHER = "com.android.launcher3"
        const val ROM_HOSTED_ACTIVITY = "com.android.launcher3.WhisperHostedAppsActivity"

        /** Home-screen words → what they open. Hosted desks are matched by desk name. */
        val HOME_WORDS: Map<String, List<String>> = mapOf(
            "messages" to listOf("desk:groups", "com.android.messaging"),
            "people" to listOf("home:people"),
            "things" to listOf("desk:grove", "com.android.gallery3d"),
            "camera" to listOf("app.grapheneos.camera", "com.android.camera2"),
            "notes" to listOf("desk:kin", "home:notes"),
            "maps" to listOf("com.android.maps", "org.osmdroid", "app.vanadium.browser"),
            "calls" to listOf("com.android.dialer"),
            "browser" to listOf("app.vanadium.browser"),
            "calendar" to listOf("com.android.calendar"),
            "terminal" to listOf("desk:webterm"),
        )
    }

    fun all(hosted: List<HostedApp>): List<Tool> {
        val list = mutableListOf<Tool>()
        hosted.forEach { h ->
            list.add(Tool(key = "desk:${h.desk}", name = h.title.lowercase(), sendable = true, hosted = h))
        }
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(main, 0).forEach { ri ->
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName || pkg == ROM_LAUNCHER) return@forEach
            val name = ri.loadLabel(pm).toString().lowercase()
            if (list.none { it.packageName == pkg }) list.add(Tool(key = pkg, name = name, sendable = false, packageName = pkg))
        }
        return list.sortedBy { it.name }
    }

    fun resolveHomeWord(word: String, tools: List<Tool>): Tool? {
        val candidates = HOME_WORDS[word] ?: listOf(word)
        for (c in candidates) {
            if (c.startsWith("home:")) return Tool(key = c, name = word, sendable = false)
            tools.firstOrNull { it.key == c }?.let { return it }
        }
        return tools.firstOrNull { it.name == word }
    }

    /** Returns false when nothing could be opened. */
    fun open(tool: Tool): Boolean {
        Log.i(TAG, "open ${tool.key} hosted=${tool.hosted != null} openable=${tool.hosted?.openable} pkg=${tool.packageName}")
        try {
            tool.hosted?.let { h ->
                if (!h.openable) return false
                val url = h.startUrl?.takeIf { it.isNotBlank() }
                    ?: (BuildConfig.SHIP_URL.trimEnd('/') + "/" + h.basePath!!.trimStart('/'))
                val i = Intent(ACTION_OPEN_URBIT_APP)
                    .setClassName(ROM_LAUNCHER, ROM_HOSTED_ACTIVITY)
                    .putExtra("id", h.id).putExtra("title", h.title).putExtra("desk", h.desk)
                    .putExtra("launchMode", h.launchMode).putExtra("androidPackage", h.androidPackage)
                    .putExtra("startUrl", url).putExtra("hostPatp", h.hostPatp)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i); return true
            }
            tool.packageName?.let { pkg ->
                val i = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
                context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "open ${tool.key} failed: ${e.javaClass.simpleName}")
        }
        return false
    }

    fun openPackage(pkg: String): Boolean {
        return try {
            val i = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
            context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (e: Exception) { false }
    }
}
