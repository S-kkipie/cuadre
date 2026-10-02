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
        "te yapearon", "te yapeó", "te yapeo", "te ha yapeado", "te han yapeado",
        "te plineó", "te plineo", "te plinearon", "te ha plineado", "te han plineado",
        "te hizo un plin", "te hicieron un plin", "te llegó un plin", "te llego un plin",
        "te envió", "te envio", "te hizo un pago", "recibiste", "pago recibido",
        "recibió", "recibio", "abono",
    )
    // Checked first: anything that reads as outgoing is never income, whatever else it says.
    private val outgoingCues = listOf(
        "yapeaste", "plineaste", "enviaste", "pagaste", "tu pago de", "realizaste un pago",
        "plin enviado", "fue enviado", "realizaste un plin", "hiciste un plin",
    )

    // Headers the wallet puts before the payer's name: "Confirmación de Pago:", "Yape!",
    // "¡Plin!", "Cobro con QR:". \b keeps real names like "Plinio" intact.
    private val namePrefix = Regex(
        """^(?:\s*(?:confirmaci[oó]n\s+de\s+pago|cobro\s+con\s+qr|¡?\s*(?:yape|plin)\b)\s*[:!]?)+\s*""",
        RegexOption.IGNORE_CASE,
    )

    // "<Name> te yapeó / te plineó / te envió / te hizo / te ha(n) ..."
    private val nameBefore = Regex(
        """^(.*?)\s+(?:te\s+(?:yape|pline|envi|hizo|hicieron|ha\s|han\s|lleg)|recib)""",
        RegexOption.IGNORE_CASE,
    )

    // Legacy Yape: "Te yapeó S/ 10.00 de Juan Perez"
    private val nameAfterAmount = Regex("""s/\.?\s?[\d.,]+\s+de\s+(.+)$""", RegexOption.IGNORE_CASE)

    // "El cód. de seguridad es: 296"
    private val securityCodeRegex = Regex(
        """c[oó]d(?:igo)?\.?\s*(?:de\s+)?seguridad(?:\s+es)?\s*:?\s*(\d{3,6})""",
        RegexOption.IGNORE_CASE,
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
        // Payer comes from the body only; the title is usually the app/header ("Yape").
        val counterparty = text?.trim()?.let(::extractCounterparty)

        return PaymentEvent(
            wallet = wallet,
            amount = amount,
            counterparty = counterparty,
            direction = PaymentDirection.INCOMING,
            postedAtMillis = postedAtMillis,
            rawText = raw,
            securityCode = securityCodeRegex.find(raw)?.groupValues?.get(1),
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
            // Both present: whichever comes last is the decimal separator.
            hasComma && hasDot && t.lastIndexOf('.') > t.lastIndexOf(',') -> t.replace(",", "") // 1,250.50
            hasComma && hasDot -> t.replace(".", "").replace(",", ".")                            // 1.250,50
            hasComma && !hasDot -> t.replace(".", "").replace(",", ".") // 1.250,50 or 1250,50
            else -> t
        }
        return normalized.toDoubleOrNull()
    }

    /** Best-effort payer name; null when not clearly present. Never blocks income capture. */
    internal fun extractCounterparty(raw: String): String? {
        // Real Yape: "Yape! ADRIAN MAMANI te envió un pago por S/ 1"
        // Real BBVA Plin: "ADRIAN ISSAC MAMANI te plineó S/1 ."
        val before = nameBefore.find(raw)?.groupValues?.get(1)?.replace(namePrefix, "")?.let(::cleanName)
        if (before != null) return before
        return nameAfterAmount.find(raw)?.groupValues?.get(1)?.let(::cleanName)
    }

    private fun cleanName(s: String): String? =
        s.trim().trimEnd('.', '!', ' ').trim()
            .takeIf { it.isNotEmpty() && it.length <= 40 && it.any(Char::isLetter) }
}
