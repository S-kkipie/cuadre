package pe.aido.cuadre.debug

import android.content.Context
import pe.aido.cuadre.core.PaymentParser
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.domain.Wallet
import pe.aido.cuadre.cuadre

/**
 * Debug-only stand-in for a real wallet notification. Goes through the same parser and
 * repository as the listener, so it exercises everything except the OS delivery itself.
 * Callers must gate on BuildConfig.DEBUG.
 */
object DebugPayments {

    private val samplePayers = listOf("Juan Perez", "Maria Quispe", "Rosa Huaman", "Carlos Mamani")
    private val sampleAmounts = listOf(3.5, 8.0, 12.5, 25.0, 50.0, 120.0)

    fun sampleYapeText(): String =
        "${samplePayers.random()} te yapeó S/ ${"%.2f".format(java.util.Locale.US, sampleAmounts.random())}"

    suspend fun simulate(
        context: Context,
        text: String = sampleYapeText(),
        title: String? = "Yape",
        packageName: String = Wallet.YAPE.packages.first(),
    ): ConfirmedPayment? {
        val event = PaymentParser.parse(packageName, title, text, System.currentTimeMillis()) ?: return null
        return context.cuadre.capture(event)
    }
}
