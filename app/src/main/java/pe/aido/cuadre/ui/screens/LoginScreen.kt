package pe.aido.cuadre.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.aido.cuadre.R
import pe.aido.cuadre.ui.components.EaseOutStrong
import pe.aido.cuadre.ui.components.Hairline
import pe.aido.cuadre.ui.components.PrimaryButton
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.pressable
import pe.aido.cuadre.ui.components.pressableText
import pe.aido.cuadre.ui.theme.Cuadre

private val FieldShape = RoundedCornerShape(12.dp)

/**
 * Owner sign-in / sign-up. Google first (one tap, no password to forget), then email. The button is
 * always pressable: a press with bad input marks the exact field and says why, instead of a dead
 * grey button that never explains itself. Each action returns an error message, or null when in.
 */
@Composable
fun LoginScreen(
    googleAvailable: Boolean,
    onGoogle: suspend () -> String?,
    onEmail: suspend (signUp: Boolean, name: String, email: String, password: String) -> String?,
    onBack: () -> Unit,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    val focus = LocalFocusManager.current
    var signUp by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var tried by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf<String?>(null) } // "google" | "email"
    // Bumped to run an action; the effect owns the coroutine so rotation doesn't double-submit.
    var googleRun by rememberSaveable { mutableIntStateOf(0) }
    var emailRun by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(googleRun) {
        if (googleRun == 0) return@LaunchedEffect
        busy = "google"
        error = onGoogle()
        busy = null
    }
    LaunchedEffect(emailRun) {
        if (emailRun == 0) return@LaunchedEffect
        busy = "email"
        error = onEmail(signUp, name.trim(), email, password)
        busy = null
    }

    val nameError = if (tried && signUp && name.isBlank()) "Escribe tu nombre" else null
    val emailError = when {
        !tried -> null
        email.isBlank() -> "Escribe tu correo"
        !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email) -> "Ese correo no parece completo"
        else -> null
    }
    val passwordError = when {
        !tried -> null
        password.isEmpty() -> "Escribe tu contraseña"
        signUp && password.length < 8 -> "Usa al menos 8 caracteres"
        else -> null
    }
    val submit = {
        tried = true
        error = null
        val ok = (!signUp || name.isNotBlank()) &&
            Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email) &&
            password.isNotEmpty() && (!signUp || password.length >= 8)
        if (ok && busy == null) {
            focus.clearFocus()
            emailRun++
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SidePadding),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.heightIn(min = 48.dp).pressableText(onBack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Volver", style = t.body, color = c.inkMuted)
        }

        Spacer(Modifier.height(16.dp))
        Text(if (signUp) "Crea tu cuenta de dueño" else "Entra como dueño", style = t.title, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Con tu cuenta conectas los celulares de tu tienda y ves los pagos en la web.",
            style = t.body,
            color = c.inkMuted,
        )

        if (googleAvailable) {
            Spacer(Modifier.height(32.dp))
            GoogleButton(busy = busy == "google", enabled = busy == null) { googleRun++ }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { Hairline() }
                Text("o con tu correo", style = t.secondary, color = c.inkMuted)
                Box(Modifier.weight(1f)) { Hairline() }
            }
        }

        Spacer(Modifier.height(24.dp))
        if (signUp) {
            Field(
                label = "Tu nombre",
                value = name,
                onChange = { name = it; error = null },
                placeholder = "Como te llaman en la tienda",
                keyboard = KeyboardType.Text,
                error = nameError,
                onNext = { focus.moveFocus(FocusDirection.Down) },
            )
            Spacer(Modifier.height(20.dp))
        }
        Field(
            label = "Correo",
            value = email,
            onChange = { email = it.trim(); error = null },
            placeholder = "tucorreo@gmail.com",
            keyboard = KeyboardType.Email,
            error = emailError,
            onNext = { focus.moveFocus(FocusDirection.Down) },
        )
        Spacer(Modifier.height(20.dp))
        Field(
            label = "Contraseña",
            value = password,
            onChange = { password = it; error = null },
            placeholder = if (signUp) "Mínimo 8 caracteres" else "Tu contraseña",
            keyboard = KeyboardType.Password,
            error = passwordError,
            secret = true,
            last = true,
            onNext = submit,
        )

        error?.let {
            Spacer(Modifier.height(20.dp))
            Text(
                it,
                style = t.body,
                color = c.stale,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FieldShape)
                    .background(c.stale.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            when {
                busy == "email" && signUp -> "Creando tu cuenta…"
                busy == "email" -> "Entrando…"
                signUp -> "Crear cuenta"
                else -> "Entrar"
            },
            submit,
            enabled = busy == null,
        )

        // The switch reads as a question + answer, so it's clear it changes the form, not submits it.
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (signUp) "¿Ya tienes cuenta?" else "¿Primera vez?", style = t.body, color = c.inkMuted)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.heightIn(min = 48.dp).pressableText { signUp = !signUp; error = null; tried = false }, contentAlignment = Alignment.Center) {
                Text(
                    if (signUp) "Entra" else "Crea tu cuenta",
                    style = t.body.copy(fontWeight = FontWeight.SemiBold),
                    color = c.primary,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * "Continuar con Google" per Google's Sign-In branding: the unaltered G on white, a #747775 outline,
 * #1F1F1F text in the system sans (Roboto). Same 56dp height and radius as our own buttons.
 */
@Composable
private fun GoogleButton(busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(FieldShape)
            .background(Color.White)
            .border(1.dp, Color(0xFF747775), FieldShape)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_google_g), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            if (busy) "Abriendo Google…" else "Continuar con Google",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color(0xFF1F1F1F),
        )
    }
}

/**
 * Boxed field: label above, a 56dp outlined box (3:1 outline, 2dp brand ring on focus, mustard when
 * wrong) and the reason below it. Passwords get a plain-word "Mostrar" toggle, no icon to decode.
 */
@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType,
    error: String?,
    onNext: () -> Unit,
    secret: Boolean = false,
    last: Boolean = false,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    var shown by rememberSaveable { mutableStateOf(false) }
    val ring by animateColorAsState(
        when {
            error != null -> c.stale
            focused -> c.primary
            else -> c.fieldLine
        },
        tween(160, easing = EaseOutStrong),
        label = "ring",
    )
    val text = keyboard == KeyboardType.Text

    Column(Modifier.fillMaxWidth()) {
        Text(label, style = t.secondary.copy(fontWeight = FontWeight.Medium), color = c.ink)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(120)) },
            textStyle = t.body.copy(color = c.ink, fontSize = 17.sp),
            singleLine = true,
            cursorBrush = SolidColor(c.primary),
            interactionSource = source,
            visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
            // Never autocorrect or capitalize an email or a password (the keyboard was suggesting words).
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboard,
                imeAction = if (last) ImeAction.Done else ImeAction.Next,
                autoCorrectEnabled = text,
                capitalization = if (text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (error != null) error(error)
                },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(FieldShape)
                        .background(c.surface)
                        .border(if (focused || error != null) 2.dp else 1.dp, ring, FieldShape)
                        .padding(start = 16.dp, end = if (secret) 4.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = t.body.copy(fontSize = 17.sp), color = c.disabledInk)
                        inner()
                    }
                    if (secret) {
                        Box(
                            Modifier.heightIn(min = 48.dp).pressableText { shown = !shown }.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (shown) "Ocultar" else "Mostrar",
                                style = t.secondary.copy(fontWeight = FontWeight.SemiBold),
                                color = c.primary,
                            )
                        }
                    }
                }
            },
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error, style = t.secondary, color = c.stale)
        }
    }
}
