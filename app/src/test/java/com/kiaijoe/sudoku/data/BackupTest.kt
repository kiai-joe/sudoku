package com.kiaijoe.sudoku.data

import android.content.Context
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.ui.theme.DarkMode
import com.kiaijoe.sudoku.ui.theme.InkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences(GameRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private lateinit var repo: GameRepository

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
        repo = GameRepository(context)
    }

    /** A phone with some of everything: stats, a daily record, achievements, lessons and settings. */
    private fun fillIn() {
        repo.recordStart(Difficulty.HARD)
        repo.recordWin(Difficulty.HARD, seconds = 611, hints = 0)
        repo.recordKillerStart()
        repo.recordDailyWin(day = 20_000, seconds = 300)
        repo.recordDailyWin(day = 20_001, seconds = 280)
        repo.addAchievements(setOf(Achievement.FIRST_STAMP, Achievement.STEADY_HAND), day = 20_001)
        repo.markLessonDone("X_WING")
        repo.saveSettings(Settings(theme = InkTheme.BAMBOO, largeNumbers = true, haptics = false, darkMode = DarkMode.DARK))
    }

    @Test
    fun aBackupRestoresEverythingOnAnotherPhone() {
        fillIn()
        val before = prefs.all
        val file = repo.exportBackup()

        // A new phone: nothing saved yet.
        prefs.edit().clear().commit()
        val restored = repo.restoreBackup(file)

        assertEquals(before.size, restored)
        assertEquals(before, prefs.all)
        assertEquals(DifficultyStats(played = 1, won = 1, bestSeconds = 611, timedWins = 1, totalSeconds = 611, noHintWins = 1), repo.loadStats()[Difficulty.HARD])
        assertEquals(mapOf(20_000L to 300L, 20_001L to 280L), repo.loadDailyResults())
        assertEquals(setOf(Achievement.FIRST_STAMP, Achievement.STEADY_HAND), repo.loadAchievements().keys)
        assertEquals(setOf("X_WING"), repo.loadLessonsDone())
        assertEquals(InkTheme.BAMBOO, repo.loadSettings().theme)
        assertEquals(DarkMode.DARK, repo.loadSettings().darkMode)
    }

    @Test
    fun restoringReplacesWhatWasThere() {
        fillIn()
        val file = repo.exportBackup()
        repo.markLessonDone("SWORDFISH")
        repo.restoreBackup(file)
        assertEquals(setOf("X_WING"), repo.loadLessonsDone())
    }

    @Test
    fun aFileThatIsNotABackupChangesNothing() {
        fillIn()
        val before = prefs.all
        for (text in listOf("", "hello", "{}", """{"format": "something-else", "version": 1, "entries": {}}""")) {
            try {
                repo.restoreBackup(text)
                fail("restored \"$text\"")
            } catch (e: BackupException) {
                assertTrue(e.message!!.isNotBlank())
            }
            assertEquals(before, prefs.all)
        }
    }

    @Test
    fun aBackupFromANewerVersionIsRefused() {
        fillIn()
        val newer = repo.exportBackup().replace("\"version\": ${Backup.VERSION}", "\"version\": ${Backup.VERSION + 1}")
        try {
            repo.restoreBackup(newer)
            fail("restored a newer backup")
        } catch (e: BackupException) {
            assertTrue(e.message!!.contains("newer version"))
        }
    }

    /** A value of the wrong type would crash the app on its next start, so it's rolled back. */
    @Test
    fun aValueTheAppCantReadIsRolledBack() {
        fillIn()
        val before = prefs.all
        val broken = """
            {"format": "sumi-backup", "version": 1, "entries": {
              "stats_HARD_won": {"type": "string", "value": "lots"}
            }}
        """.trimIndent()
        try {
            repo.restoreBackup(broken)
            fail("restored a value of the wrong type")
        } catch (e: BackupException) {
            assertEquals(before, prefs.all)
        }
    }

    @Test
    fun androidBackupAndTransferIncludeTheSaveFile() {
        val file = "path=\"${GameRepository.PREFS_NAME}.xml\""
        val rules = File("src/main/res/xml/backup_rules.xml").readText()
        val extraction = File("src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue(rules.contains(file))
        // Both Google backup and phone-to-phone transfer, on Android 12 and newer.
        assertEquals(2, Regex(Regex.escape(file)).findAll(extraction).count())
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
    }
}
