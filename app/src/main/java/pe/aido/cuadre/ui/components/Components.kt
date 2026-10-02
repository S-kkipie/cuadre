package pe.aido.cuadre.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.ui.hhmm
import pe.aido.cuadre.ui.plainAmount
import pe.aido.cuadre.ui.theme.Cuadre

val SidePadding: Dp = 24.dp
private val ButtonShape = RoundedCornerShape(12.dp)

/** 1px divider. The only separator in the app — no cards around rows. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Cuadre.colors.hairline))
}

/** Full-width 56dp primary action. Ink on paper; white with ink text on a color flood. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onFlood: Boolean = false,
) {
    val c = Cuadre.colors
    val fill = when {
        !enabled -> c.disabledFill
        onFlood -> c.paper
        else -> c.ink
    }
    val label = when {
        !enabled -> c.disabledInk
        onFlood -> c.ink
        else -> c.paper
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(ButtonShape)
            .background(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Cuadre.type.button, color = label) }
}

/** Compact secondary action: white with a 1px ink outline. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Cuadre.colors
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(ButtonShape)
            .border(1.dp, c.ink, ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Cuadre.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.ink) }
}

/** Underlined ink text with a 48dp touch target. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, muted: Boolean = false) {
    val c = Cuadre.colors
    Box(
        modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            style = Cuadre.type.body.copy(
                fontWeight = if (muted) FontWeight.Normal else FontWeight.SemiBold,
                textDecoration = if (muted) null else TextDecoration.Underline,
            ),
            color = if (muted) c.inkMuted else c.ink,
        )
    }
}

/** Plain row: title (+ optional secondary line) on the left, a trailing slot on the right. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    secondaryColor: Color = Cuadre.colors.inkMuted,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Cuadre.type.rowTitle, color = Cuadre.colors.ink)
            if (secondary != null) {
                Spacer(Modifier.height(4.dp))
                Text(secondary, style = Cuadre.type.secondary, color = secondaryColor)
            }
        }
        trailing()
    }
}

/** One payment: payer and amount, then "Yape · 14:32 · cód. 418". */
@Composable
fun PaymentRow(payment: ConfirmedPayment, modifier: Modifier = Modifier) {
    val e = payment.event
    val c = Cuadre.colors
    val wallet = e.wallet.displayName.substringBefore(" (")
    val detail = listOfNotNull(wallet, e.postedAtMillis.hhmm(), e.securityCode?.let { "cód. $it" })
        .joinToString(" · ")
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp).then(modifier), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                e.counterparty?.let(::displayName) ?: "Pagador no visible",
                style = Cuadre.type.rowTitle.copy(fontStyle = if (e.counterparty == null) FontStyle.Italic else FontStyle.Normal),
                color = if (e.counterparty == null) c.inkMuted else c.ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(detail, style = Cuadre.type.secondary, color = c.inkMuted)
        }
        Spacer(Modifier.width(16.dp))
        Text(plainAmount(e.amount), style = Cuadre.type.rowAmount, color = c.ink)
    }
}

/** Wallets send names in caps ("ROSA HUAMAN"); show them in sentence case like the design. */
fun displayName(raw: String): String =
    raw.trim().lowercase().split(Regex("\\s+")).joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

@Composable
fun CuadreSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = Cuadre.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.paper, checkedTrackColor = c.ink, checkedBorderColor = c.ink,
            uncheckedThumbColor = c.inkMuted, uncheckedTrackColor = c.paper, uncheckedBorderColor = c.inkMuted,
        ),
    )
}

/** Tiny live dot used once: "● Escuchando pagos". */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(color))
}

enum class Tab(val label: String) { Hoy("Hoy"), Historial("Historial"), Ajustes("Ajustes") }

/** Text-only bottom navigation: active = ink + semibold, inactive = muted. No pill, no icons. */
@Composable
fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Cuadre.colors
    Column(modifier.fillMaxWidth().background(c.paper)) {
        Hairline()
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp)) {
            Tab.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    Modifier.weight(1f).fillMaxWidth().height(64.dp)
                        .clickable(role = Role.Tab) { onSelect(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.label,
                        style = Cuadre.type.body.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (active) c.ink else c.inkMuted,
                    )
                }
            }
        }
    }
}
