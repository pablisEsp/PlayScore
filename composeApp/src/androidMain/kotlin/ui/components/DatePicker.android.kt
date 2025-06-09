package ui.components 


import android.app.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        initialDateMillis?.let {
            calendar.timeInMillis = it
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = remember(context, onDismissRequest, onDateSelected) {
            DatePickerDialog(
                context,
                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDayOfMonth)
                        // Normalize to midnight to represent the date without time
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onDateSelected(selectedCalendar.timeInMillis)
                },
                year, month, day
            ).apply {
                setOnDismissListener {
                    onDismissRequest()
                }
            }
        }

        DisposableEffect(dialog) {
            dialog.show()
            onDispose {
                dialog.dismiss()
            }
        }
    }
}