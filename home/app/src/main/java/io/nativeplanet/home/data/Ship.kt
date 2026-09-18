package io.nativeplanet.home.data

import io.nativeplanet.home.model.Entry
import io.nativeplanet.home.model.Person
import io.nativeplanet.home.model.Source
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads what the moon knows: people, DMs, group activity. Every call is a scry
 * against local Eyre; nothing here writes. The activity agent on current moons
 * answers the v4 paths, so those are what we use.
 */
/** Wait for the relay's invite to land before the moon joins a planet-hosted group. */
private const val INVITE_SETTLE_MS = 3000L
/** DM writs fetched per thread for the record. */
private const val DM_PREVIEW_COUNT = 5

class Ship(private val eyre: Eyre) {
    data class Snapshot(
        val people: List<Person>, val entries: List<Entry>, val unreadDm: Set<String>,
        val delegated: Boolean = false, val parent: String? = null,
        /** The planet's groups (flag to title) and the moon's own, for "join the same groups". */
        val parentGroups: List<GroupRef> = emptyList(), val moonGroups: Set<String> = emptySet(),
    )

    data class GroupRef(val flag: String, val title: String, val hostedByParent: Boolean)

    /**
     * Delegation: when %nativeplanet-mobile mirrors the parent planet's DMs, the
     * record is the planet's, sends go through the planet, and the people are
     * the planet's correspondents. Falls back to the moon's own chat otherwise.
     */
    suspend fun mirror(): JSONObject? {
        val m = eyre.scry("nativeplanet-mobile/mirror") ?: return null
        val parent = m.optString("parent").takeIf { it.isNotBlank() } ?: return null
        val snap = m.optJSONObject("snapshot") ?: return null
        if (!snap.has("writs")) return null
        return m
    }

    suspend fun snapshot(self: String?): Snapshot {
        mirror()?.let { m -> return mirrorSnapshot(m) }
        return ownSnapshot(self)
    }

    private suspend fun mirrorSnapshot(m: JSONObject): Snapshot {
        val parent = m.optString("parent")
        val snap = m.getJSONObject("snapshot")
        val our = snap.optString("our", parent)
        val writsBy = snap.optJSONObject("writs") ?: JSONObject()
        val unreads = snap.optJSONObject("unreads")
        val people = mutableListOf<Person>()
        val entries = mutableListOf<Entry>()
        val unreadDm = mutableSetOf<String>()
        writsBy.keys().forEach { ship ->
            people.add(Person(ship, null, null))
            val u = unreads?.optJSONObject(ship)
            if ((u?.optInt("count", 0) ?: 0) > 0) unreadDm.add(ship)
            val writs = writsBy.optJSONObject(ship)?.optJSONObject("writs") ?: return@forEach
            writs.keys().forEach { key ->
                val w = writs.optJSONObject(key) ?: return@forEach
                val essay = w.optJSONObject("essay") ?: return@forEach
                val author = essay.optString("author")
                val text = renderInline(essay.optJSONArray("content"))
                if (text.isBlank()) return@forEach
                entries.add(Entry(
                    id = "mdm:$ship:$key", timeMs = essay.optLong("sent", 0L),
                    who = if (author == our) "You → $ship" else author,
                    ship = author, text = text, source = Source.MESSAGE, link = "apps/groups/dm/$ship",
                ))
            }
        }
        // Live responses since the snapshot: {"chat": {"whom": "~ship", "id": ..., "response": {"add": {"essay": ...}}}}
        val live = m.optJSONArray("live")
        if (live != null) for (i in 0 until live.length()) {
            val r = live.optJSONObject(i) ?: continue
            val whom = r.optString("whom").takeIf { it.isNotBlank() } ?: continue
            val add = r.optJSONObject("response")?.optJSONObject("add") ?: continue
            val essay = add.optJSONObject("essay") ?: continue
            val author = essay.optString("author")
            val text = renderInline(essay.optJSONArray("content"))
            if (text.isBlank()) continue
            val id = "mdm:$whom:${r.opt("id")}"
            if (entries.none { it.id == id }) entries.add(Entry(
                id = id, timeMs = essay.optLong("sent", 0L),
                who = if (author == our) "You → $whom" else author,
                ship = author, text = text, source = Source.MESSAGE, link = "apps/groups/dm/$whom",
            ))
            if (people.none { it.ship == whom }) people.add(Person(whom, null, null))
        }
        // Group activity is the moon's own: once it has joined the planet's groups, its %activity carries them.
        entries.addAll(activityEntries())
        val parentGroups = mutableListOf<GroupRef>()
        snap.optJSONObject("groups")?.let { g ->
            g.keys().forEach { flag ->
                val title = g.optJSONObject(flag)?.optJSONObject("meta")?.optString("title")?.takeIf { it.isNotBlank() } ?: flag.substringAfter('/')
                parentGroups.add(GroupRef(flag, title, hostedByParent = flag.startsWith("$parent/")))
            }
        }
        val moonGroups = eyre.scry("groups/groups/light")?.keys()?.asSequence()?.toSet() ?: emptySet()
        people.sortByDescending { p -> entries.filter { it.ship == p.ship || it.link?.endsWith(p.ship) == true }.maxOfOrNull { it.timeMs } ?: 0L }
        return Snapshot(people, entries.sortedByDescending { it.timeMs }, unreadDm, delegated = true, parent = parent,
            parentGroups = parentGroups.sortedBy { it.title.lowercase() }, moonGroups = moonGroups)
    }

