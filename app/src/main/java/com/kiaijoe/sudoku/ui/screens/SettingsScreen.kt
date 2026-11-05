package com.kiaijoe.sudoku.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kiaijoe.sudoku.ui.BackupResult
import com.kiaijoe.sudoku.ui.components.ButtonTone
import com.kiaijoe.sudoku.ui.components.PillButton
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.data.Settings
import com.kiaijoe.sudoku.ui.MISTAKE_LIMIT
import com.kiaijoe.sudoku.ui.components.ListCard
import com.kiaijoe.sudoku.ui.components.ListRow
import com.kiaijoe.sudoku.ui.components.Overline
import com.kiaijoe.sudoku.ui.components.Page
import com.kiaijoe.sudoku.ui.components.RowChevron
import com.kiaijoe.sudoku.ui.components.SumiSwitch
import com.kiaijoe.sudoku.ui.components.SumiTopBar
import com.kiaijoe.sudoku.ui.components.card
import com.kiaijoe.sudoku.ui.icons.Lucide
import com.kiaijoe.sudoku.ui.theme.InkTheme
import com.kiaijoe.sudoku.ui.theme.Sora
import com.kiaijoe.sudoku.ui.theme.Sumi
import com.kiaijoe.sudoku.ui.theme.swatch

const val PRIVACY_POLICY_URL = "https://github.com/kiai-joe/sudoku/blob/main/docs/privacy-policy.md"

/** Settings, a page of its own: look, play and feel, then privacy. */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onBack: () -> Unit,
    onExportBackup: () -> String = { "" },
    onRestoreBackup: (String) -> BackupResult = { BackupResult(ok = false, message = "") },
) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = Sumi.colors
    // What the last save or restore did, shown under the backup card.
    var result by remember { mutableStateOf<BackupResult?>(null) }
    // A backup file read and waiting for the player to confirm the restore.
    var pending by remember { mutableStateOf<String?>(null) }

    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = onExportBackup()
        scope.launch {
            result = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }
                BackupResult(ok = true, message = "Backup saved. Keep it somewhere safe, like Drive or your computer.")
            } catch (e: Exception) {
                BackupResult(ok = false, message = "The backup couldn't be saved there. Try another place.")
            }
        }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                pending = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                }
            } catch (e: Exception) {
                result = BackupResult(ok = false, message = "That file couldn't be opened.")
            }
        }
    }
    Page {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            SumiTopBar("Settings", onBack = onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    Section("Look")
                    ThemePicker(selected = settings.theme, onSelect = { onChange(settings.copy(theme = it)) })
                    Spacer(Modifier.height(12.dp))
                    ListCard(
                        rows = listOf<@Composable () -> Unit>({
                            SwitchRow(
                                Lucide.Type,
                                "Larger numbers",
                                "Board and keys a quarter bigger",
                                settings.largeNumbers,
                            ) { onChange(settings.copy(largeNumbers = it)) }
                        }),
                    )

                    Section("Play")
                    ListCard(
                        rows = listOf<@Composable () -> Unit>(
                            {
                                SwitchRow(
                                    Lucide.Eye,
                                    "Show mistakes right away",
                                    if (settings.instantCheck) {
                                        "Wrong numbers turn red as you place them"
                                    } else {
                                        "Nothing is marked until you tap Check"
                                    },
                                    settings.instantCheck,
                                ) { onChange(settings.copy(instantCheck = it)) }
                            },
                            {
                                SwitchRow(
                                    Lucide.Flag,
                                    "Mistake limit",
                                    if (settings.instantCheck) {
                                        "End the game after $MISTAKE_LIMIT wrong numbers"
                                    } else {
                                        "Only applies when mistakes are shown right away"
                                    },
                                    settings.mistakeLimit && settings.instantCheck,
                                    enabled = settings.instantCheck,
                                ) { onChange(settings.copy(mistakeLimit = it)) }
                            },
                            {
                                SwitchRow(
                                    Lucide.Pencil,
                                    "Tidy pencil marks",
                                    "Clear matching notes when you place a number",
                                    settings.autoRemoveNotes,
                                ) { onChange(settings.copy(autoRemoveNotes = it)) }
                            },
                            {
                                SwitchRow(
                                    Lucide.Grid3x3,
                                    "Highlight same numbers",
                                    "Shade every cell with the selected number",
                                    settings.highlightSameNumbers,
                                ) { onChange(settings.copy(highlightSameNumbers = it)) }
                            },
                        ),
                    )

                    Section("Feel")
                    ListCard(
                        rows = listOf<@Composable () -> Unit>({
                            SwitchRow(
                                Lucide.Vibrate,
                                "Vibration",
                                "A light tap when you place a number or finish a row, column or box",
                                settings.haptics,
                            ) { onChange(settings.copy(haptics = it)) }
                        }),
                    )

                    Section("Your progress")
                    ListCard(
                        rows = listOf<@Composable () -> Unit>(
                            {
                                ListRow(
                                    Lucide.Download,
                                    "Save a backup file",
                                    subtitle = "Games, stats, streak, achievements and settings",
                                    onClick = { saveFile.launch("sumi-backup-${LocalDate.now()}.json") },
                                ) { RowChevron() }
                            },
                            {
                                ListRow(
                                    Lucide.Upload,
                                    "Restore from a backup",
                                    subtitle = "Bring your progress over from another phone",
                                    onClick = { openFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                                ) { RowChevron() }
                            },
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result?.message
                            ?: "Android's own backup also brings Sumi across when you set up a new phone, if it's turned on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (result?.ok) {
                            true -> c.matcha
                            false -> c.shu
                            null -> c.inkMuted
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    Spacer(Modifier.height(20.dp))
                    ListCard(
                        rows = listOf<@Composable () -> Unit>({
                            // Opens in the browser; Sumi itself has no internet access.
                            ListRow(
                                Lucide.ShieldCheck,
                                "Privacy",
                                subtitle = "Sumi collects nothing and works offline",
                                onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
                            ) { RowChevron() }
                        }),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    pending?.let { text ->
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = c.surface,
            title = { Text("Restore this backup?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "It replaces the games, stats, streak, achievements and settings on this phone.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                PillButton("Restore", { pending = null; result = onRestoreBackup(text) }, height = 44.dp)
            },
            dismissButton = { PillButton("Cancel", { pending = null }, tone = ButtonTone.Ghost, height = 44.dp) },
        )
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(20.dp))
    Overline(title)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    ) {
        Spacer(Modifier.width(10.dp))
        SumiSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** One card per palette, each with a tiny preview: paper, a printed number and one of yours. */
@Composable
private fun ThemePicker(selected: InkTheme, onSelect: (InkTheme) -> Unit) {
    val c = Sumi.colors
    val systemDark = isSystemInDarkTheme()
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (theme in InkTheme.entries) {
            val (paper, accent, ink) = theme.swatch(systemDark)
            val isSelected = theme == selected
            val shape = RoundedCornerShape(20.dp)
            Column(
                Modifier
                    .weight(1f)
                    .card(c, shape)
                    .then(if (isSelected) Modifier.border(2.dp, c.ai, shape) else Modifier)
                    .clip(shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(theme) })
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(paper),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("5", color = ink, fontFamily = Sora, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal)
                        Text("7", color = accent, fontFamily = Sora, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    theme.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) c.ai else c.ink,
                    maxLines = 1,
                )
            }
        }
    }
}
