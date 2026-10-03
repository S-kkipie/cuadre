package pe.aido.cuadre.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import pe.aido.cuadre.R
import pe.aido.cuadre.data.UseMode
import pe.aido.cuadre.ui.components.BackLink
import pe.aido.cuadre.ui.components.CodeInput
import pe.aido.cuadre.ui.components.LiveDot
import pe.aido.cuadre.ui.components.Notice
import pe.aido.cuadre.ui.components.TextInput
import androidx.compose.ui.platform.LocalFocusManager
import pe.aido.cuadre.ui.components.PrimaryButton
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.pressable
import pe.aido.cuadre.ui.theme.Cuadre

/** Wordmark row shared by the onboarding screens. */
@Composable
internal fun BrandRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(44.dp))
        Spacer(Modifier.width(4.dp))
        Text("Cuadre", style = Cuadre.type.wordmark, color = Cuadre.colors.primary)
    }
}

/**
 * First screen: how this phone uses Cuadre. Alone needs no account; the owner signs in;
 * an employee only types the owner's code.
 */
@Composable
fun WelcomeScreen(onPick: (UseMode) -> Unit) {
    val c = Cuadre.colors
    val t = Cuadre.type
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SidePadding),
    ) {
        Spacer(Modifier.height(32.dp))
        BrandRow()
        Spacer(Modifier.height(24.dp))
        Text("¿Cómo vas a usar Cuadre?", style = t.title, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text("Puedes cambiarlo después en Ajustes.", style = t.body, color = c.inkMuted)
        Spacer(Modifier.height(24.dp))
        RoleCard(
            R.drawable.rol_local,
            "Solo en este celular",
            "Sin cuenta. Tus pagos se quedan aquí.",
        ) { onPick(UseMode.LOCAL) }
        Spacer(Modifier.height(16.dp))
        RoleCard(
            R.drawable.rol_dueno,
            "Soy dueño de una tienda",
            "Entra con tu cuenta y conecta los celulares de tu tienda.",
        ) { onPick(UseMode.OWNER) }
        Spacer(Modifier.height(16.dp))
        RoleCard(
            R.drawable.rol_trabajador,
            "Trabajo en una tienda",
            "Usa el código que te da el dueño. No necesitas cuenta.",
        ) { onPick(UseMode.WORKER) }
        Spacer(Modifier.height(32.dp))
    }
}

/** A photo on top, the choice below. The whole card is one tap target. */
@Composable
private fun RoleCard(@DrawableRes image: Int, title: String, line: String, onClick: () -> Unit) {
    val c = Cuadre.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(shape)
            .border(1.dp, c.hairline, shape)
            .background(c.surface),
    ) {
        Image(
            painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 7f),
        )
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(title, style = Cuadre.type.section, color = c.ink)
            Spacer(Modifier.height(4.dp))
            Text(line, style = Cuadre.type.secondary, color = c.inkMuted)
        }
    }
}

