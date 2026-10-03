package pe.aido.cuadre.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pe.aido.cuadre.data.UseMode
import pe.aido.cuadre.ui.components.CuadreSwitch
import pe.aido.cuadre.ui.components.Hairline
import pe.aido.cuadre.ui.components.ListRow
import pe.aido.cuadre.ui.components.OutlineButton
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.TextLink
import androidx.compose.ui.text.font.FontWeight
import pe.aido.cuadre.ui.theme.Cuadre

/**
 * Ajustes, grouped by what the user came to do: who this phone is (and how to leave), how payments
 * are announced, what keeps capture working, and privacy. Every action is the same outlined button,
 * named for its outcome; status reads as text, never as a button.
 */
@Composable
fun SettingsScreen(
    state: SetupState,
    mode: UseMode,
    storeName: String?,
    accountEmail: String?,
    voiceOn: Boolean,
    onVoiceChange: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onOpenListenerSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
    onFixBattery: (String) -> Unit,
    onOpenPrivacy: () -> Unit,
    onSignOut: () -> Unit,
    /** Deletes the owner's account on the server. Error text, or null when done. */
    onDeleteAccount: suspend () -> String?,
    onLeaveStore: () -> Unit,
    onConnect: () -> Unit,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    var confirm by remember { mutableStateOf<UseMode?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var deleteRun by remember { mutableIntStateOf(0) }
    var deleteBusy by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(deleteRun) {
        if (deleteRun == 0) return@LaunchedEffect
        deleteBusy = true
        deleteError = onDeleteAccount()
        deleteBusy = false
        if (deleteError == null) deleting = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SidePadding),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Ajustes", style = t.title, color = c.ink)

        // --- Who this phone is, and the one way out of it -------------------------------------
        Section("Tu cuenta")
        when (mode) {
            UseMode.OWNER -> {
                Identity(storeName ?: "Tu tienda", listOfNotNull("Dueño", accountEmail).joinToString(" · "))
                OutlineButton("Cerrar sesión", { confirm = UseMode.OWNER }, Modifier.fillMaxWidth())
                Note("Tus pagos se quedan en este celular. Para volver a conectarlo, entra otra vez.")
                Spacer(Modifier.height(8.dp))
                TextLink("Eliminar mi cuenta", { deleting = true; deleteError = null }, muted = true)
            }
            UseMode.WORKER -> {
                Identity(storeName ?: "Tu tienda", "Celular de un trabajador")
                OutlineButton("Salir de la tienda", { confirm = UseMode.WORKER }, Modifier.fillMaxWidth())
                Note("Dejas de recibir los pagos de la tienda. Los que ya tienes se quedan.")
            }
            UseMode.LOCAL -> {
                Identity("Solo en este celular", "Sin cuenta. Tus pagos se quedan aquí.")
                OutlineButton("Conectar con una tienda", onConnect, Modifier.fillMaxWidth())
                Note("Si tienes una tienda o trabajas en una, todos los celulares ven los mismos pagos.")
            }
        }

        // --- How payments are announced -------------------------------------------------------
        Section("Avisos de pago")
        Hairline()
        ListRow("Anunciar con voz", secondary = "Dice el monto, quién pagó y el código") {
            CuadreSwitch(voiceOn, onVoiceChange)
        }
        Hairline()
        ListRow("Escuchar cómo suena") {
            OutlineButton("Probar", onTestVoice)
        }
        Hairline()

        // --- What keeps capture working -------------------------------------------------------
        Section("Para no perder pagos")
        Hairline()
        Requirement(
            "Acceso a notificaciones",
            ok = state.listenerEnabled,
            why = "Sin esto Cuadre no ve los pagos",
            action = "Permitir",
            onAction = onOpenListenerSettings,
        )
        Hairline()
        Requirement(
            "Avisos en pantalla",
            ok = state.notificationsEnabled,
            why = "Para avisarte con la app cerrada",
            action = "Permitir",
            onAction = onRequestNotifications,
        )
        Hairline()
        if (state.restricted.isEmpty()) {
            Requirement("Ahorro de batería", ok = true, why = "", action = "", onAction = {})
        } else {
            state.restricted.forEachIndexed { i, app ->
                if (i > 0) Hairline()
                Requirement(
                    "Batería de ${app.label}",
                    ok = false,
                    why = "Puede retrasar los avisos de pago",
                    action = "Arreglar",
                    onAction = { onFixBattery(app.packageName) },
                )
            }
        }
        Hairline()

        // --- Privacy ------------------------------------------------------------------------
        Section("Privacidad")
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onOpenPrivacy)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Qué lee Cuadre", style = t.rowTitle, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text("Solo los avisos de pagos de Yape y Plin", style = t.secondary, color = c.inkMuted)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.inkMuted)
        }
        Hairline()
        Spacer(Modifier.height(32.dp))
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { if (!deleteBusy) deleting = false },
            containerColor = c.surface,
            title = { Text("¿Eliminar tu cuenta?", style = t.section, color = c.ink) },
            text = {
                Column {
                    Text(
                        "Se borran tu cuenta, tu tienda, los celulares conectados y los pagos guardados en el servidor. " +
                            "No se puede deshacer. Los pagos que este celular ya tiene se quedan aquí.",
                        style = t.body,
                        color = c.ink,
                    )
                    deleteError?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, style = t.body, color = c.stale)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { if (!deleteBusy) deleteRun++ }) {
                    Text(if (deleteBusy) "Eliminando…" else "Eliminar", color = c.stale, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!deleteBusy) deleting = false }) { Text("Cancelar", color = c.inkMuted) }
            },
        )
    }

    confirm?.let { which ->
        val owner = which == UseMode.OWNER
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = c.surface,
            title = { Text(if (owner) "¿Cerrar sesión?" else "¿Salir de la tienda?", style = t.section, color = c.ink) },
            text = {
                Text(
                    if (owner) "Este celular deja de compartir pagos con tu tienda. Los pagos que ya tienes se quedan aquí."
                    else "Este celular deja de recibir los pagos de la tienda. Para volver, necesitas un código nuevo del dueño.",
                    style = t.body,
                    color = c.ink,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirm = null; if (owner) onSignOut() else onLeaveStore() }) {
                    Text(if (owner) "Cerrar sesión" else "Salir de la tienda", color = c.primary)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar", color = c.inkMuted) } },
        )
    }
}

/** Section heading: more space above than below, so it binds to what follows. */
@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(36.dp))
    Text(title, style = Cuadre.type.section, color = Cuadre.colors.ink)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Identity(primary: String, secondary: String) {
    Text(primary, style = Cuadre.type.rowTitle, color = Cuadre.colors.ink)
    Spacer(Modifier.height(4.dp))
    Text(secondary, style = Cuadre.type.secondary, color = Cuadre.colors.inkMuted)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Note(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(text, style = Cuadre.type.secondary, color = Cuadre.colors.inkMuted)
}

/** A thing capture needs: "Listo" in green when fine; otherwise why it matters and one button. */
@Composable
private fun Requirement(title: String, ok: Boolean, why: String, action: String, onAction: () -> Unit) {
    val c = Cuadre.colors
    ListRow(title, secondary = if (ok) null else why, secondaryColor = c.stale) {
        if (ok) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDotSmall()
                Spacer(Modifier.width(8.dp))
                Text("Listo", style = Cuadre.type.body, color = c.paid)
            }
        } else {
            OutlineButton(action, onAction)
        }
    }
}

@Composable
private fun StatusDotSmall() {
    pe.aido.cuadre.ui.components.StatusDot(Cuadre.colors.paid, Modifier.size(8.dp))
}
