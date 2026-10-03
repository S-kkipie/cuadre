package pe.aido.cuadre.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import pe.aido.cuadre.ui.components.Hairline
import pe.aido.cuadre.ui.components.PrimaryButton
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.theme.Cuadre

/** What linking with the signed-in account came back with. */
sealed class LinkOutcome {
    data object Done : LinkOutcome()
    /** The account has no store yet: ask its name and try again. */
    data object NeedStoreName : LinkOutcome()
    data class Error(val message: String) : LinkOutcome()
}

/**
 * "Vincular con mi tienda". Signed in: one tap with the account (the owner's own phones). With a
 * 6-digit code from the panel: employees' phones. [onPair] returns an error message, or null.
 */
@Composable
fun PairDialog(
    defaultName: String,
    signedIn: Boolean,
    onLinkOwn: suspend (deviceName: String, storeName: String?) -> LinkOutcome,
    onPair: suspend (code: String, name: String) -> String?,
    onDismiss: () -> Unit,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    var withCode by remember { mutableStateOf(!signedIn) }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(defaultName) }
    var storeName by remember { mutableStateOf("") }
    var needStoreName by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var submit by remember { mutableStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(withCode) { if (withCode) runCatching { focus.requestFocus() } }
    LaunchedEffect(submit) {
        if (submit == 0) return@LaunchedEffect
        busy = true
        if (withCode) {
            error = onPair(code, name.trim())
            busy = false
            if (error == null) onDismiss()
        } else {
            when (val r = onLinkOwn(name.trim(), storeName.takeIf { needStoreName })) {
                LinkOutcome.Done -> onDismiss()
                LinkOutcome.NeedStoreName -> { needStoreName = true; error = null }
                is LinkOutcome.Error -> error = r.message
            }
            busy = false
        }
    }

    val canSubmit = name.isNotBlank() && !busy && when {
        withCode -> code.length == 6
        needStoreName -> storeName.isNotBlank()
        else -> true
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.paper).padding(24.dp)) {
            Text(if (withCode) "Vincular con un código" else "Conectar a tu tienda", style = t.section, color = c.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    withCode -> "En el panel de Cuadre, toca \"Generar código\" y escríbelo aquí."
                    needStoreName -> "Tu cuenta todavía no tiene una tienda. ¿Cómo se llama?"
                    else -> "Este celular se conecta a la tienda de tu cuenta, sin código."
                },
                style = t.body,
                color = c.inkMuted,
            )
            Spacer(Modifier.height(20.dp))
            if (withCode) {
                Text("Código", style = t.secondary, color = c.inkMuted)
                BasicTextField(
                    value = code,
                    onValueChange = { v -> code = v.filter(Char::isDigit).take(6); error = null },
                    textStyle = t.code.copy(color = c.ink, letterSpacing = 0.3.em),
                    singleLine = true,
                    cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).focusRequester(focus),
                )
                Hairline()
                Spacer(Modifier.height(16.dp))
            }
            if (!withCode && needStoreName) {
                Text("Nombre de tu tienda", style = t.secondary, color = c.inkMuted)
                BasicTextField(
                    value = storeName,
                    onValueChange = { storeName = it.take(80); error = null },
                    textStyle = t.rowTitle.copy(color = c.ink),
                    singleLine = true,
                    cursorBrush = SolidColor(c.primary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Hairline()
                Spacer(Modifier.height(16.dp))
            }
            Text("Nombre de este celular", style = t.secondary, color = c.inkMuted)
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                textStyle = t.rowTitle.copy(color = c.ink),
                singleLine = true,
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Hairline()
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = t.body, color = c.stale)
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                when {
                    busy -> "Conectando…"
                    withCode -> "Vincular"
                    else -> "Conectar"
                },
                { submit++ },
                enabled = canSubmit,
            )
            if (signedIn) {
                TextLink(
                    if (withCode) "Conectar con mi cuenta" else "Tengo un código de la tienda",
                    { withCode = !withCode; error = null },
                    muted = true,
                )
            }
            TextLink("Cancelar", onDismiss, muted = true)
        }
    }
}
