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
 * Cuadre — ink on paper with one brand color. Source of truth: .stitch/DESIGN.md.
 * [CuadreColors.primary] (terracotta) is the brand and every action. [CuadreColors.paid] means
 * real money arrived, [CuadreColors.stale] means an old payment or a pending setup step — those
 * two are signals and never used for decoration. Everything else is ink.
 */
@Immutable
data class CuadreColors(
    val paper: Color = Color(0xFFFBFAF7),        // warm paper, not screen white
    val surface: Color = Color(0xFFFFFFFF),      // dialogs and the code cells sit on this
    val ink: Color = Color(0xFF1A1814),
    val primary: Color = Color(0xFFB4532A),      // terracotta; white on it = 5.0:1
    val onPrimary: Color = Color(0xFFFFFFFF),
    val inkMuted: Color = Color(0xFF6B675F),
    val hairline: Color = Color(0xFFE9E5DD),
    val fieldLine: Color = Color(0xFF948F85),    // input outline, 3:1 on surface (WCAG 1.4.11)
    val disabledFill: Color = Color(0xFFEAE6DE),
    val disabledInk: Color = Color(0xFF8C877E),
    val paid: Color = Color(0xFF0B7A3B),
    val stale: Color = Color(0xFF8C6A00),        // dark mustard, kept apart from terracotta
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
    // Material components we still use (Switch, ripple, dialogs) get the brand, not Material's purple.
    val scheme = lightColorScheme(
        primary = c.primary, onPrimary = c.onPrimary,
        secondary = c.ink, onSecondary = c.paper,
        background = c.paper, onBackground = c.ink,
        surface = c.surface, onSurface = c.ink, onSurfaceVariant = c.inkMuted,
        surfaceContainerHighest = c.hairline, outline = c.hairline, outlineVariant = c.hairline,
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalColors provides c, LocalType provides CuadreType()) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
