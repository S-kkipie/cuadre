package pe.aido.cuadre.core

import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet

/**
 * The moat. Turns a notification (package + title + text) into a [PaymentEvent].
 *
 * Rules:
 *  - incoming-only: if we cannot positively identify an INCOMING payment, we do not emit income;
 *  - no guessing: unreadable amount -> null (surfaced to the user as "check manually"),
 *    never a fabricated number;
 *  - defensive: notification wording changes over time, so matching is keyword + amount based
 *    and designed to degrade to "a payment arrived, amount unknown" rather than lie.
 *
 * Pure Kotlin. Unit-tested on the JVM with captured real-notification fixtures.
 */
object PaymentParser {

    // Incoming cues seen in Yape/Plin notifications. Extend from real captures.
    private val incomingCues = listOf(
        "te yapearon", "te envió", "te envio", "recibiste", "pago recibido",
        "recibió", "recibio", "te hizo un pago", "abono",
    )
    private val outgoingCues = listOf(
        "yapeaste", "enviaste", "pagaste", "tu pago de", "realizaste un pago",
    )

    // S/ 50  | S/50.00 | S/ 1,250.50
    private val amountRegex = Regex("""s/\.?\s?([\d.,]+)""", RegexOption.IGNORE_CASE)

    fun parse(packageName: String, title: String?, text: String?, postedAtMillis: Long): PaymentEvent? {
        val wallet = Wallet.fromPackage(packageName)
        if (wallet == Wallet.UNKNOWN) return null

        val raw = listOfNotNull(title, text).joinToString(" ").trim()
        if (raw.isEmpty()) return null
        val lower = raw.lowercase()

        val direction = when {
            outgoingCues.any { lower.contains(it) } -> PaymentDirection.OTHER
            incomingCues.any { lower.contains(it) } -> PaymentDirection.INCOMING
            else -> return null // not clearly a payment event
        }
        if (direction != PaymentDirection.INCOMING) return null

        val amount = extractAmount(raw) ?: return null
        val counterparty = extractCounterparty(raw)

        return PaymentEvent(
            wallet = wallet,
            amount = amount,
            counterparty = counterparty,
            direction = PaymentDirection.INCOMING,
            postedAtMillis = postedAtMillis,
            rawText = raw,
        )
    }

    internal fun extractAmount(raw: String): Double? {
        val match = amountRegex.find(raw)?.groupValues?.getOrNull(1) ?: return null
        // Normalize "1,250.50" and "1250,50" -> 1250.50; reject garbage.
        val cleaned = normalizeNumber(match) ?: return null
        return cleaned.takeIf { it > 0.0 }
    }

    private fun normalizeNumber(s: String): Double? {
        val t = s.trim().trimEnd('.', ',')
        if (t.isEmpty()) return null
        val hasComma = t.contains(',')
        val hasDot = t.contains('.')
        val normalized = when {
            hasComma && hasDot -> t.replace(",", "")                 // 1,250.50
            hasComma && !hasDot -> t.replace(".", "").replace(",", ".") // 1.250,50 or 1250,50
            else -> t
        }
        return normalized.toDoubleOrNull()
    }

    /** Best-effort payer name; null when not clearly present. Never blocks income capture. */
    internal fun extractCounterparty(raw: String): String? {
        // Pattern: "<Name> te yapeó/te envió ..."
        val before = Regex("""^(.*?)\s+(te\s+(?:yape|envi|hizo)|recib)""", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.getOrNull(1)?.trim()
        return before?.takeIf { it.isNotEmpty() && it.length <= 40 && it.any(Char::isLetter) }
    }
}
