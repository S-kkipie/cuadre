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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import pe.aido.cuadre.ui.components.EaseOutStrong
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.aido.cuadre.core.CashClose
import pe.aido.cuadre.core.DailyHistory
import pe.aido.cuadre.core.NuevoRus
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.data.DayCloseEntity
import pe.aido.cuadre.ui.components.Amount
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
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthName = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("es-PE"))

/** Month income vs. Nuevo RUS, export, then past days; tap a day to see its payments. */
@Composable
fun HistoryScreen(
    payments: List<ConfirmedPayment>,
    closes: List<DayCloseEntity>,
    onExportMonth: (YearMonth) -> Unit,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    val zone = ZoneId.systemDefault()
    var openDay by remember { mutableStateOf<LocalDate?>(null) }
    val days = DailyHistory.byDay(payments, zone)
    val closeByDay = closes.associateBy { LocalDate.parse(it.date) }
    BackHandler(enabled = openDay != null) { openDay = null }

    fun dateOf(p: ConfirmedPayment) = Instant.ofEpochMilli(p.event.postedAtMillis).atZone(zone).toLocalDate()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = SidePadding, end = SidePadding, top = 32.dp, bottom = 24.dp),
    ) {
        val day = openDay
        if (day == null) {
            val now = YearMonth.now()
            val month = days.filter { YearMonth.from(it.date) == now }.sumOf { it.total }
            val year = days.filter { it.date.year == now.year }.sumOf { it.total }
            val rus = NuevoRus.status(month, year)

            item {
                Text("Historial", style = t.title, color = c.ink)
                Spacer(Modifier.height(24.dp))
                Text("Ingresos de ${now.format(monthName)}", style = t.secondary, color = c.inkMuted)
                Spacer(Modifier.height(4.dp))
                Amount(month, t.amountTotal, c.ink)
                Spacer(Modifier.height(16.dp))
                // How much of the category's monthly ceiling is used. Mustard once it gets close.
                val ceiling = if (rus.category == 1) NuevoRus.CATEGORY_1_MONTHLY else NuevoRus.CATEGORY_2_MONTHLY
                val used by animateFloatAsState(
                    (month / ceiling).toFloat().coerceIn(0f, 1f),
                    tween(600, easing = EaseOutStrong),
                    label = "rus",
                )
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.hairline)) {
                    Box(
                        Modifier.fillMaxWidth(used).height(6.dp).clip(CircleShape)
                            .background(if (rus.nearLimit || rus.overMonthly) c.stale else c.ink),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(rusLine(rus), style = t.body, color = if (rus.nearLimit || rus.overMonthly) c.stale else c.inkMuted)
                Text(
                    "En el año: ${soles(year)} de ${soles(NuevoRus.ANNUAL_LIMIT)}",
                    style = t.body,
                    color = if (rus.nearAnnual || rus.overAnnual) c.stale else c.inkMuted,
                )
                Text(
                    "Referencial. Confirma tu régimen con tu contador.",
                    style = t.secondary,
                    color = c.inkMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextLink("Exportar ${now.format(monthName)} para Excel", { onExportMonth(now) })
                Spacer(Modifier.height(16.dp))
                Hairline()
            }
            if (days.isEmpty()) item {
                Text("Todavía no hay pagos guardados.", style = t.body, color = c.inkMuted, modifier = Modifier.padding(vertical = 16.dp))
            }
            items(days, key = { it.date.toString() }) { d ->
                val close = closeByDay[d.date]
                val count = if (d.count == 1) "1 pago" else "${d.count} pagos"
                ListRow(
                    d.date.longDay(),
                    secondary = close?.let { "$count · ${CashClose.differenceLabel(it.countedCash - it.expectedCash)}" } ?: count,
                    onClick = { openDay = d.date },
                ) { Text(plainAmount(d.total), style = t.rowAmount, color = c.ink) }
                Hairline()
            }
        } else {
            val dayPayments = payments.filter { dateOf(it) == day }
            val close = closeByDay[day]
            item {
                TextLink("Historial", { openDay = null }, muted = true)
                Text(day.longDay(), style = t.title, color = c.ink)
                Spacer(Modifier.height(8.dp))
                Amount(dayPayments.sumOf { it.event.amount }, t.amountTotal, c.ink)
                if (close != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Caja: contaste ${soles(close.countedCash)} · ${CashClose.differenceLabel(close.countedCash - close.expectedCash)}",
                        style = t.body,
                        color = c.inkMuted,
                    )
                }
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

private fun rusLine(s: NuevoRus.Status): String = when {
    s.overMonthly -> "Pasaste el límite del Nuevo RUS (${soles(NuevoRus.CATEGORY_2_MONTHLY)} al mes)"
    s.category == 1 -> "Nuevo RUS categoría 1: hasta ${soles(NuevoRus.CATEGORY_1_MONTHLY)} al mes" +
        if (s.nearLimit) " · te acercas al límite" else ""
    else -> "Nuevo RUS categoría 2: hasta ${soles(NuevoRus.CATEGORY_2_MONTHLY)} al mes" +
        if (s.nearLimit) " · te acercas al límite" else ""
}
