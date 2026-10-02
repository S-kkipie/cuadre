package pe.aido.cuadre.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Strong ease-out: moves the moment you look, settles softly. Every UI transition uses it. */
val EaseOutStrong = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/**
 * Tap target that answers the finger: shrinks to 0.97 while pressed (fast in, a bit slower out).
 * Replaces the Material ripple, which reads as "Android default" on our flat surfaces.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = tween(if (pressed) 100 else 180, easing = EaseOutStrong),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

/** Text links dim instead of shrinking — scaling a word looks like a glitch. */
fun Modifier.pressableText(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (pressed) 0.55f else 1f, tween(120), label = "press")
    graphicsLayer { this.alpha = alpha }
        .clickable(source, indication = null, role = Role.Button, onClick = onClick)
}

/**
 * Status dot. When [live], a slow halo breathes out of it: ambient proof the listener is on,
 * slow enough (2.4 s) to never read as an alert.
 */
@Composable
fun LiveDot(color: Color, live: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(16.dp), contentAlignment = Alignment.Center) {
        if (live) {
            val t = rememberInfiniteTransition(label = "live")
            val p by t.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
                label = "halo",
            )
            Box(
                Modifier.size(8.dp)
                    .graphicsLayer {
                        val s = 1f + p * 1.6f
                        scaleX = s; scaleY = s
                        alpha = (1f - p) * 0.35f
                    }
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}
