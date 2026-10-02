package pe.aido.cuadre.domain

/** Supported wallets. Source app package → Wallet lives in [Wallet.fromPackage]. */
enum class Wallet(val displayName: String, val packages: Set<String>) {
    // Verified on a real device (2026-10-02): Yape, BBVA.
    YAPE("Yape", setOf("com.bcp.innovacxion.yapeapp")),
    PLIN_BBVA("Plin (BBVA)", setOf("com.bbva.nxt_peru")),

    // Plin lives inside bank/caja apps. Ids below come from other open-source listeners
    // (github.com/Keny4747/motocaja, 2026-10) and are unverified on our devices.
    PLIN_INTERBANK("Plin (Interbank)", setOf("pe.com.interbank.mobilebanking", "pe.com.interbank.mpay.customer")),
    PLIN_SCOTIABANK("Plin (Scotiabank)", setOf("pe.com.scotiabank.blpm.android.client")),
    PLIN_BANBIF("Plin (BanBif)", setOf("pe.com.banbif.pnappmobile")),
    PLIN_CAJA_AREQUIPA("Plin (Caja Arequipa)", setOf("com.cmac.cajamovilaqp", "com.cajaarq.p51")),
    PLIN_CAJA_ICA("Plin (Caja Ica)", setOf("com.cmacica.prd")),
    PLIN_CAJA_HUANCAYO("Plin (Caja Huancayo)", setOf("com.cajahuancayo.cajahuancayo.appcajahuancayo")),
    PLIN_CONFIANZA("Plin (Financiera Confianza)", setOf("pe.confianza.cliente")),
    PLIN_ALFIN("Plin (Alfin Banco)", setOf("com.alfinbanco.appclientes")),
    PLIN_LIGO("Plin (Ligo)", setOf("pe.com.tarjetasperuanasprepago.tppapp")),
    PLIN_MIBANCO("Plin (Mibanco)", setOf("com.mibanco.bancamovil")),
    PLIN_PICHINCHA("Plin (Pichincha)", setOf("pe.pichincha.bm")),

    // Entered by hand at the till; never comes from a notification (no packages).
    EFECTIVO("Efectivo", emptySet()),

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
 * `securityCode` is the short code some Yape notifications carry ("cód. de seguridad"),
 * which the payer also sees — the merchant can ask for it to match a customer to a payment.
 */
data class PaymentEvent(
    val wallet: Wallet,
    val amount: Double,
    val counterparty: String?,
    val direction: PaymentDirection,
    val postedAtMillis: Long,
    val rawText: String,
    val securityCode: String? = null,
) {
    val isUsableIncome: Boolean
        get() = direction == PaymentDirection.INCOMING && amount > 0.0
}