/** Employee phone: the owner's 6-digit code and a name for this phone. No account. */
@Composable
fun WorkerJoinScreen(
    defaultName: String,
    available: Boolean,
    onJoin: suspend (code: String, name: String) -> String?,
    onBack: () -> Unit,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    val focus = LocalFocusManager.current
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf(defaultName) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var tried by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var run by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(run) {
        if (run == 0) return@LaunchedEffect
        busy = true
        error = onJoin(code, name.trim())
        busy = false
    }
    val submit = {
        tried = true
        error = null
        if (code.length == 6 && name.isNotBlank() && !busy) {
            focus.clearFocus()
            run++
        }
    }

    OnboardingColumn {
        BackLink(onClick = onBack)
        Spacer(Modifier.height(16.dp))
        Text("Únete a tu tienda", style = t.title, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pídele al dueño el código de 6 dígitos. Lo ve en el panel de Cuadre, en cuadre.aido.lat.",
            style = t.body,
            color = c.inkMuted,
        )
        if (!available) {
            Spacer(Modifier.height(20.dp))
            Notice("Esta versión de Cuadre no puede conectarse a una tienda.")
        }
        Spacer(Modifier.height(32.dp))
        CodeInput(
            code,
            { code = it; error = null; if (it.length == 6) focus.clearFocus() },
            "Código de la tienda",
            error = if (tried && code.length < 6) "Faltan ${6 - code.length} dígitos" else null,
        )
        Spacer(Modifier.height(24.dp))
        TextInput(
            label = "Nombre de este celular",
            value = name,
            onChange = { name = it; error = null },
            placeholder = "Ej. Caja 1",
            error = if (tried && name.isBlank()) "Ponle un nombre para reconocerlo" else null,
            last = true,
            maxLength = 60,
            onNext = submit,
        )
        Spacer(Modifier.height(6.dp))
        Text("Así lo verá el dueño en su lista de celulares.", style = t.secondary, color = c.inkMuted)
        error?.let {
            Spacer(Modifier.height(20.dp))
            Notice(it)
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(if (busy) "Conectando…" else "Unirme a la tienda", submit, enabled = available && !busy)
    }
}

/**
 * Owner, signed in, phone not linked yet: link it to their store right away. Only when the
 * account has no store does this ask for its name.
 */
@Composable
fun OwnerStoreScreen(
    email: String,
    defaultName: String,
    onLink: suspend (deviceName: String, storeName: String?) -> LinkOutcome,
    onSignOut: () -> Unit,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    val focus = LocalFocusManager.current
    var storeName by rememberSaveable { mutableStateOf("") }
    var needName by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var tried by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(true) }
    // 1 = try linking right away (most owners already made their store on the web).
    var run by rememberSaveable { mutableIntStateOf(1) }
    LaunchedEffect(run) {
        busy = true
        when (val r = onLink(defaultName, storeName.trim().takeIf { needName })) {
            LinkOutcome.Done -> Unit
            LinkOutcome.NeedStoreName -> { needName = true; error = null }
            is LinkOutcome.Error -> error = r.message
        }
        busy = false
    }
    val submit = {
        tried = true
        if (!needName || storeName.isNotBlank()) {
            focus.clearFocus()
            run++
        }
    }

    OnboardingColumn {
        BackLink(onClick = onSignOut)
        Spacer(Modifier.height(16.dp))
        Text(
            when {
                needName -> "¿Cómo se llama tu tienda?"
                error != null -> "No pudimos conectar tu celular"
                else -> "Conectando tu celular…"
            },
            style = t.title,
            color = c.ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                needName -> "Es el nombre que verán los celulares de tu tienda. Puedes cambiarlo en la web."
                error != null -> "Revisa tu internet e inténtalo otra vez."
                else -> "Entraste como $email. Lo estamos uniendo a tu tienda."
            },
            style = t.body,
            color = c.inkMuted,
        )
        if (busy && !needName) {
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(c.primary, live = true)
                Spacer(Modifier.width(8.dp))
                Text("Conectando con cuadre.aido.lat", style = t.secondary, color = c.inkMuted)
            }
        }
        if (needName) {
            Spacer(Modifier.height(32.dp))
            TextInput(
                label = "Nombre de tu tienda",
                value = storeName,
                onChange = { storeName = it; error = null },
                placeholder = "Ej. Bodega Rosa",
                error = if (tried && storeName.isBlank()) "Escribe el nombre de tu tienda" else null,
                last = true,
                maxLength = 80,
                onNext = submit,
            )
        }
        error?.let {
            Spacer(Modifier.height(20.dp))
            Notice(it)
        }
        if (needName || error != null) {
            Spacer(Modifier.height(28.dp))
            PrimaryButton(
                when {
                    busy -> "Conectando…"
                    needName -> "Crear mi tienda"
                    else -> "Reintentar"
                },
                submit,
                enabled = !busy,
            )
        }
    }
}

@Composable
private fun OnboardingColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SidePadding),
    ) {
        Spacer(Modifier.height(8.dp))
        content()
        Spacer(Modifier.height(32.dp))
    }
}
