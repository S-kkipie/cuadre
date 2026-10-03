package pe.aido.cuadre.account

import pe.aido.cuadre.sync.CuadreApi
import pe.aido.cuadre.sync.SyncCodec

/** Plain Spanish for what went wrong signing in. Null means it worked. */
object AuthMessages {
    const val TOO_MANY = "Demasiados intentos seguidos. Espera un minuto e inténtalo otra vez."

    fun of(r: CuadreApi.Result<*>): String? = when (r) {
        is CuadreApi.Result.Ok -> null
        is CuadreApi.Result.Failed -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        is CuadreApi.Result.Rejected -> if (r.status == 429) TOO_MANY else when (SyncCodec.errorCode(r.body)) {
            "INVALID_EMAIL_OR_PASSWORD" -> "Correo o contraseña incorrectos."
            "USER_ALREADY_EXISTS", "USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL" ->
                "Ya hay una cuenta con ese correo. Entra con tu contraseña."
            "PASSWORD_TOO_SHORT" -> "La contraseña debe tener al menos 8 caracteres."
            "PASSWORD_TOO_LONG" -> "La contraseña es demasiado larga."
            "INVALID_EMAIL" -> "Revisa el correo, parece que tiene un error."
            "INVALID_TOKEN", "PROVIDER_NOT_FOUND", "ID_TOKEN_NOT_SUPPORTED" ->
                "No se pudo entrar con Google. Prueba con tu correo."
            else -> "No se pudo entrar. Inténtalo de nuevo."
        }
    }
}
