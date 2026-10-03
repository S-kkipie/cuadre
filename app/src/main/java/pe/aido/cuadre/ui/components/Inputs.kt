package pe.aido.cuadre.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.aido.cuadre.ui.theme.Cuadre

/** Every field and boxed surface shares the buttons' 12dp radius. */
val FieldShape = RoundedCornerShape(12.dp)

/** Outline of a field: 3:1 at rest, 2dp brand ring when focused, 2dp mustard when wrong. */
@Composable
private fun ringColor(focused: Boolean, error: Boolean): Color {
    val c = Cuadre.colors
    val ring by animateColorAsState(
        when {
            error -> c.stale
            focused -> c.primary
            else -> c.fieldLine
        },
        tween(160, easing = EaseOutStrong),
        label = "ring",
    )
    return ring
}

private fun Modifier.fieldBox(ring: Color, strong: Boolean, height: Dp = 56.dp): Modifier = this
    .fillMaxWidth()
    .height(height)
    .clip(FieldShape)
    .background(Color.White)
    .border(if (strong) 2.dp else 1.dp, ring, FieldShape)

@Composable
private fun FieldLabel(label: String) {
    Text(label, style = Cuadre.type.secondary.copy(fontWeight = FontWeight.Medium), color = Cuadre.colors.ink)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun FieldError(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(6.dp))
        Text(error, style = Cuadre.type.secondary, color = Cuadre.colors.stale)
    }
}

/**
 * Boxed text field: label above, a 56dp outlined box, the reason below when wrong. Passwords get a
 * plain-word "Mostrar" toggle, no icon to decode. Emails and passwords never autocorrect.
 */
@Composable
fun TextInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    error: String? = null,
    secret: Boolean = false,
    last: Boolean = false,
    maxLength: Int = 120,
    onNext: () -> Unit = {},
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    var shown by rememberSaveable { mutableStateOf(false) }
    val ring = ringColor(focused, error != null)
    val words = keyboard == KeyboardType.Text

    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(maxLength)) },
            textStyle = t.body.copy(color = c.ink, fontSize = 17.sp),
            singleLine = true,
            cursorBrush = SolidColor(c.primary),
            interactionSource = source,
            visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboard,
                imeAction = if (last) ImeAction.Done else ImeAction.Next,
                autoCorrectEnabled = words,
                capitalization = if (words) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            ),
            keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = label
                if (error != null) error(error)
            },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fieldBox(ring, focused || error != null)
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
        FieldError(error)
    }
}

/** Money field: the same box, "S/" inside it, big tabular digits, decimal keyboard. */
@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    error: String? = null,
) {
    val c = Cuadre.colors
    val digits = Cuadre.type.amountTotal.copy(fontSize = 28.sp, lineHeight = 34.sp)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val ring = ringColor(focused, error != null)
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = { v -> onValueChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(10)) },
            textStyle = digits.copy(color = c.ink),
            singleLine = true,
            cursorBrush = SolidColor(c.primary),
            interactionSource = source,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { inner ->
                Row(
                    Modifier.fieldBox(ring, focused || error != null, height = 64.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("S/", style = digits.copy(fontSize = 20.sp), color = c.inkMuted)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text("0.00", style = digits, color = c.disabledInk.copy(alpha = 0.6f))
                        inner()
                    }
                }
            },
        )
        FieldError(error)
    }
}

/**
 * Six separate digit cells for a pairing code. One hidden field drives them, so paste and
 * backspace work as usual; the cell being typed carries the focus ring.
 */
@Composable
fun CodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    length: Int = 6,
    error: String? = null,
) {
    val c = Cuadre.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(Modifier.fillMaxWidth()) {
        FieldLabel(label)
        BasicTextField(
            value = value,
            onValueChange = { v -> onValueChange(v.filter(Char::isDigit).take(length)) },
            singleLine = true,
            interactionSource = source,
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = Cuadre.type.body.copy(color = Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = label
                if (error != null) error(error)
            },
            decorationBox = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(length) { i ->
                        val current = focused && (i == value.length || (i == length - 1 && value.length == length))
                        val ring = ringColor(current, error != null)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(FieldShape)
                                .background(Color.White)
                                .border(if (current || error != null) 2.dp else 1.dp, ring, FieldShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                value.getOrNull(i)?.toString() ?: "",
                                style = Cuadre.type.amountTotal.copy(fontSize = 28.sp, lineHeight = 34.sp),
                                color = c.ink,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            },
        )
        FieldError(error)
    }
}

/** "← Volver": the one way back on every pushed screen, a full 48dp target. */
@Composable
fun BackLink(label: String = "Volver", onClick: () -> Unit) {
    val c = Cuadre.colors
    Row(
        Modifier.heightIn(min = 48.dp).pressableText(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = c.inkMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = Cuadre.type.body, color = c.inkMuted)
    }
}

/** A message the user must read (server error, warning): tinted box, not loose colored text. */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier, color: Color = Cuadre.colors.stale) {
    Text(
        text,
        style = Cuadre.type.body,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Full-width 56dp outlined action, the secondary partner of [PrimaryButton]. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Cuadre.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(FieldShape)
            .background(Color.White)
            .border(1.dp, if (enabled) c.primary else c.hairline, FieldShape),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Cuadre.type.button, color = if (enabled) c.primary else c.disabledInk) }
}
