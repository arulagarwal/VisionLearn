package com.visionlearn.presentation.components.accessibility

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.visionlearn.presentation.theme.CVIColors
import com.visionlearn.presentation.theme.LocalAccessibilityConfig

/**
 * Accessible card for learning content
 * Optimized for CVI with:
 * - Large touch targets
 * - High contrast borders
 * - Clear visual hierarchy
 */
@Composable
fun AccessibleCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    contentDescription: String = title,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    size: Dp? = null,
    highContrast: Boolean = false,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val config = LocalAccessibilityConfig.current
    val actualSize = size ?: config.minTouchTargetSize.dp * 3
    
    val cardColors = if (highContrast) {
        CardDefaults.cardColors(
            containerColor = CVIColors.Black,
            contentColor = CVIColors.White
        )
    } else {
        CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    }
    
    val border = if (highContrast) {
        BorderStroke(4.dp, CVIColors.White)
    } else {
        BorderStroke(2.dp, borderColor)
    }
    
    Card(
        colors = cardColors,
        shape = RoundedCornerShape(20.dp),
        border = border,
        modifier = modifier
            .size(actualSize)
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (content != null) {
                content()
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = config.fontSizes.title
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = config.fontSizes.body
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Learning module card with icon and color coding
 */
@Composable
fun ModuleCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconContent: @Composable (() -> Unit)? = null,
    accentColor: Color = CVIColors.Yellow,
    isLocked: Boolean = false
) {
    val config = LocalAccessibilityConfig.current
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(3.dp, accentColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onClick)
            .semantics {
                this.contentDescription = if (isLocked) "$title, locked" else title
                this.role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon/Image area
            Box(
                modifier = Modifier
                    .size(config.minTouchTargetSize.dp + 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (iconContent != null) {
                    iconContent()
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = config.fontSizes.title
                    ),
                    color = if (isLocked) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = config.fontSizes.body
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Image card for learning activities
 */
@Composable
fun ImageCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    isSelected: Boolean = false,
    isCorrect: Boolean? = null,
    imageContent: @Composable BoxScope.() -> Unit
) {
    val config = LocalAccessibilityConfig.current
    val size = config.minTouchTargetSize.dp * 2
    
    val actualBorderColor = when {
        isCorrect == true -> CVIColors.Success
        isCorrect == false -> CVIColors.Red
        isSelected -> borderColor
        else -> MaterialTheme.colorScheme.outline
    }
    
    val borderWidth = if (isSelected || isCorrect != null) 4.dp else 2.dp
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(borderWidth, actualBorderColor),
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = when {
                    isCorrect == true -> "$contentDescription, correct"
                    isCorrect == false -> "$contentDescription, incorrect"
                    isSelected -> "$contentDescription, selected"
                    else -> contentDescription
                }
                this.role = Role.Button
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = imageContent
        )
    }
}
