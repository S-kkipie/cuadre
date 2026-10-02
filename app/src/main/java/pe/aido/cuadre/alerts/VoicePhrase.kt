package pe.aido.cuadre.alerts

import kotlin.math.roundToLong

/**
 * What the till says out loud: "Yape. 25 soles con 50 céntimos. De Rosa Huamán. Código 4, 1, 8."
 * Numbers stay as digits (the TTS engine reads them naturally in Spanish) but without thousands
 * separators, and the security code is split so it's read digit by digit.
 */
object VoicePhrase {

    fun build(wallet: String, amount: Double, payer: String?, securityCode: String?): String {
        val parts = mutableListOf("$wallet.", "${money(amount)}.")
        if (!payer.isNullOrBlank()) parts += "De $payer."
        if (!securityCode.isNullOrBlank()) parts += "Código ${securityCode.toList().joinToString(", ")}."
        return parts.joinToString(" ")
    }

    private fun money(amount: Double): String {
        val totalCents = (amount * 100).roundToLong()
        val soles = totalCents / 100
        val cents = totalCents % 100
        val solesPart = when (soles) {
            0L -> null
            1L -> "1 sol"
            else -> "$soles soles"
        }
        val centsPart = if (cents > 0) "$cents céntimos" else null
        return when {
            solesPart != null && centsPart != null -> "$solesPart con $centsPart"
            solesPart != null -> solesPart
            else -> centsPart ?: "0 soles"
        }
    }
}
