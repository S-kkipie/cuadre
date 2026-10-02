package pe.aido.cuadre.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pe.aido.cuadre.R

/**
 * Cuadre — monochrome. Source of truth: .stitch/DESIGN.md.
 * Color is a signal, never decoration: [CuadreColors.paid] means real money arrived,
 * [CuadreColors.stale] means an old payment or a pending setup step. Everything else is ink.
 */
@Immutable
data class CuadreColors(
    val paper: Color = Color(0xFFFFFFFF),
    val ink: Color = Color(0xFF111111),
    val inkMuted: Color = Color(0xFF6B6B6B),
    val hairline: Color = Color(0xFFEAEAEA),
    val disabledFill: Color = Color(0xFFE6E6E6),
    val disabledInk: Color = Color(0xFF8A8A8A),
    val paid: Color = Color(0xFF0B7A3B),
    val stale: Color = Color(0xFF9A5B00),
    val onFlood: Color = Color(0xFFFFFFFF),
    val onFloodMuted: Color = Color(0xB3FFFFFF),
)

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
)

// Every amount uses tabular figures so columns of money line up.
private const val TABULAR = "tnum"

@Immutable
data class CuadreType(
    val amountHero: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-0.03).em, fontFeatureSettings = TABULAR),
    val amountTotal: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-0.03).em, fontFeatureSettings = TABULAR),
    val code: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = 0.25.em, fontFeatureSettings = TABULAR),
    val title: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em),
    val section: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    val rowTitle: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    val rowAmount: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp, fontFeatureSettings = TABULAR),
    val body: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    val secondary: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, fontFeatureSettings = TABULAR),
    val wordmark: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em),
    val button: TextStyle = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
)

private val LocalColors = staticCompositionLocalOf { CuadreColors() }
private val LocalType = staticCompositionLocalOf { CuadreType() }

object Cuadre {
    val colors: CuadreColors @Composable get() = LocalColors.current
    val type: CuadreType @Composable get() = LocalType.current
}

@Composable
fun CuadreTheme(content: @Composable () -> Unit) {
    val c = CuadreColors()
    // Material components we still use (Switch, ripple) get ink, not Material's default purple.
    val scheme = lightColorScheme(
        primary = c.ink, onPrimary = c.paper,
        secondary = c.ink, onSecondary = c.paper,
        background = c.paper, onBackground = c.ink,
        surface = c.paper, onSurface = c.ink, onSurfaceVariant = c.inkMuted,
        surfaceContainerHighest = c.hairline, outline = c.hairline, outlineVariant = c.hairline,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalColors provides c, LocalType provides CuadreType()) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
