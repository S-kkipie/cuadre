package pe.aido.cuadre.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.aido.cuadre.core.VerificationEngine
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.domain.Wallet
import pe.aido.cuadre.ui.components.CuadreSwitch
import pe.aido.cuadre.ui.components.Hairline
import pe.aido.cuadre.ui.components.ListRow
import pe.aido.cuadre.ui.components.PaymentRow
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.StatusDot
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.shortDay
import pe.aido.cuadre.ui.soles
import pe.aido.cuadre.ui.theme.Cuadre

/** "Plin (BBVA)" → "Plin · BBVA". */
fun Wallet.label(): String = displayName.replace(" (", " · ").removeSuffix(")")

@Composable
fun TodayScreen(
    payments: List<ConfirmedPayment>,
    dayStart: Long,
    listening: Boolean,
    setupIssues: Int,
    tillMode: Boolean,
    onTillModeChange: (Boolean) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    debugAction: (@Composable () -> Unit)? = null,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = SidePadding, end = SidePadding, top = 24.dp, bottom = 24.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Cuadre", style = t.wordmark, color = c.ink)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(if (listening) c.paid else c.stale)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (listening) "Escuchando pagos" else "No está escuchando",
                            style = t.secondary,
                            color = if (listening) c.inkMuted else c.stale,
                        )
                    }
                }
                Text(dayStart.shortDay(), style = t.secondary, color = c.inkMuted)
            }
        }

        // One quiet line instead of banners: anything that silently breaks capture.
        if (setupIssues > 0) item {
            Spacer(Modifier.height(16.dp))
            Text(
                if (setupIssues == 1) "Falta 1 ajuste para recibir todos los pagos" else "Faltan $setupIssues ajustes para recibir todos los pagos",
                style = t.body,
                color = c.stale,
                modifier = Modifier.clickable(onClick = onOpenSettings).padding(vertical = 12.dp),
            )
        }

        item {
            Spacer(Modifier.height(40.dp))
            Text("Recibido hoy", style = t.secondary, color = c.inkMuted)
            Spacer(Modifier.height(4.dp))
            Text(soles(VerificationEngine.total(payments)), style = t.amountTotal, color = c.ink)
            Spacer(Modifier.height(4.dp))
            Text(
                if (payments.size == 1) "1 pago" else "${payments.size} pagos",
                style = t.secondary,
                color = c.inkMuted,
            )
            Spacer(Modifier.height(24.dp))
        }

        val byWallet = VerificationEngine.dailyClose(payments)
        if (byWallet.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                byWallet.entries.sortedByDescending { it.value }.forEach { (wallet, sum) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(wallet.label(), style = t.body, color = c.ink, modifier = Modifier.weight(1f))
                        Text(soles(sum), style = t.body.copy(fontFeatureSettings = "tnum"), color = c.ink)
                    }
                }
            }
        }

        item {
            Hairline()
            ListRow("Modo caja", secondary = "Pantalla encendida y aviso en grande con sonido") {
                CuadreSwitch(tillMode, onTillModeChange)
            }
            Hairline()
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Pagos", style = t.section, color = c.ink, modifier = Modifier.weight(1f))
                TextLink("Historial", onOpenHistory, muted = true)
            }
        }

        if (payments.isEmpty()) item {
            Text(
                "Aún no hay pagos hoy. Aparecen aquí apenas llega la notificación de Yape o Plin.",
                style = t.body,
                color = c.inkMuted,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }

        items(payments, key = { it.id }) { p ->
            PaymentRow(p)
            Hairline()
        }

        if (debugAction != null) item {
            Spacer(Modifier.height(24.dp))
            debugAction()
        }
    }
}
