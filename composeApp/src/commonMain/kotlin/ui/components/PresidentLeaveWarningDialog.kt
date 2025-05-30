package ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun PresidentLeaveWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cannot Leave Team") },
        text = {
            Text(
                "As the team president, you must transfer your role to another member before leaving. " +
                        "Please promote someone to president first."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("OK")
            }
        }
    )
}