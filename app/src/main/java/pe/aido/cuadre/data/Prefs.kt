package pe.aido.cuadre.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How this phone uses Cuadre, picked on the first screen. */
enum class UseMode {
    /** Alone, no account: everything stays on the phone. */
    LOCAL,
    /** The store owner: signs in and links their phones to their store. */
    OWNER,
    /** An employee's phone: joins the store with the owner's 6-digit code, no account. */
    WORKER,
}

/** Small on-device settings. Exposed as flows so the UI and background services stay in sync. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("cuadre", Context.MODE_PRIVATE)

    private fun bool(key: String, default: Boolean) = MutableStateFlow(sp.getBoolean(key, default))

    private val _tillMode = bool(KEY_TILL_MODE, false)
    private val _onboarded = bool(KEY_ONBOARDED, false)
    private val _voice = bool(KEY_VOICE, true)
    private val _listenerConnected = bool(KEY_LISTENER_CONNECTED, true)

    private val _mode = MutableStateFlow(sp.getString(KEY_MODE, null)?.let { runCatching { UseMode.valueOf(it) }.getOrNull() })

    /** Null until the user picks one on the first screen. */
    val mode: StateFlow<UseMode?> = _mode

    fun setMode(mode: UseMode?) {
        sp.edit().putString(KEY_MODE, mode?.name).apply()
        _mode.value = mode
    }

    val tillMode: StateFlow<Boolean> = _tillMode
    val onboarded: StateFlow<Boolean> = _onboarded
    val voice: StateFlow<Boolean> = _voice

    /** Last known binding state of the notification listener (set by the service itself). */
    val listenerConnected: StateFlow<Boolean> = _listenerConnected

    fun setTillMode(on: Boolean) = put(KEY_TILL_MODE, on, _tillMode)
    fun setOnboarded(on: Boolean) = put(KEY_ONBOARDED, on, _onboarded)
    fun setVoice(on: Boolean) = put(KEY_VOICE, on, _voice)
    fun setListenerConnected(on: Boolean) = put(KEY_LISTENER_CONNECTED, on, _listenerConnected)

    private fun put(key: String, value: Boolean, flow: MutableStateFlow<Boolean>) {
        sp.edit().putBoolean(key, value).apply()
        flow.value = value
    }

    private companion object {
        const val KEY_TILL_MODE = "till_mode"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_VOICE = "voice"
        const val KEY_LISTENER_CONNECTED = "listener_connected"
        const val KEY_MODE = "use_mode"
    }
}
