package pe.aido.cuadre

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pe.aido.cuadre.core.CashClose
import pe.aido.cuadre.core.ConfirmationPolicy
import pe.aido.cuadre.core.IncomeCsv
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.data.DayCloseEntity
import pe.aido.cuadre.debug.DebugPayments
import pe.aido.cuadre.setup.BatteryCheck
import pe.aido.cuadre.ui.components.BottomNav
import pe.aido.cuadre.ui.components.Tab
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.screens.CashSaleDialog
import pe.aido.cuadre.ui.screens.CloseDayScreen
import pe.aido.cuadre.ui.screens.ConfirmationScreen
import pe.aido.cuadre.ui.screens.DeleteCashDialog
import pe.aido.cuadre.ui.screens.HistoryScreen
import pe.aido.cuadre.ui.screens.SetupScreen
import pe.aido.cuadre.ui.screens.SetupState
import pe.aido.cuadre.ui.screens.TodayScreen
import pe.aido.cuadre.ui.shortDay
import pe.aido.cuadre.ui.theme.Cuadre
import pe.aido.cuadre.ui.theme.CuadreTheme
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private val setup = mutableStateOf(SetupState(false, true, emptyList()))
    private val dayStart = mutableLongStateOf(startOfToday())

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshSetup() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        super.onCreate(savedInstanceState)
        val prefs = cuadre.prefs
        lifecycleScope.launch { prefs.tillMode.collect(::applyTillMode) }
        setContent {
            CuadreTheme {
                App(
                    setup = setup.value,
                    dayStart = dayStart.longValue,
                    actions = Actions(
                        openListenerSettings = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        requestNotifications = ::askNotifications,
                        fixBattery = { BatteryCheck.openAppSettings(this, it) },
                        share = ::shareText,
                        exportMonth = ::exportMonth,
                    ),
                )
            }
        }
    }

    // While visible, new payments go to the full-screen confirmation instead of a notification.
    override fun onStart() {
        super.onStart()
        cuadre.alerts.onScreenStarted()
    }

    override fun onStop() {
        cuadre.alerts.onScreenStopped()
        super.onStop()
    }

    // Re-check on every resume: permissions are granted in system Settings, and the app may be
    // reopened on a new business day.
    override fun onResume() {
        super.onResume()
        refreshSetup()
        dayStart.longValue = startOfToday()
    }

    private fun refreshSetup() {
        setup.value = SetupState(
            listenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName),
            notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled(),
            battery = BatteryCheck.statuses(this),
        )
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
            )
        }
    }

    private fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "Compartir cuadre"))
    }

    /** Writes the month's income as CSV to cache/exports and opens the share sheet. */
    private fun exportMonth(month: YearMonth) {
        lifecycleScope.launch {
            val zone = ZoneId.systemDefault()
            val from = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val csv = IncomeCsv.build(cuadre.repository.paymentsBetween(from, to), zone)
            val dir = File(cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "cuadre-ingresos-$month.csv")
            // BOM so Excel opens accents (Huamán) correctly.
            file.writeText("﻿" + csv)
            val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.files", file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/csv")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, "Ingresos $month · Cuadre")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(send, "Exportar ingresos"))
        }
    }

    /** Till mode: screen stays on and the app shows over the lock screen, like a POS. */
    private fun applyTillMode(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(on)
    }

    private fun startOfToday(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private class Actions(
    val openListenerSettings: () -> Unit,
    val requestNotifications: () -> Unit,
    val fixBattery: (String) -> Unit,
    val share: (String) -> Unit,
    val exportMonth: (YearMonth) -> Unit,
)

@Composable
private fun App(setup: SetupState, dayStart: Long, actions: Actions) {
    val context = LocalContext.current
    val app = context.cuadre
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(Tab.Hoy) }
    var privacyOpen by remember { mutableStateOf(false) }
    var cashOpen by remember { mutableStateOf(false) }
    var closingDay by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ConfirmedPayment?>(null) }

    val onboarded by app.prefs.onboarded.collectAsState()
    val tillMode by app.prefs.tillMode.collectAsState()
    val voice by app.prefs.voice.collectAsState()
    val listenerConnected by app.prefs.listenerConnected.collectAsState()
    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
    val today by remember(dayStart) { app.repository.observeBetween(dayStart, dayEnd) }.collectAsState(initial = emptyList())
    val all by remember { app.repository.observeAll() }.collectAsState(initial = emptyList())
    val closes by remember { app.database.dayCloseDao().observeAll() }.collectAsState(initial = emptyList())
    val pending by app.alerts.pending.collectAsState()

    val todayDate = Instant.ofEpochMilli(dayStart).atZone(ZoneId.systemDefault()).toLocalDate()
    val todayClose = closes.firstOrNull { it.date == todayDate.toString() }
    val lastOpening = closes.firstOrNull()?.openingCash ?: 0.0

    Box(Modifier.fillMaxSize().background(Cuadre.colors.paper)) {
        val setupScreen = @Composable { firstRun: Boolean ->
            SetupScreen(
                state = setup,
                firstRun = firstRun,
                onOpenListenerSettings = actions.openListenerSettings,
                onRequestNotifications = actions.requestNotifications,
                onFixBattery = actions.fixBattery,
                onOpenPrivacy = { privacyOpen = true },
                onStart = { app.prefs.setOnboarded(true) },
                voiceOn = voice,
                onVoiceChange = app.prefs::setVoice,
                onTestVoice = app.alerts::testVoice,
            )
        }

        when {
            !onboarded -> Box(Modifier.statusBarsPadding()) { setupScreen(true) }
            closingDay -> CloseDayScreen(
                dayLabel = dayStart.shortDay(),
                payments = today,
                lastOpeningCash = lastOpening,
                onSave = { r ->
                    scope.launch {
                        app.database.dayCloseDao().upsert(
                            DayCloseEntity(todayDate.toString(), r.openingCash, r.countedCash, r.expectedCash, r.salesTotal, System.currentTimeMillis()),
                        )
                        closingDay = false
                    }
                },
                onShare = actions.share,
                onBack = { closingDay = false },
            )
            else -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.Hoy -> TodayScreen(
                            payments = today,
                            dayStart = dayStart,
                            listening = setup.listenerEnabled && listenerConnected,
                            setupIssues = setup.issues,
                            tillMode = tillMode,
                            onTillModeChange = app.prefs::setTillMode,
                            onOpenHistory = { tab = Tab.Historial },
                            onOpenSettings = { tab = Tab.Ajustes },
                            onAddCash = { cashOpen = true },
                            onCloseDay = { closingDay = true },
                            onCashTap = { deleting = it },
                            closeLabel = todayClose?.let {
                                "Contaste ${pe.aido.cuadre.ui.soles(it.countedCash)} · " +
                                    CashClose.differenceLabel(it.countedCash - it.expectedCash)
                            },
                            debugAction = if (BuildConfig.DEBUG) {
                                { TextLink("Simular yapeo (debug)", { scope.launch { DebugPayments.simulate(context) } }, muted = true) }
                            } else null,
                        )
                        Tab.Historial -> HistoryScreen(all, closes, actions.exportMonth)
                        Tab.Ajustes -> setupScreen(false)
                    }
                }
                BottomNav(tab, { tab = it })
            }
        }

        if (cashOpen) CashSaleDialog(
            onAdd = { amount ->
                scope.launch { app.repository.addCash(amount, System.currentTimeMillis()) }
                cashOpen = false
            },
            onDismiss = { cashOpen = false },
        )
        deleting?.let { p ->
            DeleteCashDialog(
                p,
                onDelete = { scope.launch { app.repository.deleteCash(p.id) }; deleting = null },
                onDismiss = { deleting = null },
            )
        }

        // Full-screen confirmation sits on top of everything until the cashier taps "Listo".
        ConfirmationPolicy.next(pending)?.let { head ->
            ConfirmationScreen(head, queued = pending.size - 1, onDismiss = { app.alerts.dismiss(head.id) })
        }

        if (privacyOpen) PrivacyDialog { privacyOpen = false }
    }
}

@Composable
private fun PrivacyDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Entendido", color = Cuadre.colors.primary) } },
        title = { Text("Privacidad", style = Cuadre.type.section, color = Cuadre.colors.ink) },
        text = {
            Text(
                "Cuadre lee únicamente las notificaciones de pagos recibidos de Yape y de los bancos con Plin, " +
                    "para mostrarte el monto, quién pagó y la hora. No lee otras notificaciones, no accede a tus " +
                    "cuentas, no mueve dinero y no envía tus datos a ningún servidor: todo se guarda solo en este " +
                    "celular. Puedes quitar el acceso cuando quieras desde los ajustes del teléfono.",
                style = Cuadre.type.body,
                color = Cuadre.colors.ink,
            )
        },
        containerColor = Cuadre.colors.paper,
    )
}
