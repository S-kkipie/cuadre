package pe.aido.cuadre.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.aido.cuadre.setup.BatteryCheck
import pe.aido.cuadre.ui.components.CuadreSwitch
import pe.aido.cuadre.ui.components.Hairline
import pe.aido.cuadre.ui.components.ListRow
import pe.aido.cuadre.ui.components.OutlineButton
import pe.aido.cuadre.ui.components.PrimaryButton
import pe.aido.cuadre.ui.components.SidePadding
import pe.aido.cuadre.ui.components.TextLink
import pe.aido.cuadre.ui.theme.Cuadre

data class SetupState(
    val listenerEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val battery: List<BatteryCheck.AppStatus>,
) {
    val restricted get() = battery.filter { !it.unrestricted }

    /** What silently breaks capture, counted once each. */
    val issues get() = listOf(!listenerEnabled, !notificationsEnabled, restricted.isNotEmpty()).count { it }

    /** Notification access is the only hard requirement; the rest degrades, it doesn't block. */
    val canStart get() = listenerEnabled
}

/**
 * First run: "Antes de empezar", with the Play-required prominent disclosure, the three things
 * capture needs, and "Empezar". Day-to-day settings live in [SettingsScreen].
 */
@Composable
fun SetupScreen(
    state: SetupState,
    onOpenListenerSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
    onFixBattery: (String) -> Unit,
    onOpenPrivacy: () -> Unit,
    onStart: () -> Unit,
    linkedStore: String? = null,
) {
    val c = Cuadre.colors
    val t = Cuadre.type
    Column(Modifier.fillMaxSize().padding(horizontal = SidePadding)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(32.dp))
            Text("Antes de empezar", style = t.title, color = c.ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tres permisos para que cada pago real te llegue al instante.",
                style = t.body,
                color = c.inkMuted,
            )

            // Prominent disclosure (Play policy): what is read, and that it stays on the phone.
            Spacer(Modifier.height(32.dp))
            Text("Qué lee Cuadre", style = t.section, color = c.ink)
            Spacer(Modifier.height(12.dp))
            listOf(
                "Solo las notificaciones de pagos que recibes en Yape y Plin.",
                "Nunca entra a tu cuenta ni mueve dinero.",
                // Must stay true: once linked, payments do leave the phone (to the store only).
                if (linkedStore == null) {
                    "Tus pagos se quedan en tu celular. Tu cuenta solo guarda tu nombre y correo."
                } else {
                    "Los pagos se comparten solo con los celulares de $linkedStore."
                },
            ).forEach { line ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("–", style = t.body, color = c.inkMuted)
                    Spacer(Modifier.width(12.dp))
                    Text(line, style = t.body, color = c.ink)
                }
            }
            TextLink("Política de privacidad", onOpenPrivacy)

            Spacer(Modifier.height(16.dp))
            Hairline()
            ListRow("Acceso a notificaciones", secondary = if (state.listenerEnabled) null else "Para leer los avisos de Yape y Plin") {
                if (state.listenerEnabled) Done() else OutlineButton("Permitir", onOpenListenerSettings)
            }
            Hairline()
            ListRow("Avisos con sonido", secondary = if (state.notificationsEnabled) null else "Para avisarte con la app cerrada") {
                if (state.notificationsEnabled) Done() else OutlineButton("Permitir", onRequestNotifications)
            }
            Hairline()
            if (state.restricted.isEmpty()) {
                ListRow("Ahorro de batería") { Done() }
            } else {
                ListRow(
                    "Ahorro de batería",
                    secondary = "Los pagos pueden llegar tarde",
                    secondaryColor = c.stale,
                )
                // One "Arreglar" per app — no duplicate global button.
                state.battery.forEach { app ->
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(app.label, style = t.body, color = c.ink, modifier = Modifier.weight(1f))
                        if (app.unrestricted) Done() else OutlineButton("Arreglar", { onFixBattery(app.packageName) })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Hairline()
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton("Empezar", onStart, enabled = state.canStart, modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun Done() {
    Text("Listo ✓", style = Cuadre.type.body, color = Cuadre.colors.paid)
}
