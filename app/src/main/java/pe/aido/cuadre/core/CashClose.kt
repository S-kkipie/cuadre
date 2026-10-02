package pe.aido.cuadre.core

import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.domain.Wallet
import java.util.Locale
import kotlin.math.abs

/**
 * End-of-day "cuadre de caja": what should be in the drawer vs. what was counted.
 * Digital payments (Yape/Plin) are already proven by the listener; cash is what needs counting.
 */
object CashClose {

    data class Result(
        val byWallet: Map<Wallet, Double>,
        val cashSales: Double,
        val openingCash: Double,
        val countedCash: Double,
        val expectedCash: Double,
        /** counted − expected: negative = missing money, positive = extra. */
        val difference: Double,
        val digitalTotal: Double,
        val salesTotal: Double,
        val count: Int,
    )

    fun compute(day: List<ConfirmedPayment>, openingCash: Double, countedCash: Double): Result {
        val byWallet = VerificationEngine.dailyClose(day)
        val cash = byWallet[Wallet.EFECTIVO] ?: 0.0
        val total = VerificationEngine.total(day)
        val expected = openingCash + cash
        return Result(
            byWallet = byWallet,
            cashSales = cash,
            openingCash = openingCash,
            countedCash = countedCash,
            expectedCash = expected,
            difference = round2(countedCash - expected),
            digitalTotal = total - cash,
            salesTotal = total,
            count = day.size,
        )
    }

    /** Plain text to paste in WhatsApp. */
    fun shareText(dayLabel: String, r: Result): String = buildString {
        appendLine("Cuadre · $dayLabel")
        r.byWallet.entries.sortedByDescending { it.value }.forEach { (w, sum) -> appendLine("${w.shortName}: ${soles(sum)}") }
        appendLine("Total vendido: ${soles(r.salesTotal)} (${r.count} ${if (r.count == 1) "venta" else "ventas"})")
        appendLine("Efectivo esperado: ${soles(r.expectedCash)} · contado: ${soles(r.countedCash)}")
        append(differenceLabel(r.difference))
    }

    fun differenceLabel(diff: Double): String = when {
        abs(diff) < 0.005 -> "Caja cuadrada"
        diff < 0 -> "Faltan ${soles(-diff)}"
        else -> "Sobran ${soles(diff)}"
    }

    private fun round2(v: Double) = Math.round(v * 100) / 100.0
    private fun soles(v: Double) = String.format(Locale.US, "S/ %,.2f", v)
}
