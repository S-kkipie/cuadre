package pe.aido.cuadre.core

import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet
import kotlin.math.abs

/**
 * Deterministic trust layer. A payment is "real" because it arrived through the system
 * notification listener — a screenshot never reaches here. This engine's only jobs are to
 * reject non-income and to collapse duplicate notifications for the same payment.
 */
object VerificationEngine {

    /** Two events are the same payment if same wallet + amount and within this window. */
    const val DEDUPE_WINDOW_MILLIS = 60_000L

    data class ConfirmedPayment(
        val id: String,
        val event: PaymentEvent,
    )

    /** Accept an incoming event unless it duplicates one already confirmed. */
    fun confirm(event: PaymentEvent, existing: List<ConfirmedPayment>): ConfirmedPayment? {
        if (!event.isUsableIncome) return null
        if (existing.any { isSamePayment(it.event, event) }) return null
        return ConfirmedPayment(id = idFor(event), event = event)
    }

    fun isSamePayment(a: PaymentEvent, b: PaymentEvent): Boolean =
        a.wallet == b.wallet &&
            abs(a.amount - b.amount) < 0.005 &&
            abs(a.postedAtMillis - b.postedAtMillis) <= DEDUPE_WINDOW_MILLIS &&
            normalize(a.counterparty) == normalize(b.counterparty)

    private fun normalize(name: String?): String = name?.trim()?.lowercase() ?: ""

    private fun idFor(e: PaymentEvent): String =
        "${e.wallet.name}:${"%.2f".format(e.amount)}:${e.postedAtMillis}"

    /** Daily close: total confirmed income, broken down by wallet. */
    fun dailyClose(payments: List<ConfirmedPayment>): Map<Wallet, Double> =
        payments.groupBy { it.event.wallet }
            .mapValues { (_, list) -> list.sumOf { it.event.amount } }

    fun total(payments: List<ConfirmedPayment>): Double =
        payments.sumOf { it.event.amount }
}
