package pe.aido.cuadre.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import pe.aido.cuadre.MainActivity
import pe.aido.cuadre.R
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Announces a payment the moment the core confirms it. Only ever called with payments that
 * came through the notification listener (or the debug simulator), so nothing a customer
 * shows on their own screen can trigger it.
 *
 * App visible (till mode): queue it for the full-screen confirmation.
 * App in background: high-importance notification with Cuadre's own sound; tapping opens the app.
 */
class PaymentAlerts(private val context: Context) {

    private val _pending = MutableStateFlow<List<ConfirmedPayment>>(emptyList())

    /** Confirmations waiting to be shown full-screen, oldest first. */
    val pending: StateFlow<List<ConfirmedPayment>> = _pending

    // Counted, not a flag: if two activity instances overlap (relaunch, notification tap), the
    // old one's onStop must not mark the app hidden while the new one is on screen.
    private val visibleScreens = java.util.concurrent.atomic.AtomicInteger(0)

    val uiVisible: Boolean get() = visibleScreens.get() > 0

    fun onScreenStarted() { visibleScreens.incrementAndGet() }
    fun onScreenStopped() { visibleScreens.updateAndGet { (it - 1).coerceAtLeast(0) } }

    fun announce(payment: ConfirmedPayment) {
        if (uiVisible) _pending.update { it + payment } else postNotification(payment)
    }

    fun dismiss(id: String) = _pending.update { list -> list.filterNot { it.id == id } }

    fun createChannel() {
        val sound = Uri.parse("android.resource://${context.packageName}/${R.raw.cuadre_pago}")
        val channel = NotificationChannel(CHANNEL_ID, "Pagos recibidos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso con sonido cuando llega un pago real de Yape o Plin"
            setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun postNotification(payment: ConfirmedPayment) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val e = payment.event
        val time = Instant.ofEpochMilli(e.postedAtMillis).atZone(ZoneId.systemDefault()).format(TIME)
        val detail = listOfNotNull(e.counterparty, e.wallet.displayName, time, e.securityCode?.let { "cód. $it" })
            .joinToString(" · ")
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pago)
            .setContentTitle("Pago recibido: ${soles(e.amount)}")
            .setContentText(detail)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setWhen(e.postedAtMillis)
            .setShowWhen(true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(payment.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check and the call; the payment is still stored.
        }
    }

    companion object {
        const val CHANNEL_ID = "pagos_recibidos"
        val VIBRATION_PATTERN = longArrayOf(0, 180, 90, 320)
        private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss")

        fun soles(amount: Double): String = String.format(Locale.US, "S/ %,.2f", amount)
    }
}
