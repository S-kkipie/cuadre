package pe.aido.cuadre.notifications

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pe.aido.cuadre.BuildConfig
import pe.aido.cuadre.repository
import pe.aido.cuadre.core.PaymentParser
import pe.aido.cuadre.domain.Wallet

/**
 * Captures incoming payment notifications on device. The only entry point of truth:
 * a payment exists only if the OS delivered its notification here. A screenshot cannot.
 *
 * Parsed events go to [pe.aido.cuadre.data.PaymentRepository], which runs
 * VerificationEngine.confirm against nearby payments and inserts idempotently.
 * Kept thin so the trust logic stays in the unit-tested core.
 */
class PaymentNotificationListenerService : NotificationListenerService() {

    private val watchedPackages: Set<String> =
        Wallet.entries.flatMap { it.packages }.toSet()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // Android can unbind the listener (app update, low memory) and not bring it back on its
    // own; ask to be rebound so capture does not silently stop.
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        requestRebind(ComponentName(this, PaymentNotificationListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in watchedPackages) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()
        val bigText = extras.getCharSequence("android.bigText")?.toString()

        // Debug builds: dump the raw notification so real wording can become parser fixtures.
        if (BuildConfig.DEBUG) {
            Log.i(
                "CuadreListener",
                "pkg=$pkg title=$title text=$text bigText=$bigText " +
                    "subText=${extras.getCharSequence("android.subText")}",
            )
        }

        // bigText is the full expanded body; android.text may be cut short (e.g. the security code).
        val body = bigText?.takeIf { it.isNotBlank() } ?: text
        val event = PaymentParser.parse(pkg, title, body, sbn.postTime) ?: run {
            // TODO: log an "unreadable payment notification" so the parser can be hardened.
            return
        }

        // The UI observes Room, so storing is enough to show it. TODO: anti-fraud confirmation (D7).
        scope.launch { repository.onEvent(event) }
    }
}
