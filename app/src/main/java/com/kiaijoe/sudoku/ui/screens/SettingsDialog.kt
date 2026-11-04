package com.kiaijoe.sudoku.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiaijoe.sudoku.data.Settings
import com.kiaijoe.sudoku.ui.MISTAKE_LIMIT
import com.kiaijoe.sudoku.ui.theme.InkTheme
import com.kiaijoe.sudoku.ui.theme.swatch

@Composable
fun SettingsDialog(settings: Settings, onChange: (Settings) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                ThemePicker(selected = settings.theme, onSelect = { onChange(settings.copy(theme = it)) })
                Spacer(Modifier.height(8.dp))
                SettingRow(
                    title = "Larger numbers",
                    subtitle = "Bigger digits on the board and number pad",
                    checked = settings.largeNumbers,
                    onCheckedChange = { onChange(settings.copy(largeNumbers = it)) },
                )
                SettingRow(
                    title = "Show mistakes right away",
                    subtitle = if (settings.instantCheck) {
                        "Wrong numbers turn red as you place them"
                    } else {
                        "Nothing is marked until you tap Check"
                    },
                    checked = settings.instantCheck,
                    onCheckedChange = { onChange(settings.copy(instantCheck = it)) },
                )
                SettingRow(
                    title = "Mistake limit",
                    subtitle = if (settings.instantCheck) {
                        "End the game after $MISTAKE_LIMIT wrong numbers"
                    } else {
                        "Only applies when mistakes are shown right away"
                    },
                    checked = settings.mistakeLimit && settings.instantCheck,
                    enabled = settings.instantCheck,
                    onCheckedChange = { onChange(settings.copy(mistakeLimit = it)) },
                )
                SettingRow(
                    title = "Auto-remove notes",
                    subtitle = "Clear matching notes when you place a number",
                    checked = settings.autoRemoveNotes,
                    onCheckedChange = { onChange(settings.copy(autoRemoveNotes = it)) },
                )
                SettingRow(
                    title = "Highlight same numbers",
                    subtitle = "Shade every cell with the selected number",
                    checked = settings.highlightSameNumbers,
                    onCheckedChange = { onChange(settings.copy(highlightSameNumbers = it)) },
                )
                SettingRow(
                    title = "Vibration",
                    subtitle = "A light tap when you place a number or finish a row, column or box",
                    checked = settings.haptics,
                    onCheckedChange = { onChange(settings.copy(haptics = it)) },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** One chip per ink palette, each with a tiny preview: paper, ink and accent. */
@Composable
private fun ThemePicker(selected: InkTheme, onSelect: (InkTheme) -> Unit) {
    val systemDark = isSystemInDarkTheme()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (theme in InkTheme.entries) {
            val (paper, accent, ink) = theme.swatch(systemDark)
            val isSelected = theme == selected
            Surface(
                onClick = { onSelect(theme) },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { this.selected = isSelected },
            ) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(paper),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("5", color = ink, style = MaterialTheme.typography.labelLarge)
                            Text("7", color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(theme.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}
