package pe.aido.cuadre.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import pe.aido.cuadre.core.CashClose
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.core.parseSoles
import pe.aido.cuadre.core.shortName
import pe.aido.cuadre.ui.components.AmountInput
import pe.aido.cuadre.ui.components.BackLink
import pe.aido.cuadre.ui.components.FieldShape
import pe.aido.cuadre.ui.components.SecondaryButton
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import pe.aido.cuadre.ui.components.Hairline
import pe.aido.cuadre.ui.components.OutlineButton
import pe.aido.cuadre.ui.components.PrimaryButton
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.soles
import pe.aido.cuadre.ui.theme.Cuadre

/** "+ Efectivo": one field, one button. Cash is typed by hand; Yape/Plin never are. */
@Composable
fun CashSaleDialog(onAdd: (Double) -> Unit, onDismiss: () -> Unit) {
    val c = Cuadre.colors
    var text by remember { mutableStateOf("") }
    var tried by remember { mutableStateOf(false) }
    val amount = parseSoles(text)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.paper).padding(24.dp),
        ) {
            Text("Venta en efectivo", style = Cuadre.type.section, color = c.ink)
            Spacer(Modifier.height(4.dp))
            Text("Se suma al total de hoy y al cierre de caja.", style = Cuadre.type.secondary, color = c.inkMuted)
            Spacer(Modifier.height(20.dp))
            AmountInput(
                text,
                { text = it },
                "Monto",
                focusRequester = focus,
                error = if (tried && amount == null) "Escribe cuánto te pagaron" else null,
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Agregar", { tried = true; amount?.let(onAdd) })
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TextLink("Cancelar", onDismiss, muted = true) }
        }
    }
}

/** Confirm before removing a typed cash sale (Yape/Plin rows can't be removed at all). */
@Composable
fun DeleteCashDialog(payment: ConfirmedPayment, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val c = Cuadre.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.paper,
        title = { Text("¿Borrar esta venta?", style = Cuadre.type.section, color = c.ink) },
        text = { Text("Se quitarán ${soles(payment.event.amount)} en efectivo del total de hoy. Los pagos de Yape y Plin no se pueden borrar.", style = Cuadre.type.body, color = c.ink) },
        confirmButton = { TextButton(onClick = onDelete) { Text("Borrar", color = c.stale, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = c.inkMuted) } },
    )
}

/**
 * "Cerrar caja": count the drawer, see if it squares, save and share the day.
 * Expected cash = opening float + cash sales; Yape/Plin are already proven by the listener.
 */
@Composable
fun CloseDayScreen(
    dayLabel: String,
    payments: List<ConfirmedPayment>,
    lastOpeningCash: Double,
    onSave: (CashClose.Result) -> Unit,
    onShare: (String) -> Unit,
    onBack: () -> Unit,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    var opening by remember { mutableStateOf(if (lastOpeningCash > 0) plain(lastOpeningCash) else "") }
    var counted by remember { mutableStateOf("") }
    val openingValue = parseSoles(opening) ?: 0.0
    val countedValue = parseSoles(counted)
    val result = CashClose.compute(payments, openingValue, countedValue ?: 0.0)
    var tried by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(c.paper).statusBarsPadding().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = SidePadding)) {
            Spacer(Modifier.height(8.dp))
            BackLink("Hoy", onBack)
            Spacer(Modifier.height(16.dp))
            Text("Cerrar caja", style = t.title, color = c.ink)
            Spacer(Modifier.height(4.dp))
            Text(dayLabel, style = t.body, color = c.inkMuted)

            // What came in today, as one boxed receipt.
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.fillMaxWidth().clip(FieldShape).background(Color.White)
                    .border(1.dp, c.hairline, FieldShape).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                result.byWallet.entries.sortedByDescending { it.value }.forEach { (w, sum) ->
                    SummaryRow(w.shortName, soles(sum))
                }
                Hairline()
                SummaryRow("Total vendido", soles(result.salesTotal), strong = true)
            }

            Spacer(Modifier.height(32.dp))
            Text("Cuenta el efectivo", style = t.section, color = c.ink)
            Spacer(Modifier.height(4.dp))
            Text("Yape y Plin ya están confirmados. Solo falta la plata de la caja.", style = t.secondary, color = c.inkMuted)
            Spacer(Modifier.height(20.dp))
            AmountInput(opening, { opening = it }, "Con cuánto abriste la caja")
            Spacer(Modifier.height(20.dp))
            AmountInput(
                counted,
                { counted = it },
                "Cuánto hay ahora en la caja",
                error = if (tried && countedValue == null) "Cuenta la caja y escribe el monto" else null,
            )

            Spacer(Modifier.height(20.dp))
            SummaryRow("Deberías tener", soles(result.expectedCash))
            if (countedValue != null) {
                val exact = kotlin.math.abs(result.difference) < 0.005
                val tone = when {
                    exact -> c.paid
                    result.difference < 0 -> c.stale
                    else -> c.ink
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    CashClose.differenceLabel(result.difference),
                    style = t.section,
                    color = tone,
                    modifier = Modifier.fillMaxWidth().clip(FieldShape).background(tone.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.navigationBarsPadding().padding(horizontal = SidePadding, vertical = 16.dp)) {
            PrimaryButton("Guardar cierre", { tried = true; if (countedValue != null) onSave(result) })
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Compartir por WhatsApp", { onShare(CashClose.shareText(dayLabel, result)) })
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, strong: Boolean = false) {
    val c = Cuadre.colors
    val style = if (strong) Cuadre.type.rowAmount.copy(fontWeight = FontWeight.SemiBold) else Cuadre.type.body.copy(fontFeatureSettings = "tnum")
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = style, color = c.ink, modifier = Modifier.weight(1f))
        Text(value, style = style, color = c.ink)
    }
}

private fun plain(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.US, "%.2f", v)
