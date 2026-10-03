package pe.aido.cuadre

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.aido.cuadre.sync.CuadreApi
import pe.aido.cuadre.core.CashClose
import pe.aido.cuadre.core.ConfirmationPolicy
import pe.aido.cuadre.core.IncomeCsv
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.data.DayCloseEntity
import pe.aido.cuadre.debug.DebugPayments
import pe.aido.cuadre.sync.PushRegisterWorker
import pe.aido.cuadre.sync.SyncWorker
import pe.aido.cuadre.sync.SyncCodec
import pe.aido.cuadre.account.AuthMessages
import pe.aido.cuadre.account.GoogleSignIn
import pe.aido.cuadre.ui.screens.LinkOutcome
import pe.aido.cuadre.ui.screens.LoginScreen
import pe.aido.cuadre.ui.screens.OwnerStoreScreen
import pe.aido.cuadre.ui.screens.WelcomeScreen
import pe.aido.cuadre.ui.screens.SettingsScreen
import pe.aido.cuadre.ui.screens.WorkerJoinScreen
import pe.aido.cuadre.data.UseMode
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
        // Catch up with the store's other phones every time the app comes up.
        if (cuadre.storeLink.link.value != null) SyncWorker.enqueue(this)
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
            restrictedSettings = installedOutsidePlay(),
        )
    }

    /** Android 13+ restricts notification access for apps that didn't come from a store. */
    private fun installedOutsidePlay(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        val installer = runCatching { packageManager.getInstallSourceInfo(packageName).installingPackageName }.getOrNull()
        return installer != "com.android.vending"
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
    val link by app.storeLink.link.collectAsState()
    val session by app.account.session.collectAsState()
    val mode by app.prefs.mode.collectAsState()
    val api = remember { CuadreApi() }

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
        // Back to the first screen; payments stay on the phone, only the link and session go.
        val leave = {
            val token = session?.token
            app.storeLink.clear()
            app.account.clear()
            app.prefs.setMode(null)
            if (token != null) scope.launch(Dispatchers.IO) { api.signOut(token) }
        }
        val setupScreen = @Composable {
            SetupScreen(
                state = setup,
                onOpenListenerSettings = actions.openListenerSettings,
                onRequestNotifications = actions.requestNotifications,
                onFixBattery = actions.fixBattery,
                onOpenPrivacy = { privacyOpen = true },
                onOpenAppInfo = { actions.fixBattery(app.packageName) },
                onStart = { app.prefs.setOnboarded(true) },
                linkedStore = link?.storeName,
            )
        }
        val settingsScreen = @Composable {
            SettingsScreen(
                state = setup,
                mode = mode ?: UseMode.LOCAL,
                storeName = link?.storeName,
                accountEmail = session?.email,
                voiceOn = voice,
                onVoiceChange = app.prefs::setVoice,
                onTestVoice = app.alerts::testVoice,
                onOpenListenerSettings = actions.openListenerSettings,
                onRequestNotifications = actions.requestNotifications,
                onFixBattery = actions.fixBattery,
                onOpenPrivacy = { privacyOpen = true },
                onSignOut = leave,
                onDeleteAccount = {
                    val token = session?.token
                    if (token == null) null
                    else when (val r = withContext(Dispatchers.IO) { api.deleteAccount(token) }) {
                        is CuadreApi.Result.Ok -> { leave(); null }
                        is CuadreApi.Result.Rejected ->
                            if (r.status == 401) "Tu sesión venció. Sal, vuelve a entrar e inténtalo otra vez."
                            else "No se pudo eliminar la cuenta. Inténtalo otra vez."
                        is CuadreApi.Result.Failed -> "Sin conexión. Revisa tu internet e inténtalo otra vez."
                    }
                },
                onLeaveStore = leave,
                onConnect = { app.prefs.setMode(null) },
            )
        }

        when {
            // An account is required once the build talks to a backend; without one there's nothing to sign in to.
            mode == null -> WelcomeScreen(onPick = app.prefs::setMode)
            mode == UseMode.WORKER && link == null -> WorkerJoinScreen(
                defaultName = android.os.Build.MODEL,
                available = api.configured,
                onJoin = { code, name -> pairPhone(app, code, name) },
                onBack = { app.prefs.setMode(null) },
            )
            mode == UseMode.OWNER && session == null -> LoginScreen(
                googleAvailable = GoogleSignIn.available,
                onGoogle = {
                    when (val g = GoogleSignIn.idToken(context as Activity)) {
                        is GoogleSignIn.Result.Token -> signIn(app, withContext(Dispatchers.IO) { api.signInGoogle(g.idToken) })
                        GoogleSignIn.Result.Cancelled -> null
                        is GoogleSignIn.Result.Error -> g.message
                    }
                },
                onEmail = { signUp, name, email, password ->
                    signIn(
                        app,
                        withContext(Dispatchers.IO) {
                            if (signUp) api.signUpEmail(name, email, password) else api.signInEmail(email, password)
                        },
                    )
                },
                onForgot = { email ->
                    when (val r = withContext(Dispatchers.IO) { api.requestPasswordReset(email) }) {
                        is CuadreApi.Result.Ok -> null
                        else -> AuthMessages.of(r) ?: "No se pudo enviar el correo. Inténtalo otra vez."
                    }
                },
                onBack = { app.prefs.setMode(null) },
            )
            mode == UseMode.OWNER && link == null -> OwnerStoreScreen(
                email = session?.email.orEmpty(),
                defaultName = android.os.Build.MODEL,
                onLink = { name, storeName -> linkOwnPhone(app, name, storeName) },
                onSignOut = {
                    val token = session?.token
                    app.account.clear()
                    app.prefs.setMode(null)
                    if (token != null) scope.launch(Dispatchers.IO) { api.signOut(token) }
                },
            )
            !onboarded -> Box(Modifier.statusBarsPadding()) { setupScreen() }
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
                            storeName = link?.storeName,
                            closeLabel = todayClose?.let {
                                "Contaste ${pe.aido.cuadre.ui.soles(it.countedCash)} · " +
                                    CashClose.differenceLabel(it.countedCash - it.expectedCash)
                            },
                            debugAction = if (BuildConfig.DEBUG) {
                                { TextLink("Simular yapeo (debug)", { scope.launch { DebugPayments.simulate(context) } }, muted = true) }
                            } else null,
                        )
                        Tab.Historial -> HistoryScreen(all, closes, actions.exportMonth)
                        Tab.Ajustes -> settingsScreen()
                    }
                }
                BottomNav(tab, { tab = it })
            }
        }

        if (cashOpen) CashSaleDialog(
            onAdd = { amount ->
                scope.launch { app.addCash(amount, System.currentTimeMillis()) }
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


        if (privacyOpen) PrivacyDialog(link?.storeName) { privacyOpen = false }
    }
}

