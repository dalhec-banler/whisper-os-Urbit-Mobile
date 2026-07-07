package io.nativeplanet.launcher.ui.home

import android.content.Context
import io.nativeplanet.launcher.domain.model.HostedApp
import io.nativeplanet.launcher.platform.LauncherAppInfo
import io.nativeplanet.launcher.ui.components.GlyphKind
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONArray
import org.json.JSONObject

data class HomeTileSpec(
    val id: String,
    val label: String,
    val action: String? = null,
    val packageName: String? = null,
    val className: String? = null,
    val hostedPath: String? = null,
    val glyphName: String? = null,
    val worldName: String = AppWorld.SYSTEM.name,
    val hostPatp: String? = null,
    val page: Int = 0,
    val cell: Int = -1
) {
    val glyph: GlyphKind?
        get() = glyphName?.let {
            runCatching { GlyphKind.valueOf(it) }.getOrNull()
        }

    val world: AppWorld
        get() = runCatching { AppWorld.valueOf(worldName) }.getOrDefault(AppWorld.DEV)
}

object HomeLayoutStore {
    private const val PREFS = "whisper_home_layout"
    private const val KEY_TILES = "tiles"
    const val COLUMNS = 4
    const val ROWS = 5
    const val PAGE_SIZE = COLUMNS * ROWS
    const val ACTION_HOSTED_PENDING = "hosted_pending"
    const val ACTION_HOSTED_LOCAL = "hosted_local"

    fun load(context: Context, hostPatp: String?): List<HomeTileSpec> {
        val encoded = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TILES, null)

