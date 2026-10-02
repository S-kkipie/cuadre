package pe.aido.cuadre.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.aido.cuadre.core.DailyHistory
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.ui.components.Hairline
import pe.aido.cuadre.ui.components.ListRow
import pe.aido.cuadre.ui.components.PaymentRow
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.longDay
import pe.aido.cuadre.ui.plainAmount
import pe.aido.cuadre.ui.soles
import pe.aido.cuadre.ui.theme.Cuadre
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Past days with their totals; tap a day to see its payments. */
@Composable
fun HistoryScreen(payments: List<ConfirmedPayment>) {
    val c = Cuadre.colors
    val t = Cuadre.type
    val zone = ZoneId.systemDefault()
    var openDay by remember { mutableStateOf<LocalDate?>(null) }
    val days = DailyHistory.byDay(payments, zone)
    BackHandler(enabled = openDay != null) { openDay = null }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = SidePadding, end = SidePadding, top = 32.dp, bottom = 24.dp),
    ) {
        val day = openDay
        if (day == null) {
            item {
                Text("Historial", style = t.title, color = c.ink)
                Spacer(Modifier.height(24.dp))
                Hairline()
            }
            if (days.isEmpty()) item {
                Text("Todavía no hay pagos guardados.", style = t.body, color = c.inkMuted, modifier = Modifier.padding(vertical = 16.dp))
            }
            items(days, key = { it.date.toString() }) { d ->
                ListRow(
                    d.date.longDay(),
                    secondary = if (d.count == 1) "1 pago" else "${d.count} pagos",
                    onClick = { openDay = d.date },
                ) { Text(plainAmount(d.total), style = t.rowAmount, color = c.ink) }
                Hairline()
            }
        } else {
            val dayPayments = payments.filter {
                Instant.ofEpochMilli(it.event.postedAtMillis).atZone(zone).toLocalDate() == day
            }
            item {
                TextLink("Historial", { openDay = null }, muted = true)
                Text(day.longDay(), style = t.title, color = c.ink)
                Spacer(Modifier.height(8.dp))
                Text(soles(dayPayments.sumOf { it.event.amount }), style = t.amountTotal, color = c.ink)
                Spacer(Modifier.height(24.dp))
                Hairline()
            }
            items(dayPayments, key = { it.id }) { p ->
                PaymentRow(p)
                Hairline()
            }
        }
    }
}
