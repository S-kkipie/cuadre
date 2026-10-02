package pe.aido.cuadre.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import pe.aido.cuadre.core.PaymentParser
import pe.aido.cuadre.domain.Wallet

/**
 * Captures incoming payment notifications on device. The only entry point of truth:
 * a payment exists only if the OS delivered its notification here. A screenshot cannot.
 *
 * STUB persistence: parsed events are handed to a repository (TODO) that runs
 * VerificationEngine.confirm against recent payments and inserts via PaymentDao
 * (idempotent). Kept thin so the trust logic stays in the unit-tested core.
 */
class PaymentNotificationListenerService : NotificationListenerService() {

    private val watchedPackages: Set<String> =
        Wallet.entries.flatMap { it.packages }.toSet()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in watchedPackages) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()

        val event = PaymentParser.parse(pkg, title, text, sbn.postTime) ?: run {
            // TODO: log an "unreadable payment notification" so the parser can be hardened.
            return
        }

        // TODO: PaymentRepository.onEvent(event) -> VerificationEngine.confirm -> dao.insert
        // -> emit to UI + fire anti-fraud confirmation.
    }
}
