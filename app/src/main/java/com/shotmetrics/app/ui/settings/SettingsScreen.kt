package com.shotmetrics.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shotmetrics.app.domain.model.AngleUnit
import com.shotmetrics.app.domain.model.DistanceUnit
import com.shotmetrics.app.domain.model.LengthUnit
import com.shotmetrics.app.ui.editor.CaliberPickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val angleUnit by viewModel.angleUnit.collectAsStateWithLifecycle()
    val lengthUnit by viewModel.lengthUnit.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val referenceSize by viewModel.referenceSize.collectAsStateWithLifecycle()
    val turretClickValue by viewModel.turretClickValue.collectAsStateWithLifecycle()
    val turretAngleUnit by viewModel.turretAngleUnit.collectAsStateWithLifecycle()
    val defaultDistance by viewModel.defaultDistance.collectAsStateWithLifecycle()
    val defaultCaliber by viewModel.defaultCaliber.collectAsStateWithLifecycle()
    val autoShowOverlays by viewModel.autoShowOverlays.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                        onBack()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = "Units") {
                SettingRow(label = "Angle Unit") {
                    AngleUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = angleUnit == unit,
                            onClick = { viewModel.setAngleUnit(unit) },
                            label = { Text(unit.abbreviation) }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingRow(label = "Length Unit") {
                    LengthUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = lengthUnit == unit,
                            onClick = { viewModel.setLengthUnit(unit) },
                            label = { Text(unit.abbreviation) }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingRow(label = "Distance Unit") {
                    DistanceUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = distanceUnit == unit,
                            onClick = { viewModel.setDistanceUnit(unit) },
                            label = { Text(unit.abbreviation) }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }

            SectionCard(title = "Reference") {
                var refText by remember(referenceSize) {
                    mutableStateOf(if (referenceSize == referenceSize.toLong().toDouble()) referenceSize.toLong().toString() else referenceSize.toString())
                }
                OutlinedTextField(
                    value = refText,
                    onValueChange = { input ->
                        refText = input
                        input.toDoubleOrNull()?.let { if (it > 0) viewModel.setReferenceSize(it) }
                    },
                    label = { Text("Reference Size (${lengthUnit.abbreviation})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            SectionCard(title = "Turret") {
                var expanded by remember { mutableStateOf(false) }
                val currentPresetLabel = viewModel.turretPresets.find {
                    it.clickValue == turretClickValue && it.angleUnit == turretAngleUnit
                }?.name ?: "Custom"

                Column {
                    Text("Preset", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = currentPresetLabel,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        viewModel.turretPresets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name) },
                                onClick = {
                                    viewModel.applyTurretPreset(preset)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                var clickText by remember(turretClickValue) { mutableStateOf(turretClickValue.toString()) }
                OutlinedTextField(
                    value = clickText,
                    onValueChange = { input ->
                        clickText = input
                        input.toDoubleOrNull()?.let { if (it > 0) viewModel.setTurretClickValue(it) }
                    },
                    label = { Text("Click Value (${turretAngleUnit.abbreviation})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                SettingRow(label = "Turret Unit") {
                    AngleUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = turretAngleUnit == unit,
                            onClick = { viewModel.setTurretAngleUnit(unit) },
                            label = { Text(unit.abbreviation) }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }

            SectionCard(title = "Defaults") {
                var distText by remember(defaultDistance) {
                    mutableStateOf(if (defaultDistance == defaultDistance.toLong().toDouble()) defaultDistance.toLong().toString() else defaultDistance.toString())
                }
                OutlinedTextField(
                    value = distText,
                    onValueChange = { input ->
                        distText = input
                        input.toDoubleOrNull()?.let { if (it > 0) viewModel.setDefaultDistance(it) }
                    },
                    label = { Text("Default Distance (${distanceUnit.abbreviation})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))

                var showCaliberPicker by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = defaultCaliber.ifBlank { "Tap to select" },
                    onValueChange = {},
                    label = { Text("Default Caliber") },
                    modifier = Modifier.fillMaxWidth().clickable { showCaliberPicker = true },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        Text(
                            "\u25BC",
                            modifier = Modifier.clickable { showCaliberPicker = true },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                if (showCaliberPicker) {
                    CaliberPickerSheet(
                        onSelect = { name ->
                            viewModel.setDefaultCaliber(name)
                            showCaliberPicker = false
                        },
                        onDismiss = { showCaliberPicker = false }
                    )
                }
            }

            SectionCard(title = "Display") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-show Overlays", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Automatically display overlay circles after tapping Finish",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoShowOverlays,
                        onCheckedChange = { viewModel.setAutoShowOverlays(it) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            content()
        }
    }
}
