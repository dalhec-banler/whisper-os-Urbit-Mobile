package io.nativeplanet.home.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "whisper_home")

/** The few things the user sets once: who can reach them, which tools show, what is done or muted. */
class Prefs(private val context: Context) {
    private val reach = stringSetPreferencesKey("reach_ships")
    private val muted = stringSetPreferencesKey("muted_ships")
    private val done = stringSetPreferencesKey("done_ids")
    private val tools = stringPreferencesKey("tools_order")

    companion object {
        /** Default tool list: the words on the home screen, in order. Keys match Tools.kt. */
        const val DEFAULT_TOOLS = "messages,people,things,camera,notes,maps,calls"
    }

    val reachShips: Flow<Set<String>> = context.store.data.map { it[reach] ?: emptySet() }
    val mutedShips: Flow<Set<String>> = context.store.data.map { it[muted] ?: emptySet() }
    val doneIds: Flow<Set<String>> = context.store.data.map { it[done] ?: emptySet() }
    val toolOrder: Flow<List<String>> = context.store.data.map { (it[tools] ?: DEFAULT_TOOLS).split(',').filter { s -> s.isNotBlank() } }

    suspend fun setReach(ship: String, canReach: Boolean) = context.store.edit { p ->
        val cur = (p[reach] ?: emptySet()).toMutableSet(); if (canReach) cur.add(ship) else cur.remove(ship); p[reach] = cur
    }
    suspend fun setMuted(ship: String, isMuted: Boolean) = context.store.edit { p ->
        val cur = (p[muted] ?: emptySet()).toMutableSet(); if (isMuted) cur.add(ship) else cur.remove(ship); p[muted] = cur
    }
    suspend fun markDone(id: String) = context.store.edit { p -> p[done] = (p[done] ?: emptySet()) + id }
    suspend fun setTools(order: List<String>) = context.store.edit { p -> p[tools] = order.joinToString(",") }
}
