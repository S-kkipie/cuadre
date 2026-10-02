package pe.aido.cuadre.setup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import pe.aido.cuadre.domain.Wallet

/**
 * Battery savers (Samsung's "Freecess" especially) freeze Yape/bank apps in the background.
 * A frozen wallet app does not process its push, so its notification — our only source of
 * truth — arrives late or not at all. Real capture 2026-10-02: Yape and BBVA notifications
 * were held while frozen and all delivered at once when Samsung unfroze them.
 *
 * No app can change another app's battery setting, so we detect and guide the user to it.
 */
object BatteryCheck {

    data class AppStatus(val packageName: String, val label: String, val unrestricted: Boolean)

    /** Cuadre plus every installed wallet app we listen to. */
    fun statuses(context: Context): List<AppStatus> {
        val pm = context.packageManager
        val power = context.getSystemService(PowerManager::class.java)
        val packages = listOf(context.packageName) + Wallet.entries.flatMap { it.packages }
        return packages.distinct().mapNotNull { pkg ->
            val info = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                return@mapNotNull null
            }
            AppStatus(pkg, pm.getApplicationLabel(info).toString(), power.isIgnoringBatteryOptimizations(pkg))
        }
    }

    /** The app's own settings page, where "Batería → Sin restricciones" lives. */
    fun openAppSettings(context: Context, packageName: String) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
