package pe.aido.cuadre.sync

import org.json.JSONObject
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet

/** Wire format shared with cuadre-backend (`uploadPaymentSchema` / FCM data payload). */
object SyncCodec {

    fun uploadBody(p: ConfirmedPayment): String = JSONObject().apply {
        put("id", p.id)
        put("wallet", p.event.wallet.name)
        put("amount", p.event.amount)
        p.event.counterparty?.let { put("counterparty", it) }
        p.event.securityCode?.let { put("securityCode", it) }
        put("postedAt", p.event.postedAtMillis)
    }.toString()

    fun pairBody(code: String, deviceName: String): String =
        JSONObject().put("code", code).put("name", deviceName).toString()

    fun pushBody(fcmToken: String): String = JSONObject().put("fcmToken", fcmToken).toString()

    data class Paired(val deviceId: String, val token: String, val storeId: String, val storeName: String)

    /** `{ response: { deviceId, token, storeId, storeName }, code, status }` */
    fun parsePaired(json: String): Paired {
        val r = JSONObject(json).getJSONObject("response")
        return Paired(r.getString("deviceId"), r.getString("token"), r.getString("storeId"), r.getString("storeName"))
    }

    /**
     * FCM data payload → payment. Null if it isn't a payment or is malformed: a push can never
     * invent money, so anything we can't read is dropped rather than guessed.
     */
    fun fromPush(data: Map<String, String>): RemotePayment? {
        if (data["type"] != "payment") return null
        val id = data["id"]?.takeIf { it.isNotBlank() } ?: return null
        val amount = data["amount"]?.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        val postedAt = data["postedAt"]?.toLongOrNull() ?: return null
        val wallet = Wallet.entries.firstOrNull { it.name == data["wallet"] } ?: Wallet.UNKNOWN
        val payment = ConfirmedPayment(
            id = id,
            event = PaymentEvent(
                wallet = wallet,
                amount = amount,
                counterparty = data["counterparty"]?.takeIf { it.isNotBlank() },
                direction = PaymentDirection.INCOMING,
                postedAtMillis = postedAt,
                rawText = "remoto",
                securityCode = data["securityCode"]?.takeIf { it.isNotBlank() },
            ),
        )
        return RemotePayment(payment, fromDevice = data["fromDevice"], storeName = data["storeName"])
    }

    data class RemotePayment(val payment: ConfirmedPayment, val fromDevice: String?, val storeName: String?)
}
