package pe.aido.cuadre.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pe.aido.cuadre.R
import pe.aido.cuadre.alerts.PaymentAlerts
import pe.aido.cuadre.core.ConfirmationPolicy
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.ui.components.PrimaryButton
import pe.aido.cuadre.ui.components.displayName
import pe.aido.cuadre.ui.hhmmss
import pe.aido.cuadre.ui.soles
import pe.aido.cuadre.ui.theme.Cuadre

/**
 * The anti-fraud moment. Fresh payments: green flood, sound + vibration once.
 * Older than [ConfirmationPolicy.FRESH_WINDOW_MILLIS]: amber flood, no sound, and a plain
 * warning that it must not be used to confirm the customer in front of you.
 */
@Composable
fun ConfirmationScreen(payment: ConfirmedPayment, queued: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = Cuadre.colors
    val t = Cuadre.type
    val e = payment.event
    val now by produceState(System.currentTimeMillis(), payment.id) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val fresh = ConfirmationPolicy.isFresh(e.postedAtMillis, now)

    LaunchedEffect(payment.id) {
        if (ConfirmationPolicy.isFresh(e.postedAtMillis, System.currentTimeMillis())) playConfirmation(context)
    }
    BackHandler(onBack = onDismiss)

    // White status-bar icons over the color flood; restore dark icons on paper afterwards.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = true
            controller?.isAppearanceLightNavigationBars = true
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (fresh) c.paid else c.stale)
            // Swallow taps so nothing underneath can be pressed through the flood.
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding()
            .padding(horizontal = 28.dp, vertical = 32.dp),
    ) {
        Text(
            if (fresh) "✓  Pago recibido" else "◷  Pago anterior",
            style = t.body.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            color = c.onFlood,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${e.wallet.label()} · ${e.postedAtMillis.hhmmss()}",
            style = t.secondary.copy(fontSize = 16.sp),
            color = c.onFloodMuted,
        )

        Spacer(Modifier.weight(1f))

        Text(soles(e.amount), style = t.amountHero, color = c.onFlood)
        Spacer(Modifier.height(8.dp))
        Text(
            e.counterparty?.let(::displayName) ?: "Pagador no visible",
            style = t.title.copy(fontWeight = FontWeight.Normal),
            color = c.onFlood,
        )

        if (!fresh) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Este pago no es de ahora. No lo uses para confirmar al cliente que tienes enfrente.",
                style = t.body.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
                color = c.onFlood,
            )
        }

        e.securityCode?.let { code ->
            Spacer(Modifier.height(32.dp))
            Text("Código de seguridad", style = t.secondary.copy(fontSize = 16.sp), color = c.onFloodMuted)
            Spacer(Modifier.height(4.dp))
            Text(code, style = t.code, color = if (fresh) c.onFlood else c.onFloodMuted)
        }

        Spacer(Modifier.height(16.dp))
        Text(ConfirmationPolicy.ageLabel(e.postedAtMillis, now), style = t.secondary.copy(fontSize = 16.sp), color = c.onFloodMuted)

        Spacer(Modifier.weight(1f))

        if (queued > 0) {
            Text(
                if (queued == 1) "1 pago más" else "$queued pagos más",
                style = t.secondary.copy(fontSize = 16.sp),
                color = c.onFloodMuted,
            )
            Spacer(Modifier.height(12.dp))
        }
        PrimaryButton("Listo", onDismiss, Modifier.fillMaxWidth(), onFlood = true)
    }
}

private fun playConfirmation(context: Context) {
    MediaPlayer.create(context, R.raw.cuadre_pago)?.apply {
        setOnCompletionListener { it.release() }
        start()
    }
    context.getSystemService(Vibrator::class.java)
        ?.vibrate(VibrationEffect.createWaveform(PaymentAlerts.VIBRATION_PATTERN, -1))
}
