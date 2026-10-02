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

/**
 * "Vincular con mi tienda": the owner reads a 6-digit code from the web panel; this phone trades
 * it for its own device token. [onPair] returns an error message, or null on success.
 */
@Composable
fun PairDialog(defaultName: String, onPair: suspend (code: String, name: String) -> String?, onDismiss: () -> Unit) {
    val c = Cuadre.colors
    val t = Cuadre.type
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(defaultName) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var submit by remember { mutableStateOf(0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(submit) {
        if (submit == 0) return@LaunchedEffect
        busy = true
        error = onPair(code, name.trim())
        busy = false
        if (error == null) onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.paper).padding(24.dp)) {
            Text("Vincular con mi tienda", style = t.section, color = c.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "En el panel de Cuadre, toca \"Generar código\" y escríbelo aquí.",
                style = t.body,
                color = c.inkMuted,
            )
            Spacer(Modifier.height(20.dp))
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
                if (busy) "Vinculando…" else "Vincular",
                { submit++ },
                enabled = code.length == 6 && name.isNotBlank() && !busy,
            )
            TextLink("Cancelar", onDismiss, muted = true)
        }
    }
}
