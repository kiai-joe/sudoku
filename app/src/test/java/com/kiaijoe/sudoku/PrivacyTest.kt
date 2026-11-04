package com.kiaijoe.sudoku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The privacy policy (docs/privacy-policy.md) says Sumi asks for no
 * permissions and can't reach the internet. This fails if that stops being
 * true, so the policy gets updated along with the app.
 */
class PrivacyTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun theAppAsksForNoPermissions() {
        assertFalse("A permission was added; update docs/privacy-policy.md", manifest.contains("uses-permission"))
    }

    @Test
    fun thePolicyIsThere() {
        val policy = File("../docs/privacy-policy.md")
        assertTrue(policy.exists())
        assertTrue(policy.readText().contains("collects nothing"))
    }
}
