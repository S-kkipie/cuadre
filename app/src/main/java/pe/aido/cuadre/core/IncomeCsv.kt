package pe.aido.cuadre.core

import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Income ledger as CSV (opens in Excel / Google Sheets) — for the merchant's accountant. */
object IncomeCsv {
    private val date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val time = DateTimeFormatter.ofPattern("HH:mm")

    fun build(payments: List<ConfirmedPayment>, zone: ZoneId): String = buildString {
        append("fecha,hora,medio,pagador,monto,codigo")
        payments.sortedBy { it.event.postedAtMillis }.forEach { p ->
            val e = p.event
            val at = Instant.ofEpochMilli(e.postedAtMillis).atZone(zone)
            append('\n')
            append(listOf(
                at.format(date),
                at.format(time),
                e.wallet.shortName,
                e.counterparty?.let(::displayName).orEmpty(),
                String.format(Locale.US, "%.2f", e.amount),
                e.securityCode.orEmpty(),
            ).joinToString(",") { field(it) })
        }
    }

    private fun field(v: String) = if (v.any { it == ',' || it == '"' || it == '\n' }) "\"${v.replace("\"", "\"\"")}\"" else v
}
