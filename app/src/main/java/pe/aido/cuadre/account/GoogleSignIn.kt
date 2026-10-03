package pe.aido.cuadre.account

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import pe.aido.cuadre.BuildConfig

/**
 * "Continuar con Google" through Android's Credential Manager. Returns Google's ID token for the
 * backend to verify, or an error message. Cancelled by the user → [Result.Cancelled].
 */
object GoogleSignIn {
    sealed class Result {
        data class Token(val idToken: String) : Result()
        data object Cancelled : Result()
        data class Error(val message: String) : Result()
    }

    /** Hidden when the build has no Web client ID (e.g. a fork without the Firebase project). */
    val available: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun idToken(activity: Activity): Result {
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.Error("No se pudo entrar con Google. Prueba con tu correo.")
            }
        } catch (_: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (_: GetCredentialException) {
            Result.Error("No se pudo entrar con Google. Prueba con tu correo.")
        }
    }
}
