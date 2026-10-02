package pe.aido.cuadre.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug builds only. Simulates a wallet notification from the shell:
 *
 *   adb shell am broadcast -n pe.aido.cuadre/.debug.DebugPaymentReceiver \
 *     --es text "Juan Perez te yapeó S/ 50.00"
 *
 * Optional extras: title (default "Yape"), pkg (default Yape's package). No text = random sample.
 */
class DebugPaymentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val text = intent.getStringExtra("text") ?: DebugPayments.sampleYapeText()
                val result = DebugPayments.simulate(
                    context,
                    text = text,
                    title = intent.getStringExtra("title") ?: "Yape",
                    packageName = intent.getStringExtra("pkg") ?: "com.bcp.innovacxion.yapeapp",
                )
                Log.i(TAG, if (result != null) "stored ${result.id} <- \"$text\"" else "ignored <- \"$text\"")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object { const val TAG = "CuadreDebug" }
}
