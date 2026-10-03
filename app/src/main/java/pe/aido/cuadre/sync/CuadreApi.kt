package pe.aido.cuadre.sync

import pe.aido.cuadre.BuildConfig
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Minimal client for cuadre-backend (`/api/v1`). Blocking: call from Dispatchers.IO or a worker. */
class CuadreApi(private val baseUrl: String = BuildConfig.CUADRE_API_URL) {

    val configured: Boolean get() = baseUrl.isNotBlank()

    sealed class Result<out T> {
        data class Ok<T>(val value: T) : Result<T>()
        /** The server answered and said no (bad code, revoked device, wrong password…). Don't retry. */
        data class Rejected(val status: Int, val body: String = "") : Result<Nothing>()
        /** No connection or a 5xx. Retry later. */
        data class Failed(val reason: String) : Result<Nothing>()
    }

    fun pair(code: String, deviceName: String): Result<SyncCodec.Paired> =
        call("POST", "/devices/pair", null, SyncCodec.pairBody(code, deviceName)) { SyncCodec.parsePaired(it) }

    fun registerPush(token: String, fcmToken: String): Result<Unit> =
        call("PUT", "/devices/me/push", token, SyncCodec.pushBody(fcmToken)) { }

    fun upload(token: String, payment: ConfirmedPayment): Result<Unit> =
        call("POST", "/payments", token, SyncCodec.uploadBody(payment)) { }

    // --- Account (Better Auth). The session token travels as a bearer, never as a cookie. ---

    fun signInEmail(email: String, password: String): Result<SyncCodec.Session> =
        call("POST", "/auth/sign-in/email", null, SyncCodec.signInBody(email, password)) { SyncCodec.parseSession(it) }

    fun signUpEmail(name: String, email: String, password: String): Result<SyncCodec.Session> =
        call("POST", "/auth/sign-up/email", null, SyncCodec.signUpBody(name, email, password)) { SyncCodec.parseSession(it) }

    /** Google ID token from Credential Manager; the server checks it against Google's keys. */
    fun signInGoogle(idToken: String): Result<SyncCodec.Session> =
        call("POST", "/auth/sign-in/social", null, SyncCodec.googleBody(idToken)) { SyncCodec.parseSession(it) }

    fun signOut(session: String): Result<Unit> = call("POST", "/auth/sign-out", session, "{}") { }

    /** Signed-in owner links this phone to their store, no code. 404 = no store yet: ask its name. */
    fun linkOwnDevice(session: String, deviceName: String, storeName: String?): Result<SyncCodec.Paired> =
        call("POST", "/stores/mine/devices", session, SyncCodec.linkOwnBody(deviceName, storeName)) { SyncCodec.parsePaired(it) }

    private fun <T> call(method: String, path: String, bearer: String?, body: String, parse: (String) -> T): Result<T> {
        if (!configured) return Result.Failed("CUADRE_API_URL not set")
        val conn = (URL(baseUrl.trimEnd('/') + "/api/v1" + path).openConnection() as HttpURLConnection)
        return try {
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            bearer?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            conn.outputStream.use { it.write(body.toByteArray()) }
            val status = conn.responseCode
            when {
                status in 200..299 -> Result.Ok(parse(conn.inputStream.bufferedReader().use { it.readText() }))
                status >= 500 -> Result.Failed("HTTP $status")
                else -> Result.Rejected(status, conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())
            }
        } catch (e: IOException) {
            Result.Failed(e.message ?: "IO")
        } finally {
            conn.disconnect()
        }
    }
}
