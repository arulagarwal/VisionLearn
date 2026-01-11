package com.visionlearn.presentation.components.accessibility

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.visionlearn.presentation.theme.CVIColors
import kotlinx.coroutines.delay

/**
 * Switch access mode for users who use external switches
 */
enum class SwitchAccessMode {
    OFF,            // Normal touch mode
    SINGLE_SWITCH,  // One switch: auto-scan, switch to select
    DUAL_SWITCH     // Two switches: one to move, one to select
}

/**
 * State for switch access scanning
 */
data class SwitchScanState(
    val isEnabled: Boolean = false,
    val mode: SwitchAccessMode = SwitchAccessMode.OFF,
    val currentIndex: Int = 0,
    val scanIntervalMs: Long = 1500,
    val highlightColor: Color = CVIColors.Yellow
)

/**
 * Controller for switch access functionality
 */
class SwitchAccessController {
    private val _state = mutableStateOf(SwitchScanState())
    val state: State<SwitchScanState> = _state
    
    var onSelect: ((Int) -> Unit)? = null
    
    fun enable(mode: SwitchAccessMode) {
        _state.value = _state.value.copy(
            isEnabled = true,
            mode = mode,
            currentIndex = 0
        )
    }
    
    fun disable() {
        _state.value = _state.value.copy(
            isEnabled = false,
            mode = SwitchAccessMode.OFF
        )
    }
    
    fun moveNext(itemCount: Int) {
        if (!_state.value.isEnabled) return
        _state.value = _state.value.copy(
            currentIndex = (_state.value.currentIndex + 1) % itemCount
        )
    }
    
    fun movePrevious(itemCount: Int) {
        if (!_state.value.isEnabled) return
        _state.value = _state.value.copy(
            currentIndex = if (_state.value.currentIndex > 0) {
                _state.value.currentIndex - 1
            } else {
                itemCount - 1
            }
        )
    }
    
    fun select() {
        if (!_state.value.isEnabled) return
        onSelect?.invoke(_state.value.currentIndex)
    }
    
    fun setScanInterval(intervalMs: Long) {
        _state.value = _state.value.copy(scanIntervalMs = intervalMs)
    }
    
    fun setHighlightColor(color: Color) {
        _state.value = _state.value.copy(highlightColor = color)
    }
}

/**
 * Composable that provides switch access scanning behavior
 */
@Composable
fun SwitchAccessScanner(
    controller: SwitchAccessController,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable (currentIndex: Int, isHighlighted: (Int) -> Boolean) -> Unit
) {
    val state by controller.state
    
    // Auto-scan for single switch mode
    if (state.isEnabled && state.mode == SwitchAccessMode.SINGLE_SWITCH) {
        LaunchedEffect(state.scanIntervalMs, itemCount) {
            while (true) {
                delay(state.scanIntervalMs)
                controller.moveNext(itemCount)
            }
        }
    }
    
    Box(modifier = modifier) {
        content(state.currentIndex) { index ->
            state.isEnabled && index == state.currentIndex
        }
    }
}

/**
 * A container that can be highlighted for switch access
 */
@Composable
fun SwitchAccessItem(
    isHighlighted: Boolean,
    highlightColor: Color = CVIColors.Yellow,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) highlightColor else Color.Transparent,
        animationSpec = tween(200),
        label = "border_color"
    )
    
    // Pulsing animation when highlighted
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Box(
        modifier = modifier
            .border(
                width = if (isHighlighted) 4.dp else 0.dp,
                color = borderColor.copy(alpha = if (isHighlighted) pulseAlpha else 0f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(if (isHighlighted) 4.dp else 0.dp)
    ) {
        content()
    }
}

/**
 * Grid of items with switch access support
 */
@Composable
fun <T> SwitchAccessGrid(
    items: List<T>,
    controller: SwitchAccessController,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    itemContent: @Composable (T, Boolean, () -> Unit) -> Unit
) {
    SwitchAccessScanner(
        controller = controller,
        itemCount = items.size,
        modifier = modifier
    ) { currentIndex, isHighlighted ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    row.forEachIndexed { colIndex, item ->
                        val index = rowIndex * columns + colIndex
                        Box(modifier = Modifier.weight(1f)) {
                            SwitchAccessItem(
                                isHighlighted = isHighlighted(index)
                            ) {
                                itemContent(
                                    item,
                                    isHighlighted(index)
                                ) {
                                    controller.onSelect?.invoke(index)
                                }
                            }
                        }
                    }
                    // Fill remaining space if row is incomplete
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Button that responds to switch access
 */
@Composable
fun SwitchAccessButton(
    text: String,
    onClick: () -> Unit,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
    highlightColor: Color = CVIColors.Yellow,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    SwitchAccessItem(
        isHighlighted = isHighlighted,
        highlightColor = highlightColor,
        modifier = modifier
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics {
                    contentDescription = if (isHighlighted) {
                        "$text, highlighted, press switch to select"
                    } else {
                        text
                    }
                }
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
