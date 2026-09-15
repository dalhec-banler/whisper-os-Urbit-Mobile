package io.nativeplanet.home.data

import android.util.Log
import io.nativeplanet.home.model.Entry
import io.nativeplanet.home.model.Person
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads what the moon knows: people, DMs, group activity. Every call is a scry
 * against local Eyre; nothing here writes. The activity agent on current moons
 * answers the v4 paths, so those are what we use.
 */
class Ship(private val eyre: Eyre) {
    companion object { private const val TAG = "WhisperHome.Ship" }

    data class Snapshot(val people: List<Person>, val entries: List<Entry>, val unreadDm: Set<String>)

    suspend fun snapshot(self: String?): Snapshot {
        val contacts = eyre.scry("contacts/all")
        val people = mutableListOf<Person>()
        contacts?.keys()?.forEach { ship ->
            val o = contacts.optJSONObject(ship)
            if (ship == self) return@forEach
            people.add(Person(ship, o?.optString("nickname")?.takeIf { it.isNotBlank() }, o?.optString("avatar")?.takeIf { it.isNotBlank() }))
        }

        val dms: List<String> = eyre.scry("chat/dm")?.optJSONArray("list")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
        dms.forEach { ship -> if (people.none { it.ship == ship }) people.add(Person(ship, null, null)) }

        val unreads = eyre.scry("chat/unreads")
        val unreadDm = mutableSetOf<String>()
        val entries = mutableListOf<Entry>()
        for (ship in dms) {
            val u = unreads?.optJSONObject(ship)
            val count = u?.optInt("count", 0) ?: 0
            if (count > 0) unreadDm.add(ship)
            val writs = eyre.scry("chat/dm/$ship/writs/newest/5/light")?.optJSONObject("writs") ?: continue
            writs.keys().forEach { key ->
                val w = writs.optJSONObject(key) ?: return@forEach
                val essay = w.optJSONObject("essay") ?: return@forEach
                val author = essay.optString("author")
                val sent = essay.optLong("sent", 0L)
                val text = renderInline(essay.optJSONArray("content"))
                if (text.isBlank()) return@forEach
                val nick = people.firstOrNull { it.ship == author }?.display ?: author
                entries.add(Entry(
                    id = "dm:$ship:$key", timeMs = sent,
                    who = if (author == self) "You → ${people.firstOrNull { it.ship == ship }?.display ?: ship}" else nick,
                    ship = author, text = text, source = "MESSAGE",
                    link = "apps/groups/dm/$ship",
                ))
            }
        }

        // Group-level activity: one line per group/channel with unread activity, from the v4 summaries.
        eyre.scry("activity/v4/activity")?.let { act ->
            act.keys().forEach { key ->
                if (!key.startsWith("channel/") && !key.startsWith("group/")) return@forEach
                val s = act.optJSONObject(key) ?: return@forEach
                val count = s.optInt("count", 0)
                if (count <= 0) return@forEach
                val name = key.substringAfterLast('/').replace('-', ' ')
                entries.add(Entry(
                    id = "act:$key", timeMs = s.optLong("recency", 0L),
                    who = name, ship = null,
                    text = if (count == 1) "1 new post" else "$count new posts",
                    source = if (key.startsWith("channel/chat")) "CHAT" else "GROUP",
                    link = "apps/groups",
                ))
            }
        }

        people.sortByDescending { p -> entries.filter { it.ship == p.ship }.maxOfOrNull { it.timeMs } ?: 0L }
        return Snapshot(people, entries.sortedByDescending { it.timeMs }, unreadDm)
    }

    private fun renderInline(content: JSONArray?): String {
        if (content == null) return ""
        val sb = StringBuilder()
        for (i in 0 until content.length()) {
            val block = content.optJSONObject(i) ?: continue
            val inline = block.optJSONArray("inline") ?: continue
            for (j in 0 until inline.length()) {
                when (val item = inline.get(j)) {
                    is String -> sb.append(item)
                    is JSONObject -> {
                        item.optJSONObject("link")?.let { sb.append(it.optString("content").ifEmpty { it.optString("href") }) }
                        item.optString("bold").takeIf { it.isNotEmpty() }?.let { sb.append(it) }
                        item.optString("italics").takeIf { it.isNotEmpty() }?.let { sb.append(it) }
                        item.optJSONObject("ship")?.let { sb.append(it.toString()) }
                        if (item.has("ship") && item.opt("ship") is String) sb.append(item.getString("ship"))
                        if (item.has("break")) sb.append(' ')
                    }
                }
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
