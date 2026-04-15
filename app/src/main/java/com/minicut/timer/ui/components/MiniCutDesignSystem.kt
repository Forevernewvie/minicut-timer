package com.minicut.timer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

val MiniCutCardShape = RoundedCornerShape(28.dp)
val MiniCutPanelShape = RoundedCornerShape(22.dp)
val MiniCutPillShape = RoundedCornerShape(16.dp)

enum class MiniCutInlineFeedbackTone {
    Info,
    Caution,
}

@Composable
fun MiniCutBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.035f),
                ),
            ),
        ),
        content = content,
    )
}

@Composable
fun MiniCutSectionHeader(
    kicker: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = MiniCutPillShape,
        ) {
            Text(
                text = kicker,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun MiniCutMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    supporting: String? = null,
) {
    Card(
        modifier = modifier,
        shape = MiniCutPanelShape,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.11f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = tint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun MiniCutInlineFeedback(
    message: String,
    modifier: Modifier = Modifier,
    tone: MiniCutInlineFeedbackTone = MiniCutInlineFeedbackTone.Info,
) {
    val accent =
        when (tone) {
            MiniCutInlineFeedbackTone.Info -> MaterialTheme.colorScheme.primary
            MiniCutInlineFeedbackTone.Caution -> MaterialTheme.colorScheme.tertiary
        }
    val container =
        when (tone) {
            MiniCutInlineFeedbackTone.Info -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            MiniCutInlineFeedbackTone.Caution -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MiniCutPanelShape,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
        )
    }
}
