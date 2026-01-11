package com.visionlearn.presentation.components.accessibility

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.visionlearn.presentation.theme.CVIColors
import com.visionlearn.presentation.theme.LocalAccessibilityConfig

/**
 * Module card for displaying learning modules on home screen
 * CVI-optimized with large touch targets and high contrast borders
 */
@Composable
fun ModuleCard(
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable (() -> Unit)? = null,
    isLocked: Boolean = false
) {
    val config = LocalAccessibilityConfig.current
    
    Card(
        onClick = { if (!isLocked) onClick() },
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 100.dp)
            .semantics {
                contentDescription = if (isLocked) {
                    "$title, coming soon"
                } else {
                    "$title: $description. Tap to open"
                }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                accentColor.copy(alpha = 0.15f)
            }
        ),
        border = BorderStroke(
            width = 3.dp,
            color = if (isLocked) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            } else {
                accentColor
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            if (iconContent != null) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = config.fontSizes.title
                    ),
                    color = if (isLocked) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        accentColor
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = config.fontSizes.body
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Lock indicator
            if (isLocked) {
                Text(
                    text = "🔒",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

/**
 * Image card for learning activities
 * Displays content with CVI-appropriate styling
 */
@Composable
fun ImageDisplayCard(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = CVIColors.Yellow.copy(alpha = 0.2f),
    borderColor: Color = CVIColors.Yellow
) {
    val config = LocalAccessibilityConfig.current
    
    Card(
        modifier = modifier.size(config.minTouchTargetSize.dp * 3),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = if (config.highContrast) {
            BorderStroke(4.dp, borderColor)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Stats card for progress display
 */
@Composable
fun StatCard(
    value: String,
    label: String,
    emoji: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Profile header card
 */
@Composable
fun ProfileHeaderCard(
    childName: String,
    cviPhase: String,
    preferredColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = preferredColor.copy(alpha = 0.2f)
        ),
        border = BorderStroke(2.dp, preferredColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Card(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(36.dp),
                colors = CardDefaults.cardColors(
                    containerColor = preferredColor
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = childName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = CVIColors.getContrastColor(preferredColor)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = childName,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = cviPhase,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
