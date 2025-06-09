package ui.components

import androidx.compose.runtime.Composable

/**
 * Represents a platform-specific date as milliseconds since epoch.
 */
typealias PlatformDateMillis = Long

/**
 * Expected function to format a platform date (milliseconds) into a string.
 * @param dateMillis The date in milliseconds since epoch.
 * @param formatPattern The desired date format pattern (e.g., "yyyy-MM-dd").
 * @return Formatted date string.
 */
expect fun formatPlatformDate(dateMillis: PlatformDateMillis, formatPattern: String): String

/**
 * Expected composable function to display a date picker dialog.
 * @param show Controls the visibility of the dialog.
 * @param initialDateMillis The initial date to be selected in the picker (optional).
 * @param onDismissRequest Called when the dialog is dismissed.
 * @param onDateSelected Called with the selected date in milliseconds since epoch.
 */
@Composable
expect fun ExpectedDatePickerDialog(
    show: Boolean,
    initialDateMillis: PlatformDateMillis?,
    onDismissRequest: () -> Unit,
    onDateSelected: (dateMillis: PlatformDateMillis) -> Unit
)