package com.voxmind.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.voxmind.app.ui.theme.AmberWarning
import com.voxmind.app.ui.theme.CyanAccent
import com.voxmind.app.ui.theme.EmeraldSuccess
import com.voxmind.app.ui.theme.IndigoPrimary
import com.voxmind.app.ui.theme.Slate800

@Composable
fun DeepSeekActionButtons(
    enabled: Boolean,
    isLoading: Boolean,
    currentAction: String?,
    onOrganizeThoughts: () -> Unit,
    onGenerateBullets: () -> Unit,
    onSummarize: () -> Unit,
    onExtractReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Organize Thoughts
        ActionButton(
            text = "🧠 Organize Thoughts",
            tint = CyanAccent,
            isLoading = isLoading && currentAction == "organize",
            enabled = enabled && !isLoading,
            onClick = onOrganizeThoughts
        )

        // 2. Bullet Point List
        ActionButton(
            text = "📋 Bullet Points",
            tint = EmeraldSuccess,
            isLoading = isLoading && currentAction == "bullets",
            enabled = enabled && !isLoading,
            onClick = onGenerateBullets
        )

        // 3. Summarize
        ActionButton(
            text = "📝 Summarize",
            tint = IndigoPrimary,
            isLoading = isLoading && currentAction == "summary",
            enabled = enabled && !isLoading,
            onClick = onSummarize
        )

        // 4. Extract Reminders
        ActionButton(
            text = "⏰ Extract Reminders",
            tint = AmberWarning,
            isLoading = isLoading && currentAction == "extract",
            enabled = enabled && !isLoading,
            onClick = onExtractReminders
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    tint: Color,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Slate800,
            contentColor = tint,
            disabledContainerColor = Slate800.copy(alpha = 0.5f),
            disabledContentColor = Color.Gray
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp),
                color = tint,
                strokeWidth = 2.dp
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) tint else Color.Gray
        )
    }
}
