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

    /** A Better Auth session for this phone's account. */
    data class Session(val token: String, val name: String, val email: String)

    fun signInBody(email: String, password: String): String =
        JSONObject().put("email", email.trim()).put("password", password).toString()

    fun signUpBody(name: String, email: String, password: String): String =
        JSONObject().put("name", name.trim()).put("email", email.trim()).put("password", password).toString()

    fun resetBody(email: String, redirectTo: String): String =
        JSONObject().put("email", email.trim()).put("redirectTo", redirectTo).toString()

    fun googleBody(idToken: String): String =
        JSONObject().put("provider", "google").put("idToken", JSONObject().put("token", idToken)).toString()

    fun linkOwnBody(deviceName: String, storeName: String?): String = JSONObject().apply {
        put("name", deviceName)
        storeName?.trim()?.takeIf { it.isNotEmpty() }?.let { put("storeName", it) }
    }.toString()

    /** `{ token, user: { name, email } }` (sign-in, sign-up and social idToken all answer this way). */
    fun parseSession(json: String): Session {
        val o = JSONObject(json)
        val user = o.getJSONObject("user")
        return Session(o.getString("token"), user.optString("name"), user.getString("email"))
    }

    /** Better Auth error code (e.g. INVALID_EMAIL_OR_PASSWORD) from an error body, or null. */
    fun errorCode(body: String): String? =
        runCatching { JSONObject(body).optString("code").takeIf { it.isNotBlank() } }.getOrNull()

    /**
     * `{ response: [{ id, wallet, amount, counterparty, securityCode, postedAt (ISO), sourceDevice }] }`.
     * Same rule as a push: a row we can't read is dropped, never guessed.
     */
    fun parseStorePayments(json: String): List<ConfirmedPayment> {
        val rows = JSONObject(json).getJSONArray("response")
        return (0 until rows.length()).mapNotNull { i ->
            val r = rows.getJSONObject(i)
            val id = r.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val amount = r.optDouble("amount").takeIf { !it.isNaN() && it > 0 } ?: return@mapNotNull null
            val postedAt = runCatching { java.time.Instant.parse(r.getString("postedAt")).toEpochMilli() }.getOrNull()
                ?: return@mapNotNull null
            ConfirmedPayment(
                id = id,
                event = PaymentEvent(
                    wallet = Wallet.entries.firstOrNull { it.name == r.optString("wallet") } ?: Wallet.UNKNOWN,
                    amount = amount,
                    counterparty = r.optNullable("counterparty"),
                    direction = PaymentDirection.INCOMING,
                    postedAtMillis = postedAt,
                    rawText = "remoto",
                    securityCode = r.optNullable("securityCode"),
                ),
                fromDevice = r.optNullable("sourceDevice"),
            )
        }
    }

    private fun JSONObject.optNullable(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

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
        val from = data["fromDevice"]?.takeIf { it.isNotBlank() }
        return RemotePayment(payment.copy(fromDevice = from), fromDevice = from, storeName = data["storeName"])
    }

    data class RemotePayment(val payment: ConfirmedPayment, val fromDevice: String?, val storeName: String?)
}
