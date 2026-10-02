package pe.aido.cuadre.domain

/** Supported wallets. Source app package → Wallet lives in [Wallet.fromPackage]. */
enum class Wallet(val displayName: String, val packages: Set<String>) {
    // NOTE: verify these package ids on a real device before shipping.
    YAPE("Yape", setOf("com.bcp.innovacxp.yapeapp")),
    // Plin is embedded inside bank apps; add each bank package as captured.
    PLIN_INTERBANK("Plin (Interbank)", setOf("pe.com.interbank.mobilebanking")),
    PLIN_BBVA("Plin (BBVA)", setOf("com.bbva.nxt_peru")),
    UNKNOWN("Desconocido", emptySet());

    companion object {
        fun fromPackage(pkg: String): Wallet =
            entries.firstOrNull { pkg in it.packages } ?: UNKNOWN
    }
}

enum class PaymentDirection { INCOMING, OTHER }

/**
 * A parsed payment. `direction` must be INCOMING to count. `counterparty` may be null
 * when the notification does not expose the payer. Amount is in soles.
 */
data class PaymentEvent(
    val wallet: Wallet,
    val amount: Double,
    val counterparty: String?,
    val direction: PaymentDirection,
    val postedAtMillis: Long,
    val rawText: String,
) {
    val isUsableIncome: Boolean
        get() = direction == PaymentDirection.INCOMING && amount > 0.0
}
