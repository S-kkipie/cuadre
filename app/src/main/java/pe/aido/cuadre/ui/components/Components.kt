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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.core.displayName
import pe.aido.cuadre.core.shortName
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

/** Full-width 56dp primary action. Brand fill on paper; white with ink text on a color flood. */
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
        else -> c.primary
    }
    val label = when {
        !enabled -> c.disabledInk
        onFlood -> c.ink
        else -> c.onPrimary
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

/** Compact secondary action: white with a 1px brand outline. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Cuadre.colors
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(ButtonShape)
            .border(1.dp, c.primary, ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Cuadre.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.primary) }
}

/** Brand-colored text link with a 48dp touch target; `muted` = quiet grey for low-priority links. */
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
            color = if (muted) c.inkMuted else c.primary,
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
    val wallet = e.wallet.shortName
    val detail = listOfNotNull(wallet, e.postedAtMillis.hhmm(), e.securityCode?.let { "cód. $it" })
        .joinToString(" · ")
    val isCash = e.wallet == pe.aido.cuadre.domain.Wallet.EFECTIVO
    val unknownPayer = e.counterparty == null && !isCash
    Row(modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    isCash -> "Venta en efectivo"
                    e.counterparty != null -> displayName(e.counterparty)
                    else -> "Pagador no visible"
                },
                style = Cuadre.type.rowTitle.copy(fontStyle = if (unknownPayer) FontStyle.Italic else FontStyle.Normal),
                color = if (unknownPayer) c.inkMuted else c.ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(detail, style = Cuadre.type.secondary, color = c.inkMuted)
        }
        Spacer(Modifier.width(16.dp))
        Text(plainAmount(e.amount), style = Cuadre.type.rowAmount, color = c.ink)
    }
}

/** Big money input: "S/" prefix, decimal keyboard, hairline underline. */
@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    val c = Cuadre.colors
    Column(modifier.fillMaxWidth()) {
        Text(label, style = Cuadre.type.secondary, color = c.inkMuted)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("S/ ", style = Cuadre.type.amountTotal.copy(fontSize = 36.sp), color = c.inkMuted)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { v -> onValueChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(10)) },
                textStyle = Cuadre.type.amountTotal.copy(fontSize = 36.sp, color = c.ink),
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.primary),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                ),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) Text("0.00", style = Cuadre.type.amountTotal.copy(fontSize = 36.sp), color = c.hairline)
                        inner()
                    }
                },
                modifier = Modifier.weight(1f).then(
                    if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Hairline()
    }
}

@Composable
fun CuadreSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val c = Cuadre.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onPrimary, checkedTrackColor = c.primary, checkedBorderColor = c.primary,
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

/** Text-only bottom navigation: active = brand + semibold with a short underline; inactive = muted. */
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            tab.label,
                            style = Cuadre.type.body.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (active) c.primary else c.inkMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.width(20.dp).height(2.dp).clip(CircleShape)
                                .background(if (active) c.primary else Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}
