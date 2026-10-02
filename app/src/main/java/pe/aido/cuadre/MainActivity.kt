package pe.aido.cuadre

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.launch
import pe.aido.cuadre.core.VerificationEngine
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.debug.DebugPayments
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val listenerEnabled = mutableStateOf(false)
    private val dayStart = mutableLongStateOf(startOfToday())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Today(listenerEnabled.value, dayStart.longValue) } }
    }

    // Re-check on every resume: the user grants access in system Settings and comes back,
    // and the app may be reopened on a new business day.
    override fun onResume() {
        super.onResume()
        listenerEnabled.value = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        dayStart.longValue = startOfToday()
    }

    private fun startOfToday(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private val esPE = Locale.forLanguageTag("es-PE")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", esPE)
private val dayFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", esPE)

private fun soles(amount: Double) = String.format(Locale.US, "S/ %,.2f", amount)

@Composable
private fun Today(listenerEnabled: Boolean, dayStart: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
    val payments by remember(dayStart) { context.repository.observeBetween(dayStart, dayEnd) }
        .collectAsState(initial = emptyList())

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Cuadre", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Hoy, " + Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).format(dayFormat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!listenerEnabled) item { AccessBanner() }
            item { TotalCard(payments) }
            if (BuildConfig.DEBUG) item {
                OutlinedButton(onClick = { scope.launch { DebugPayments.simulate(context) } }) {
                    Text("Simular yapeo (debug)")
                }
            }
            item {
                Text(
                    "Pagos de hoy (${payments.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (payments.isEmpty()) item {
                Text(
                    "Aún no hay pagos. Aparecen aquí apenas llega la notificación de Yape o Plin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(payments, key = { it.id }) { PaymentRow(it) }
        }
    }
}

@Composable
private fun AccessBanner() {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Para confirmar pagos reales de Yape/Plin, activa el acceso a notificaciones.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = {
                // Prominent disclosure must be shown BEFORE this (Play policy). TODO: disclosure screen.
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) { Text("Activar acceso a notificaciones") }
        }
    }
}

@Composable
private fun TotalCard(payments: List<ConfirmedPayment>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Recibido hoy", style = MaterialTheme.typography.labelLarge)
            Text(
                soles(VerificationEngine.total(payments)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            VerificationEngine.dailyClose(payments).forEach { (wallet, sum) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(wallet.displayName, style = MaterialTheme.typography.bodyMedium)
                    Text(soles(sum), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: ConfirmedPayment) {
    val e = payment.event
    Column {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(e.counterparty ?: "Pagador no visible", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${e.wallet.displayName} · " +
                        Instant.ofEpochMilli(e.postedAtMillis).atZone(ZoneId.systemDefault()).format(timeFormat) +
                        (e.securityCode?.let { " · cód. $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(soles(e.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider()
    }
}
