package com.johnpaulpatigas.weightcast.feature.weight_logging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.DateTimeUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightLoggingDialog(
    uiState: WeightLoggingUiState,
    onWeightInputChanged: (String) -> Unit,
    onUnitSelected: (WeightUnit) -> Unit,
    onTimestampSelected: (Long) -> Unit,
    onNotesInputChanged: (String) -> Unit,
    onSaveEntry: (allowDuplicate: Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val currentLdt = remember(uiState.timestamp) {
        DateTimeUtils.toLocalDateTime(uiState.timestamp)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = if (uiState.isEditing) "Edit Weight Entry" else "Log Weight",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Unit Selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    WeightUnit.entries.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = uiState.selectedUnit == unit,
                            onClick = { onUnitSelected(unit) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = WeightUnit.entries.size
                            )
                        ) {
                            Text(unit.displayName)
                        }
                    }
                }

                // Weight Input Field
                OutlinedTextField(
                    value = uiState.weightInput,
                    onValueChange = onWeightInputChanged,
                    label = { Text("Weight (${uiState.selectedUnit.symbol})") },
                    placeholder = { Text(if (uiState.selectedUnit == WeightUnit.KG) "e.g., 70.5" else "e.g., 155.4") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.validationError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.validationError != null) {
                    Text(
                        text = uiState.validationError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Date & Time Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateTimeUtils.formatDate(uiState.timestamp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "Pick Time")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateTimeUtils.formatTime(uiState.timestamp))
                    }
                }

                // Optional Notes Field
                OutlinedTextField(
                    value = uiState.notesInput,
                    onValueChange = onNotesInputChanged,
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g., Morning weigh-in, after workout") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveEntry(false) },
                enabled = !uiState.isLoading
            ) {
                Text(if (uiState.isEditing) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.timestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            // Preserve local time
                            val selectedLocalDate = Instant.ofEpochMilli(selectedDate)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val newLdt = LocalDateTime.of(
                                selectedLocalDate,
                                currentLdt.toLocalTime()
                            )
                            onTimestampSelected(DateTimeUtils.toEpochMillis(newLdt))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = currentLdt.hour,
            initialMinute = currentLdt.minute,
            is24Hour = false
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                val newLdt = LocalDateTime.of(
                                    currentLdt.toLocalDate(),
                                    java.time.LocalTime.of(
                                        timePickerState.hour,
                                        timePickerState.minute
                                    )
                                )
                                onTimestampSelected(DateTimeUtils.toEpochMillis(newLdt))
                                showTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    // Duplicate Warning Dialog
    if (uiState.duplicateWarningEntry != null) {
        AlertDialog(
            onDismissRequest = { onSaveEntry(false) },
            title = { Text("Duplicate Entry Detected") },
            text = {
                Text(
                    "A measurement of ${uiState.duplicateWarningEntry.getFormattedWeight(uiState.selectedUnit)} " +
                            "was already recorded for ${DateTimeUtils.formatDateTime(uiState.duplicateWarningEntry.timestamp)}. " +
                            "Would you like to overwrite/update this entry or save anyway?"
                )
            },
            confirmButton = {
                Button(onClick = { onSaveEntry(true) }) {
                    Text("Save Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        )
    }
}
