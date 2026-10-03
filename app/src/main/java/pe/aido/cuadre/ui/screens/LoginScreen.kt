package pe.aido.cuadre.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import pe.aido.cuadre.R
import pe.aido.cuadre.ui.components.Hairline
import pe.aido.cuadre.ui.components.OutlineButton
import pe.aido.cuadre.ui.components.PrimaryButton
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.theme.Cuadre

/**
 * First screen: an account ties this phone to the owner's store. Google in one tap, or email and
 * password. Each action returns an error message, or null when signed in.
 */
@Composable
fun LoginScreen(
    googleAvailable: Boolean,
    onGoogle: suspend () -> String?,
    onEmail: suspend (signUp: Boolean, name: String, email: String, password: String) -> String?,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    var signUp by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }
    // Bumped to run an action; the effect owns the coroutine so rotation doesn't double-submit.
    var googleRun by rememberSaveable { mutableIntStateOf(0) }
    var emailRun by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(googleRun) {
        if (googleRun == 0) return@LaunchedEffect
        busy = true
        error = onGoogle()
        busy = false
    }
    LaunchedEffect(emailRun) {
        if (emailRun == 0) return@LaunchedEffect
        busy = true
        error = onEmail(signUp, name, email, password)
        busy = false
    }

    val formOk = email.contains('@') && password.length >= 8 && (!signUp || name.isNotBlank())

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SidePadding),
    ) {
        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(4.dp))
            Text("Cuadre", style = t.wordmark, color = c.primary)
        }
        Spacer(Modifier.height(32.dp))
        Text(if (signUp) "Crea tu cuenta" else "Entra a Cuadre", style = t.title, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Con tu cuenta, tus celulares se conectan a tu tienda. Tus pagos se siguen guardando en tu celular.",
            style = t.body,
            color = c.inkMuted,
        )

        if (googleAvailable) {
            Spacer(Modifier.height(32.dp))
            OutlineButton(
                if (busy) "Un momento…" else "Continuar con Google",
                { if (!busy) googleRun++ },
                Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Hairline() }
                Text("o con tu correo", style = t.secondary, color = c.inkMuted)
                Box(Modifier.weight(1f)) { Hairline() }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (signUp) {
            Field("Tu nombre", name, { name = it; error = null }, KeyboardType.Text)
            Spacer(Modifier.height(16.dp))
        }
        Field("Correo", email, { email = it.trim(); error = null }, KeyboardType.Email)
        Spacer(Modifier.height(16.dp))
        Field(
            if (signUp) "Contraseña (mínimo 8 caracteres)" else "Contraseña",
            password,
            { password = it; error = null },
            KeyboardType.Password,
            secret = true,
        )

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = t.body, color = c.stale)
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            when {
                busy -> "Un momento…"
                signUp -> "Crear cuenta"
                else -> "Entrar"
            },
            { emailRun++ },
            enabled = formOk && !busy,
        )
        TextLink(
            if (signUp) "Ya tengo cuenta" else "Crear una cuenta",
            { signUp = !signUp; error = null },
            muted = true,
        )
        Spacer(Modifier.height(32.dp))
    }
}

/** Label above, the value on a hairline: the same field style as the cash and pairing dialogs. */
@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType,
    secret: Boolean = false,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = t.secondary, color = c.inkMuted)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(120)) },
            textStyle = t.rowTitle.copy(color = c.ink),
            singleLine = true,
            cursorBrush = SolidColor(c.primary),
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
        Hairline()
    }
}