        val loaded = encoded?.let(::decode).orEmpty()
        return if (loaded.isNotEmpty()) {
            val migrated = assignMissingCells(loaded.map { migrateTile(it, hostPatp) })
            if (migrated != loaded) {
                save(context, migrated)
            }
            migrated
        } else {
            defaultTiles(hostPatp).also { save(context, it) }
        }
    }

    fun save(context: Context, tiles: List<HomeTileSpec>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TILES, encode(tiles))
            .apply()
        HomeLayoutEvents.notifyChanged()
    }

    fun addApp(context: Context, app: LauncherAppInfo, hostPatp: String?): List<HomeTileSpec> {
        val existing = load(context, hostPatp).toMutableList()
        val tile = app.toHomeTile()
        if (existing.none { it.id == tile.id }) {
            existing.add(tile.withFirstOpenCell(existing))
            save(context, existing)
        }
        return existing
    }

    fun addHostedApp(context: Context, app: HostedApp, hostPatp: String?): List<HomeTileSpec> {
        val existing = load(context, hostPatp).toMutableList()
        val tile = app.toHomeTile(hostPatp)
        val index = existing.indexOfFirst { it.id == tile.id }
        if (index >= 0) {
            existing[index] = existing[index].copy(
                label = tile.label,
                action = tile.action,
                hostedPath = tile.hostedPath,
                glyphName = tile.glyphName,
                worldName = tile.worldName,
                hostPatp = tile.hostPatp
            )
        } else {
            existing.add(tile.withFirstOpenCell(existing))
        }
        save(context, existing)
        return existing
    }

    fun remove(context: Context, tileId: String, hostPatp: String?): List<HomeTileSpec> {
        val updated = load(context, hostPatp).filterNot { it.id == tileId }
        save(context, updated)
        return updated
    }

    fun reset(context: Context, hostPatp: String?): List<HomeTileSpec> {
        val defaults = defaultTiles(hostPatp)
        save(context, defaults)
        return defaults
    }

    fun move(tiles: List<HomeTileSpec>, from: Int, to: Int): List<HomeTileSpec> {
        if (from !in tiles.indices || to !in tiles.indices || from == to) return tiles
        val mutable = tiles.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }

    fun moveToCell(
        tiles: List<HomeTileSpec>,
        tileId: String,
        page: Int,
        cell: Int
    ): List<HomeTileSpec> {
        if (page < 0 || cell !in 0 until PAGE_SIZE) return tiles
        val moving = tiles.firstOrNull { it.id == tileId } ?: return tiles
        val target = tiles.firstOrNull { it.id != tileId && it.page == page && it.cell == cell }
        return tiles.map { tile ->
            when (tile.id) {
                moving.id -> tile.copy(page = page, cell = cell)
                target?.id -> tile.copy(page = moving.page, cell = moving.cell)
                else -> tile
            }
        }
    }

    fun moveToPage(
        tiles: List<HomeTileSpec>,
        tileId: String,
        targetPage: Int
    ): List<HomeTileSpec> {
        if (targetPage < 0) return tiles
        val moving = tiles.firstOrNull { it.id == tileId } ?: return tiles
        val targetCell = firstOpenCell(tiles, targetPage, excludeId = tileId) ?: moving.cell.coerceIn(0, PAGE_SIZE - 1)
        return moveToCell(tiles, tileId, targetPage, targetCell)
    }

    private fun LauncherAppInfo.toHomeTile(): HomeTileSpec {
        val glyph = glyphForApp(this)
        return HomeTileSpec(
            id = "app:$packageName/$className",
            label = label,
            packageName = packageName,
            className = className,
            glyphName = glyph?.name,
            worldName = world().name,
            cell = -1
        )
    }

    private fun HostedApp.toHomeTile(hostPatp: String?): HomeTileSpec {
        return HomeTileSpec(
            id = homeIdForHostedApp(this),
            label = title.lowercase(),
            action = ACTION_HOSTED_LOCAL,
            hostedPath = basePath,
            glyphName = glyphForHostedAppId(id).name,
            worldName = AppWorld.URBIT.name,
            hostPatp = hostPatp,
            cell = -1
        )
    }

    private fun migrateTile(tile: HomeTileSpec, hostPatp: String?): HomeTileSpec {
        var updated = tile
        if (updated.world == AppWorld.URBIT && updated.hostPatp == null) {
            updated = updated.copy(hostPatp = hostPatp)
        }

        updated = when {
            updated.packageName?.startsWith("app.grapheneos") == true ->
                updated.copy(worldName = AppWorld.SYSTEM.name)

            updated.id == "action:messages" && updated.action == "search" ->
                updated.copy(action = "messages")

            updated.id == "hosted:identity" ->
                updated.copy(
                    action = "settings",
                    packageName = null,
                    className = null,
                    hostedPath = null,
                    worldName = AppWorld.URBIT.name,
                    hostPatp = updated.hostPatp ?: hostPatp
                )

            updated.id.startsWith("hosted:") && updated.hostedPath != null ->
                updated.copy(
                    action = ACTION_HOSTED_LOCAL,
                    packageName = null,
                    className = null,
                    worldName = AppWorld.URBIT.name,
                    hostPatp = updated.hostPatp ?: hostPatp
                )

            updated.id.startsWith("hosted:") ->
                updated.copy(
                    action = ACTION_HOSTED_PENDING,
                    packageName = null,
                    className = null,
                    hostedPath = null,
                    worldName = AppWorld.URBIT.name,
                    hostPatp = updated.hostPatp ?: hostPatp
                )

            else -> updated
        }

        return updated
    }

    private fun defaultTiles(hostPatp: String?): List<HomeTileSpec> = listOf(
        HomeTileSpec("action:phone", "phone", action = "phone", glyphName = GlyphKind.Phone.name),
        HomeTileSpec("action:messages", "messages", action = "messages", glyphName = GlyphKind.Messages.name),
        HomeTileSpec("action:browser", "browser", action = "browser", glyphName = GlyphKind.Browser.name),
        HomeTileSpec("action:camera", "camera", action = "camera", glyphName = GlyphKind.Camera.name),
        HomeTileSpec("hosted:tlon", "tlon", action = ACTION_HOSTED_PENDING, glyphName = GlyphKind.Messages.name, worldName = AppWorld.URBIT.name, hostPatp = hostPatp),
        HomeTileSpec("hosted:talk", "talk", action = ACTION_HOSTED_PENDING, glyphName = GlyphKind.Talk.name, worldName = AppWorld.URBIT.name, hostPatp = hostPatp),
        HomeTileSpec("hosted:memex", "memex", action = ACTION_HOSTED_PENDING, glyphName = GlyphKind.Memex.name, worldName = AppWorld.URBIT.name, hostPatp = hostPatp),
        HomeTileSpec("hosted:notes", "notes", action = ACTION_HOSTED_PENDING, glyphName = GlyphKind.Notes.name, worldName = AppWorld.URBIT.name, hostPatp = hostPatp),
        HomeTileSpec("hosted:hark", "hark", action = ACTION_HOSTED_PENDING, glyphName = GlyphKind.Hark.name, worldName = AppWorld.URBIT.name, hostPatp = hostPatp),
        HomeTileSpec("hosted:dojo", "dojo", action = ACTION_HOSTED_PENDING, glyphName = GlyphKind.Dojo.name, worldName = AppWorld.URBIT.name, hostPatp = hostPatp),
        HomeTileSpec("hosted:files", "files", action = ACTION_HOSTED_PENDING, glyphName = GlyphKind.Files.name, worldName = AppWorld.URBIT.name, hostPatp = hostPatp),
        HomeTileSpec("hosted:identity", "identity", action = "settings", glyphName = GlyphKind.Contact.name, worldName = AppWorld.URBIT.name, hostPatp = hostPatp)
    ).mapIndexed { index, tile -> tile.copy(page = 0, cell = index) }

    private fun assignMissingCells(tiles: List<HomeTileSpec>): List<HomeTileSpec> {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        return tiles.mapIndexed { index, tile ->
            val requested = tile.page to tile.cell
            if (tile.cell in 0 until PAGE_SIZE && requested !in occupied) {
                occupied.add(requested)
                tile
            } else {
                val fallbackPage = index / PAGE_SIZE
                val open = firstOpenCellInOccupied(occupied, fallbackPage)
                    ?: firstOpenCellInOccupied(occupied, 0)
                    ?: (occupied.size / PAGE_SIZE to occupied.size % PAGE_SIZE)
                occupied.add(open)
                tile.copy(page = open.first, cell = open.second)
            }
        }
    }

    private fun HomeTileSpec.withFirstOpenCell(existing: List<HomeTileSpec>): HomeTileSpec {
        val pageCount = maxOf(1, (existing.maxOfOrNull { it.page } ?: 0) + 1)
        for (page in 0..pageCount) {
            val cell = firstOpenCell(existing, page)
            if (cell != null) return copy(page = page, cell = cell)
        }
        return copy(page = pageCount, cell = 0)
    }

    private fun firstOpenCell(
        tiles: List<HomeTileSpec>,
        page: Int,
        excludeId: String? = null
    ): Int? {
        val occupied = tiles
            .asSequence()
            .filter { it.id != excludeId && it.page == page && it.cell in 0 until PAGE_SIZE }
            .map { it.cell }
            .toSet()
        return (0 until PAGE_SIZE).firstOrNull { it !in occupied }
    }

    private fun firstOpenCellInOccupied(
        occupied: Set<Pair<Int, Int>>,
        preferredPage: Int
    ): Pair<Int, Int>? {
        return (0 until PAGE_SIZE)
            .firstOrNull { cell -> preferredPage to cell !in occupied }
            ?.let { preferredPage to it }
    }

    private fun encode(tiles: List<HomeTileSpec>): String {
        val array = JSONArray()
        tiles.forEach { tile ->
            array.put(JSONObject().apply {
                put("id", tile.id)
                put("label", tile.label)
                put("action", tile.action)
                put("packageName", tile.packageName)
                put("className", tile.className)
                put("hostedPath", tile.hostedPath)
                put("glyphName", tile.glyphName)
                put("worldName", tile.worldName)
                put("hostPatp", tile.hostPatp)
                put("page", tile.page)
                put("cell", tile.cell)
            })
        }
        return array.toString()
    }

    private fun decode(encoded: String): List<HomeTileSpec> {
        return runCatching {
            val array = JSONArray(encoded)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                HomeTileSpec(
                    id = obj.optString("id"),
                    label = obj.optString("label"),
                    action = obj.optString("action").takeIf { it.isNotBlank() && it != "null" },
                    packageName = obj.optString("packageName").takeIf { it.isNotBlank() && it != "null" },
                    className = obj.optString("className").takeIf { it.isNotBlank() && it != "null" },
                    hostedPath = obj.optString("hostedPath").takeIf { it.isNotBlank() && it != "null" },
                    glyphName = obj.optString("glyphName").takeIf { it.isNotBlank() && it != "null" },
                    worldName = obj.optString("worldName", AppWorld.DEV.name),
                    hostPatp = obj.optString("hostPatp").takeIf { it.isNotBlank() && it != "null" },
                    page = obj.optInt("page", 0),
                    cell = if (obj.has("cell")) obj.optInt("cell", -1) else -1
                )
            }.filter { it.id.isNotBlank() && it.label.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}

object HomeLayoutEvents {
    val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun notifyChanged() {
        changes.tryEmit(Unit)
    }
}

private fun homeIdForHostedApp(app: HostedApp): String {
    return when (app.id.lowercase()) {
        "groups" -> "hosted:tlon"
        "webterm" -> "hosted:dojo"
        else -> "hosted:${app.id.lowercase()}"
    }
}

private fun glyphForHostedAppId(id: String): GlyphKind {
    return when (id.lowercase()) {
        "groups", "tlon", "talk" -> GlyphKind.Messages
        "webterm", "dojo" -> GlyphKind.Dojo
        "grove", "files" -> GlyphKind.Files
        "kin", "memex" -> GlyphKind.Memex
        "landscape" -> GlyphKind.Browser
        "hark" -> GlyphKind.Hark
        else -> GlyphKind.Browser
    }
}
