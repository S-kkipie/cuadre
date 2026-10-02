package pe.aido.cuadre

import android.Manifest
import android.content.Context
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
import kotlinx.coroutines.launch
import pe.aido.cuadre.core.ConfirmationPolicy
import pe.aido.cuadre.debug.DebugPayments
import pe.aido.cuadre.setup.BatteryCheck
import pe.aido.cuadre.ui.components.BottomNav
import pe.aido.cuadre.ui.components.Tab
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.screens.ConfirmationScreen
import pe.aido.cuadre.ui.screens.HistoryScreen
import pe.aido.cuadre.ui.screens.SetupScreen
import pe.aido.cuadre.ui.screens.SetupState
import pe.aido.cuadre.ui.screens.TodayScreen
import pe.aido.cuadre.ui.theme.Cuadre
import pe.aido.cuadre.ui.theme.CuadreTheme
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    private val setup = mutableStateOf(SetupState(false, true, emptyList()))
    private val tillMode = mutableStateOf(false)
    private val onboarded = mutableStateOf(false)
    private val dayStart = mutableLongStateOf(startOfToday())

    private val prefs by lazy { getSharedPreferences("cuadre", Context.MODE_PRIVATE) }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshSetup() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        super.onCreate(savedInstanceState)
        tillMode.value = prefs.getBoolean(KEY_TILL_MODE, false)
        onboarded.value = prefs.getBoolean(KEY_ONBOARDED, false)
        applyTillMode()
        setContent {
            CuadreTheme {
                App(
                    setup = setup.value,
                    onboarded = onboarded.value,
                    tillMode = tillMode.value,
                    dayStart = dayStart.longValue,
                    onTillModeChange = ::setTillMode,
                    onOpenListenerSettings = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onRequestNotifications = ::askNotifications,
                    onFixBattery = { BatteryCheck.openAppSettings(this, it) },
                    onStart = {
                        onboarded.value = true
                        prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
                    },
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
        const val KEY_ONBOARDED = "onboarded"
    }
}

@Composable
private fun App(
    setup: SetupState,
    onboarded: Boolean,
    tillMode: Boolean,
    dayStart: Long,
    onTillModeChange: (Boolean) -> Unit,
    onOpenListenerSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
    onFixBattery: (String) -> Unit,
    onStart: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(Tab.Hoy) }
    var privacyOpen by remember { mutableStateOf(false) }
    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
    val today by remember(dayStart) { context.repository.observeBetween(dayStart, dayEnd) }
        .collectAsState(initial = emptyList())
    val all by remember { context.repository.observeAll() }.collectAsState(initial = emptyList())
    val pending by context.cuadre.alerts.pending.collectAsState()

    Box(Modifier.fillMaxSize().background(Cuadre.colors.paper)) {
        val setupScreen = @Composable { firstRun: Boolean ->
            SetupScreen(
                state = setup,
                firstRun = firstRun,
                onOpenListenerSettings = onOpenListenerSettings,
                onRequestNotifications = onRequestNotifications,
                onFixBattery = onFixBattery,
                onOpenPrivacy = { privacyOpen = true },
                onStart = onStart,
            )
        }

        if (!onboarded) {
            Box(Modifier.statusBarsPadding()) { setupScreen(true) }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.Hoy -> TodayScreen(
                            payments = today,
                            dayStart = dayStart,
                            listening = setup.listenerEnabled,
                            setupIssues = setup.issues,
                            tillMode = tillMode,
                            onTillModeChange = onTillModeChange,
                            onOpenHistory = { tab = Tab.Historial },
                            onOpenSettings = { tab = Tab.Ajustes },
                            debugAction = if (BuildConfig.DEBUG) {
                                { TextLink("Simular yapeo (debug)", { scope.launch { DebugPayments.simulate(context) } }, muted = true) }
                            } else null,
                        )
                        Tab.Historial -> HistoryScreen(all)
                        Tab.Ajustes -> setupScreen(false)
                    }
                }
                BottomNav(tab, { tab = it })
            }
        }

        // Full-screen confirmation sits on top of everything until the cashier taps "Listo".
        ConfirmationPolicy.next(pending)?.let { head ->
            ConfirmationScreen(head, queued = pending.size - 1, onDismiss = { context.cuadre.alerts.dismiss(head.id) })
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
