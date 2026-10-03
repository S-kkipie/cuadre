package pe.aido.cuadre

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import pe.aido.cuadre.account.AuthMessages
import pe.aido.cuadre.sync.CuadreApi
import pe.aido.cuadre.sync.SyncCodec

/** Sign-in bodies, session parsing and the Spanish error text, against real Better Auth shapes. */
class AccountCodecTest {

    @Test
    fun `sign-in trims the email but never the password`() {
        val o = JSONObject(SyncCodec.signInBody("  rosa@bodega.pe ", " clave con espacios "))
        assertEquals("rosa@bodega.pe", o.getString("email"))
        assertEquals(" clave con espacios ", o.getString("password"))
    }

    @Test
    fun `google body carries the id token as Better Auth expects`() {
        val o = JSONObject(SyncCodec.googleBody("eyJhbGciOi"))
        assertEquals("google", o.getString("provider"))
        assertEquals("eyJhbGciOi", o.getJSONObject("idToken").getString("token"))
    }

    @Test
    fun `link body sends storeName only when there is one`() {
        assertFalse(JSONObject(SyncCodec.linkOwnBody("Mi celular", null)).has("storeName"))
        assertFalse(JSONObject(SyncCodec.linkOwnBody("Mi celular", "  ")).has("storeName"))
        assertEquals("Bodega Rosa", JSONObject(SyncCodec.linkOwnBody("Mi celular", " Bodega Rosa ")).getString("storeName"))
    }

    @Test
    fun `parses the session Better Auth returns`() {
        val s = SyncCodec.parseSession(
            """{"token":"8REN","user":{"name":"Rosa","email":"rosa@bodega.pe","emailVerified":false}}""",
        )
        assertEquals(SyncCodec.Session("8REN", "Rosa", "rosa@bodega.pe"), s)
    }

    @Test
    fun `wrong password and taken email read as plain Spanish`() {
        val wrong = CuadreApi.Result.Rejected(401, """{"message":"Invalid email or password","code":"INVALID_EMAIL_OR_PASSWORD"}""")
        assertEquals("Correo o contraseña incorrectos.", AuthMessages.of(wrong))
        val taken = CuadreApi.Result.Rejected(422, """{"code":"USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL"}""")
        assertEquals("Ya hay una cuenta con ese correo. Entra con tu contraseña.", AuthMessages.of(taken))
    }

    @Test
    fun `no connection and success`() {
        assertEquals("Sin conexión. Revisa tu internet e inténtalo de nuevo.", AuthMessages.of(CuadreApi.Result.Failed("IO")))
        assertNull(AuthMessages.of(CuadreApi.Result.Ok(Unit)))
        assertNull(SyncCodec.errorCode("<html>"))
    }
}