/** Privacy in four short answers instead of one paragraph: what, what not, where it goes, how to stop. */
@Composable
private fun PrivacyDialog(linkedStore: String?, onClose: () -> Unit) {
    val c = Cuadre.colors
    val t = Cuadre.type
    val points = listOf(
        "Qué lee" to "Solo los avisos de pagos recibidos en Yape y Plin: monto, quién pagó, hora y código.",
        "Qué no hace" to "No lee otras notificaciones, no entra a tus cuentas y no mueve dinero.",
        "A dónde va" to if (linkedStore == null) {
            "A ningún lado. Todo se guarda solo en este celular."
        } else {
            "Cada pago confirmado se comparte con $linkedStore: el servidor de Cuadre y los celulares de la tienda."
        },
        "Cómo pararlo" to if (linkedStore == null) {
            "Quita el acceso a notificaciones en los ajustes del teléfono."
        } else {
            "Ajustes → Tu cuenta para dejar de compartir. O quita el acceso a notificaciones del teléfono."
        },
    )
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("Entendido", color = c.primary, fontWeight = FontWeight.SemiBold) } },
        // The full policy (also the URL in the Play listing).
        dismissButton = if (BuildConfig.CUADRE_API_URL.isNotBlank()) {
            {
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                TextButton(onClick = { uri.openUri(BuildConfig.CUADRE_API_URL.trimEnd('/') + "/privacidad") }) {
                    Text("Ver política completa", color = c.inkMuted)
                }
            }
        } else null,
        title = { Text("Qué hace Cuadre con tus datos", style = t.section, color = c.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                points.forEach { (head, body) ->
                    Column {
                        Text(head, style = t.secondary.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
                        Spacer(Modifier.height(2.dp))
                        Text(body, style = t.body, color = c.ink)
                    }
                }
            }
        },
        containerColor = c.paper,
    )
}

/** Saves the session from a sign-in / sign-up answer. Error text, or null when signed in. */
private fun signIn(app: CuadreApp, r: CuadreApi.Result<SyncCodec.Session>): String? {
    AuthMessages.of(r)?.let { return it }
    app.account.save((r as CuadreApi.Result.Ok).value)
    return null
}

/** Links this phone to the signed-in owner's store, no code. */
private suspend fun linkOwnPhone(app: CuadreApp, name: String, storeName: String?): LinkOutcome {
    val session = app.account.session.value ?: return LinkOutcome.Error("Vuelve a entrar a tu cuenta.")
    return when (val r = withContext(Dispatchers.IO) { CuadreApi().linkOwnDevice(session.token, name, storeName) }) {
        is CuadreApi.Result.Ok -> {
            app.storeLink.save(r.value, linkedAt = System.currentTimeMillis())
            PushRegisterWorker.enqueue(app)
            SyncWorker.enqueue(app)
            LinkOutcome.Done
        }
        is CuadreApi.Result.Rejected -> when (r.status) {
            404 -> LinkOutcome.NeedStoreName
            401 -> {
                app.account.clear()
                LinkOutcome.Error("Tu sesión venció. Vuelve a entrar.")
            }
            else -> LinkOutcome.Error("No se pudo conectar. Inténtalo de nuevo.")
        }
        is CuadreApi.Result.Failed -> LinkOutcome.Error("Sin conexión. Revisa tu internet e inténtalo de nuevo.")
    }
}

/** Trades the owner's 6-digit code for this phone's store membership. Error text, or null. */
private suspend fun pairPhone(app: CuadreApp, code: String, name: String): String? =
    when (val r = withContext(Dispatchers.IO) { CuadreApi().pair(code, name) }) {
        is CuadreApi.Result.Ok -> {
            app.storeLink.save(r.value, linkedAt = System.currentTimeMillis())
            PushRegisterWorker.enqueue(app)
            SyncWorker.enqueue(app)
            null
        }
        is CuadreApi.Result.Rejected ->
            if (r.status == 429) "Demasiados códigos equivocados. Espera 15 minutos e inténtalo otra vez."
            else "Código incorrecto o vencido. Pídele al dueño uno nuevo."
        is CuadreApi.Result.Failed -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
    }
