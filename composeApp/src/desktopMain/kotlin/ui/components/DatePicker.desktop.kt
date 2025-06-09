package ui.components 

import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


actual fun formatPlatformDate(dateMillis: PlatformDateMillis, formatPattern: String): String {
    val sdf = SimpleDateFormat(formatPattern, Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

@Composable
actual fun ExpectedDatePickerDialog(
    show: Boolean,
    initialDateMillis: PlatformDateMillis?,
    onDismissRequest: () -> Unit,
    onDateSelected: (dateMillis: PlatformDateMillis) -> Unit
) {
    if (show) {
        // For Desktop, a simple JOptionPane can be used for a native feel,
        // or a custom Composable dialog. Here's a simple Composable dialog.
        // A more robust solution might involve a dedicated date picker library for Compose Desktop.

        var tempYear by remember { mutableStateOf("") }
        var tempMonth by remember { mutableStateOf("") }
        var tempDay by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(initialDateMillis) {
            val calendar = Calendar.getInstance()
            initialDateMillis?.let { calendar.timeInMillis = it }
            tempYear = calendar.get(Calendar.YEAR).toString()
            tempMonth = (calendar.get(Calendar.MONTH) + 1).toString() // Month is 0-indexed
            tempDay = calendar.get(Calendar.DAY_OF_MONTH).toString()
        }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Select Date") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text("Enter date (YYYY-MM-DD):") // Simplified input for desktop
                    androidx.compose.material.TextField(
                        value = tempYear,
                        onValueChange = { tempYear = it },
                        label = { Text("Year (YYYY)") }
                    )
                    androidx.compose.material.TextField(
                        value = tempMonth,
                        onValueChange = { tempMonth = it },
                        label = { Text("Month (MM)") }
                    )
                    androidx.compose.material.TextField(
                        value = tempDay,
                        onValueChange = { tempDay = it },
                        label = { Text("Day (DD)") }
                    )
                    errorMessage?.let {
                        Text(it, color = androidx.compose.ui.graphics.Color.Red)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val year = tempYear.toInt()
                            val month = tempMonth.toInt() // 1-12
                            val day = tempDay.toInt()

                            if (month < 1 || month > 12 || day < 1 || day > 31) { // Basic validation
                                errorMessage = "Invalid date values."
                                return@Button
                            }

                            val calendar = Calendar.getInstance().apply {
                                clear() // Clear all fields first
                                set(year, month - 1, day) // Month is 0-indexed for Calendar
                                // Normalize to midnight
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onDateSelected(calendar.timeInMillis)
                            errorMessage = null
                        } catch (e: NumberFormatException) {
                            errorMessage = "Please enter valid numbers for date parts."
                        }
                    }
                ) {
                    Text("OK")
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