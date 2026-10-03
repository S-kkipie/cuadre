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
import pe.aido.cuadre.ui.components.OutlineButton
import pe.aido.cuadre.ui.components.PaymentRow
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.Amount
import pe.aido.cuadre.ui.components.EaseOutStrong
import pe.aido.cuadre.ui.components.LiveDot
import pe.aido.cuadre.ui.components.WalletSplit
import pe.aido.cuadre.core.shortName
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.components.FieldShape
import pe.aido.cuadre.ui.components.SecondaryButton
import pe.aido.cuadre.ui.components.pressable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
    onAddCash: () -> Unit,
    onCloseDay: () -> Unit,
    onCashTap: (ConfirmedPayment) -> Unit,
    closeLabel: String?,
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
                    Text("Cuadre", style = t.wordmark, color = c.primary)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveDot(if (listening) c.paid else c.stale, live = listening)
                        Spacer(Modifier.width(4.dp))
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
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(onClick = onOpenSettings)
                    .clip(FieldShape)
                    .background(c.stale.copy(alpha = 0.08f))
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (setupIssues == 1) "Falta 1 ajuste para recibir todos los pagos" else "Faltan $setupIssues ajustes para recibir todos los pagos",
                    style = t.body,
                    color = c.stale,
                    modifier = Modifier.weight(1f),
                )
                Text("Arreglar", style = t.body.copy(fontWeight = FontWeight.SemiBold), color = c.stale)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.stale)
            }
        }

        item {
            Spacer(Modifier.height(40.dp))
            Text("Recibido hoy", style = t.secondary, color = c.inkMuted)
            Spacer(Modifier.height(4.dp))
            // The total rolls up when a payment lands: old value exits up, new one rises in.
            AnimatedContent(
                targetState = VerificationEngine.total(payments),
                transitionSpec = {
                    (slideInVertically(tween(320, easing = EaseOutStrong)) { it / 2 } + fadeIn(tween(220)))
                        .togetherWith(slideOutVertically(tween(200, easing = EaseOutStrong)) { -it / 2 } + fadeOut(tween(150)))
                        .using(SizeTransform(clip = true))
                },
                label = "total",
            ) { total -> Amount(total, t.amountTotal, c.ink) }
            Spacer(Modifier.height(4.dp))
            Text(
                if (payments.size == 1) "1 pago" else "${payments.size} pagos",
                style = t.secondary,
                color = c.inkMuted,
            )
            Spacer(Modifier.height(24.dp))
        }

        val byWallet = VerificationEngine.dailyClose(payments).entries
            .groupBy({ it.key.shortName }, { it.value })
            .map { (label, sums) -> label to sums.sum() }
            .sortedByDescending { it.second }
        if (byWallet.isNotEmpty()) item {
            WalletSplit(byWallet, Modifier.padding(bottom = 28.dp))
        }

        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("+ Efectivo", onAddCash, Modifier.weight(1f))
                SecondaryButton(if (closeLabel != null) "Ver cierre" else "Cerrar caja", onCloseDay, Modifier.weight(1f))
            }
            if (closeLabel != null) {
                Text(closeLabel, style = t.secondary, color = c.inkMuted, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(Modifier.height(16.dp))
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
                TextLink("Ver historial", onOpenHistory)
            }
        }

        if (payments.isEmpty()) item {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth().clip(FieldShape).background(c.surface)
                    .border(1.dp, c.hairline, FieldShape).padding(20.dp),
            ) {
                Text("Aún no hay pagos hoy", style = t.rowTitle, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Apenas te llegue un Yape o Plin aparece aquí, con el nombre y el código.",
                    style = t.secondary,
                    color = c.inkMuted,
                )
            }
        }

        items(payments, key = { it.id }) { p ->
            val isCash = p.event.wallet == Wallet.EFECTIVO
            // New payments slide into place instead of popping in.
            Column(Modifier.animateItem(fadeInSpec = tween(260), placementSpec = tween(320, easing = EaseOutStrong))) {
                PaymentRow(p, if (isCash) Modifier.clickable { onCashTap(p) } else Modifier)
                Hairline()
            }
        }

        if (debugAction != null) item {
            Spacer(Modifier.height(24.dp))
            debugAction()
        }
    }
}
