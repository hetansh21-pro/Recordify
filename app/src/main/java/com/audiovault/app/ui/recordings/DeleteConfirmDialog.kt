package com.audiovault.app.ui.recordings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.audiovault.app.data.local.entity.RecordingEntity
import com.audiovault.app.ui.theme.StateRecordingRed

@Composable
fun DeleteConfirmDialog(
    recording: RecordingEntity,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Delete Recording?", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete \"${recording.title}\"? The audio file will be removed from your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = StateRecordingRed)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
