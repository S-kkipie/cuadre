package pe.aido.cuadre

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.aido.cuadre.alerts.PaymentAlerts
import pe.aido.cuadre.alerts.PaymentAlerts.Companion.soles
import pe.aido.cuadre.core.ConfirmationPolicy
import pe.aido.cuadre.core.VerificationEngine
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.debug.DebugPayments
import pe.aido.cuadre.setup.BatteryCheck
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val listenerEnabled = mutableStateOf(false)
    private val notificationsEnabled = mutableStateOf(true)
    private val tillMode = mutableStateOf(false)
    private val dayStart = mutableLongStateOf(startOfToday())
    private val batteryStatuses = mutableStateOf<List<BatteryCheck.AppStatus>>(emptyList())

    private val prefs by lazy { getSharedPreferences("cuadre", Context.MODE_PRIVATE) }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tillMode.value = prefs.getBoolean(KEY_TILL_MODE, false)
        applyTillMode()
        setContent {
            MaterialTheme {
                Today(
                    listenerEnabled = listenerEnabled.value,
                    notificationsEnabled = notificationsEnabled.value,
                    tillMode = tillMode.value,
                    batteryStatuses = batteryStatuses.value,
                    dayStart = dayStart.longValue,
                    onTillModeChange = ::setTillMode,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }
        }
    }

    // While visible, new payments go to the full-screen confirmation instead of a notification.
    override fun onStart() {
        super.onStart()
        cuadre.alerts.uiVisible = true
    }

    override fun onStop() {
        cuadre.alerts.uiVisible = false
        super.onStop()
    }

    // Re-check on every resume: the user grants access in system Settings and comes back,
    // and the app may be reopened on a new business day.
    override fun onResume() {
        super.onResume()
        refreshPermissions()
        dayStart.longValue = startOfToday()
    }

    private fun refreshPermissions() {
        listenerEnabled.value = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        notificationsEnabled.value = NotificationManagerCompat.from(this).areNotificationsEnabled()
        batteryStatuses.value = BatteryCheck.statuses(this)
    }

    private fun setTillMode(on: Boolean) {
        tillMode.value = on
        prefs.edit().putBoolean(KEY_TILL_MODE, on).apply()
        applyTillMode()
    }

    /** Till mode: screen stays on and the app shows over the lock screen, like a POS. */
    private fun applyTillMode() {
        val on = tillMode.value
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(on)
    }

    private fun startOfToday(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private companion object {
        const val KEY_TILL_MODE = "till_mode"
    }
}

private val esPE = Locale.forLanguageTag("es-PE")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", esPE)
private val timeSecondsFormat = DateTimeFormatter.ofPattern("HH:mm:ss", esPE)
private val dayFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", esPE)

private fun Long.localTime(format: DateTimeFormatter) =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(format)

@Composable
private fun Today(
    listenerEnabled: Boolean,
    notificationsEnabled: Boolean,
    tillMode: Boolean,
    batteryStatuses: List<BatteryCheck.AppStatus>,
    dayStart: Long,
    onTillModeChange: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
    val payments by remember(dayStart) { context.repository.observeBetween(dayStart, dayEnd) }
        .collectAsState(initial = emptyList())
    val pending by context.cuadre.alerts.pending.collectAsState()

    Box(Modifier.fillMaxSize()) {
        Scaffold { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text("Cuadre", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Hoy, " + dayStart.localTime(dayFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!listenerEnabled) item { AccessBanner() }
                if (!notificationsEnabled) item { NotificationsBanner(onRequestNotifications) }
                if (batteryStatuses.any { !it.unrestricted }) item { BatteryBanner(batteryStatuses) }
                item { TotalCard(payments) }
                item { TillModeRow(tillMode, onTillModeChange) }
                if (BuildConfig.DEBUG) item {
                    OutlinedButton(onClick = { scope.launch { DebugPayments.simulate(context) } }) {
                        Text("Simular yapeo (debug)")
                    }
                }
                item {
                    Text("Pagos de hoy (${payments.size})", style = MaterialTheme.typography.titleMedium)
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

        // Full-screen confirmation sits on top of everything until the cashier taps "Listo".
        pending.firstOrNull()?.let { head ->
            ConfirmationOverlay(
                payment = head,
                queued = pending.size - 1,
                onDismiss = { context.cuadre.alerts.dismiss(head.id) },
            )
        }
    }
}

private val PaidGreen = Color(0xFF0B7A3B)
private val StaleAmber = Color(0xFF9A5B00)

@Composable
private fun ConfirmationOverlay(payment: ConfirmedPayment, queued: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val e = payment.event
    // Ticks every second so a payment visibly "ages" while on screen.
    val now by produceState(System.currentTimeMillis(), payment.id) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val fresh = ConfirmationPolicy.isFresh(e.postedAtMillis, now)

    // Sound + vibration once per payment, and only if it is actually fresh.
    LaunchedEffect(payment.id) {
        if (ConfirmationPolicy.isFresh(e.postedAtMillis, System.currentTimeMillis())) playConfirmation(context)
    }
    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (fresh) PaidGreen else StaleAmber)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (fresh) "✅ PAGO RECIBIDO" else "⚠️ PAGO ANTERIOR",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        Text(soles(e.amount), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            e.counterparty ?: "Pagador no visible",
            color = Color.White,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${e.wallet.displayName} · ${e.postedAtMillis.localTime(timeSecondsFormat)} · " +
                ConfirmationPolicy.ageLabel(e.postedAtMillis, now),
            color = Color.White,
            fontSize = 18.sp,
        )
        e.securityCode?.let {
            Spacer(Modifier.height(8.dp))
            Text("Código de seguridad: $it", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        if (!fresh) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Este pago no es de ahora. No lo uses para confirmar al cliente que tienes enfrente.",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            Text(if (queued > 0) "Listo (+$queued más)" else "Listo", fontSize = 20.sp)
        }
    }
}

private fun playConfirmation(context: Context) {
    MediaPlayer.create(context, R.raw.cuadre_pago)?.apply {
        setOnCompletionListener { it.release() }
        start()
    }
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    vibrator.vibrate(VibrationEffect.createWaveform(PaymentAlerts.VIBRATION_PATTERN, -1))
}

@Composable
private fun TillModeRow(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Modo caja", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pantalla siempre encendida. Cada pago real se muestra en grande con sonido.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = on, onCheckedChange = onChange)
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
private fun BatteryBanner(statuses: List<BatteryCheck.AppStatus>) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ahorro de batería activo", style = MaterialTheme.typography.titleMedium)
            Text(
                "El celular puede congelar estas apps y los avisos de pago llegan tarde o no llegan. " +
                    "Toca \"Arreglar\", entra a Batería y elige \"Sin restricciones\".",
                style = MaterialTheme.typography.bodyMedium,
            )
            statuses.forEach { app ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        (if (app.unrestricted) "✅ " else "⚠️ ") + app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (!app.unrestricted) {
                        Button(onClick = { BatteryCheck.openAppSettings(context, app.packageName) }) { Text("Arreglar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsBanner(onRequest: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Permite los avisos de Cuadre para escuchar cada pago aunque la app esté cerrada.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequest) { Text("Permitir avisos") }
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
                    "${e.wallet.displayName} · " + e.postedAtMillis.localTime(timeFormat) +
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
