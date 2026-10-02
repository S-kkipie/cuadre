package pe.aido.cuadre.core

/**
 * When a confirmed payment may be shown as "just arrived". The anti-fraud promise is about
 * the customer in front of you: a payment from minutes ago must look visibly different, or a
 * scammer could time a fake screenshot to an unrelated earlier payment.
 */
object ConfirmationPolicy {

    const val FRESH_WINDOW_MILLIS = 60_000L

    fun isFresh(postedAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis - postedAtMillis <= FRESH_WINDOW_MILLIS

    fun ageLabel(postedAtMillis: Long, nowMillis: Long): String {
        val seconds = ((nowMillis - postedAtMillis) / 1000).coerceAtLeast(0)
        return when {
            seconds < 1 -> "ahora"
            seconds < 60 -> "hace $seconds s"
            seconds < 3600 -> "hace ${seconds / 60} min"
            else -> "hace ${seconds / 3600} h"
        }
    }
}