    /**
     * Join one of the planet's groups as the moon. A group the planet hosts needs an invite
     * first, which the relay issues on the moon's behalf; the moon's own %groups then joins
     * with the token. Any other group is joined directly, which works for open groups and
     * leaves a knock pending for closed ones.
     */
    suspend fun joinGroup(self: String, group: GroupRef): Boolean {
        if (group.hostedByParent) {
            val ask = JSONObject().put("invite-moon", group.flag)
            if (!eyre.poke("nativeplanet-mobile", "json", ask, self)) return false
            kotlinx.coroutines.delay(INVITE_SETTLE_MS)
        }
        val join = JSONObject().put("flag", group.flag).put("join-all", true)
        return eyre.poke("groups", "group-join", join, self)
    }

    /** Send a DM as the planet, through the moon's mirror agent and the planet's relay. */
    suspend fun sendDm(self: String, ship: String, text: String): Boolean {
        val jon = JSONObject().put("send-dm", JSONObject().put("ship", ship).put("text", text))
        return eyre.poke("nativeplanet-mobile", "json", jon, self)
    }

    private suspend fun ownSnapshot(self: String?): Snapshot {
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
            val writs = eyre.scry("chat/dm/$ship/writs/newest/$DM_PREVIEW_COUNT/light")?.optJSONObject("writs") ?: continue
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
                    ship = author, text = text, source = Source.MESSAGE,
                    link = "apps/groups/dm/$ship",
                ))
            }
        }

        entries.addAll(activityEntries())

        people.sortByDescending { p -> entries.filter { it.ship == p.ship }.maxOfOrNull { it.timeMs } ?: 0L }
        return Snapshot(people, entries.sortedByDescending { it.timeMs }, unreadDm)
    }

    /** Group-level activity: one line per group or channel with unread activity, from the v4 summaries. */
    private suspend fun activityEntries(): List<Entry> {
        val out = mutableListOf<Entry>()
        eyre.scry("activity/v4/activity")?.let { act ->
            act.keys().forEach { key ->
                if (!key.startsWith("channel/") && !key.startsWith("group/")) return@forEach
                val s = act.optJSONObject(key) ?: return@forEach
                val count = s.optInt("count", 0)
                if (count <= 0) return@forEach
                val name = key.substringAfterLast('/').replace('-', ' ')
                out.add(Entry(
                    id = "act:$key", timeMs = s.optLong("recency", 0L),
                    who = name, ship = null,
                    text = if (count == 1) "1 new post" else "$count new posts",
                    source = if (key.startsWith("channel/chat")) Source.CHAT else Source.GROUP,
                    link = "apps/groups",
                ))
            }
        }
        return out
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
                        if (item.has("ship") && item.opt("ship") is String) sb.append(item.getString("ship"))
                        if (item.has("break")) sb.append(' ')
                    }
                }
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
