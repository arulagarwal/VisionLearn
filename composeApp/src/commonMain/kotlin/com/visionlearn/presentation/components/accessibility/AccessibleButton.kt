package com.visionlearn.presentation.components.accessibility

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionlearn.presentation.theme.CVIColors
import com.visionlearn.presentation.theme.LocalAccessibilityConfig

/**
 * Accessible button with CVI-optimized styling
 * - Large touch targets
 * - High contrast options
 * - Clear visual feedback
 */
@Composable
fun AccessibleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = text,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    highContrast: Boolean = false,
    minSize: Dp? = null
) {
    val config = LocalAccessibilityConfig.current
    val actualMinSize = minSize ?: config.minTouchTargetSize.dp
    
    val buttonColors = if (highContrast) {
        ButtonDefaults.buttonColors(
            containerColor = CVIColors.Black,
            contentColor = CVIColors.White,
            disabledContainerColor = CVIColors.DarkGray,
            disabledContentColor = CVIColors.White.copy(alpha = 0.5f)
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    }
    
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = buttonColors,
        shape = RoundedCornerShape(16.dp),
        border = if (highContrast) BorderStroke(3.dp, CVIColors.White) else null,
        modifier = modifier
            .defaultMinSize(minWidth = actualMinSize, minHeight = actualMinSize)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = config.fontSizes.label
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Large action button for primary actions in learning activities
 */
@Composable
fun LargeActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = CVIColors.Yellow,
    icon: @Composable (() -> Unit)? = null
) {
    val config = LocalAccessibilityConfig.current
    val textColor = CVIColors.getContrastColor(color)
    
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(config.minTouchTargetSize.dp + 32.dp)
            .semantics {
                this.contentDescription = text
                this.role = Role.Button
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = config.fontSizes.title
                )
            )
        }
    }
}

/**
 * Choice button for multiple choice activities
 */
@Composable
fun ChoiceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isCorrect: Boolean? = null, // null = not answered yet
    color: Color = CVIColors.Blue
) {
    val config = LocalAccessibilityConfig.current
    
    val backgroundColor = when {
        isCorrect == true -> CVIColors.Success
        isCorrect == false -> CVIColors.Red
        isSelected -> color
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val borderColor = when {
        isCorrect == true -> CVIColors.Success
        isCorrect == false -> CVIColors.Red
        isSelected -> color
        else -> MaterialTheme.colorScheme.outline
    }
    
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor.copy(alpha = 0.2f),
            contentColor = CVIColors.getContrastColor(backgroundColor.copy(alpha = 0.2f))
        ),
        border = BorderStroke(3.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = config.minTouchTargetSize.dp)
            .semantics {
                this.contentDescription = when {
                    isCorrect == true -> "$text, correct"
                    isCorrect == false -> "$text, incorrect"
                    isSelected -> "$text, selected"
                    else -> text
                }
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = config.fontSizes.body
            ),
            textAlign = TextAlign.Center
        )
    }
}
