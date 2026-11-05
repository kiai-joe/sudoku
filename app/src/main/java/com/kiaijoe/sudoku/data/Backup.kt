package com.kiaijoe.sudoku.data

import android.content.SharedPreferences
import com.kiaijoe.sudoku.engine.tr
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A backup file that can't be restored, with a message for the player. */
class BackupException(message: String) : Exception(message)

/**
 * A backup file of everything Sumi keeps: saved games, the daily record,
 * statistics, achievements, lessons and settings. It is every entry of
 * Sumi's preferences with its type, as JSON:
 *
 * ```
 * {"format": "sumi-backup", "version": 1, "entries": {"stats_EASY_won": {"type": "int", "value": 13}, …}}
 * ```
 *
 * The file goes wherever the player chooses through Android's file picker,
 * so it needs no permission and never leaves the phone unless they move it.
 */
object Backup {
    const val FORMAT = "sumi-backup"
    const val VERSION = 1

    fun export(prefs: SharedPreferences): String {
        val entries = JSONObject()
        for ((key, value) in prefs.all.toSortedMap()) {
            val (type, json) = when (value) {
                is String -> "string" to value
                is Int -> "int" to value
                is Long -> "long" to value
                is Boolean -> "boolean" to value
                is Float -> "float" to value.toDouble()
                is Set<*> -> "strings" to JSONArray(value.filterIsInstance<String>().sorted())
                else -> continue
            }
            entries.put(key, JSONObject().put("type", type).put("value", json))
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("entries", entries)
            .toString(2)
    }

    /**
     * Replaces everything in [prefs] with the backup in [text], then runs
     * [verify], which should read every value the app uses. If the file can't
     * be read, or [verify] fails, [prefs] is left as it was.
     *
     * @return how many entries were restored.
     * @throws BackupException when the file isn't a Sumi backup this version can read.
     */
    fun restore(prefs: SharedPreferences, text: String, verify: () -> Unit): Int {
        val entries = parse(text)
        val before = prefs.all.toMap()
        write(prefs, entries)
        try {
            verify()
        } catch (e: Exception) {
            write(prefs, before)
            throw BackupException(tr("This backup has values Sumi can't read, so nothing was changed.", "Nakala hii ina thamani ambazo Sumi haiwezi kusoma, kwa hiyo hakuna kilichobadilishwa."))
        }
        return entries.size
    }

    private fun parse(text: String): Map<String, Any> {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupException(tr("This file isn't a Sumi backup.", "Faili hili si nakala ya Sumi."))
        }
        if (root.optString("format") != FORMAT) throw BackupException(tr("This file isn't a Sumi backup.", "Faili hili si nakala ya Sumi."))
        val version = root.optInt("version", -1)
        if (version !in 1..VERSION) {
            throw BackupException(tr("This backup is from a newer version of Sumi. Update Sumi, then try again.", "Nakala hii ni ya toleo jipya zaidi la Sumi. Sasisha Sumi, kisha ujaribu tena."))
        }
        val entries = root.optJSONObject("entries") ?: throw BackupException(tr("This backup is empty.", "Nakala hii ni tupu."))
        val out = LinkedHashMap<String, Any>()
        try {
            for (key in entries.keys()) {
                val entry = entries.getJSONObject(key)
                out[key] = when (entry.getString("type")) {
                    "string" -> entry.getString("value")
                    "int" -> entry.getInt("value")
                    "long" -> entry.getLong("value")
                    "boolean" -> entry.getBoolean("value")
                    "float" -> entry.getDouble("value").toFloat()
                    "strings" -> entry.getJSONArray("value").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                    else -> throw BackupException(tr("This backup has values Sumi can't read, so nothing was changed.", "Nakala hii ina thamani ambazo Sumi haiwezi kusoma, kwa hiyo hakuna kilichobadilishwa."))
                }
            }
        } catch (e: JSONException) {
            throw BackupException(tr("This backup is damaged, so nothing was changed.", "Nakala hii imeharibika, kwa hiyo hakuna kilichobadilishwa."))
        }
        return out
    }

    private fun write(prefs: SharedPreferences, entries: Map<String, Any?>) {
        val editor = prefs.edit().clear()
        for ((key, value) in entries) {
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        // Written at once, so the app reloads from what's on disk.
        editor.commit()
    }
}
