package pe.aido.cuadre.account

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pe.aido.cuadre.sync.SyncCodec

/**
 * The signed-in account on this phone. The session token is a secret kept in app-private
 * storage; payments never ride on it (they stay local unless the store is linked).
 */
class Account(context: Context) {
    private val sp = context.getSharedPreferences("account", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(read())
    val session: StateFlow<SyncCodec.Session?> = _session

    private fun read(): SyncCodec.Session? {
        val token = sp.getString("token", null) ?: return null
        return SyncCodec.Session(token, sp.getString("name", "").orEmpty(), sp.getString("email", "").orEmpty())
    }

    fun save(s: SyncCodec.Session) {
        sp.edit().putString("token", s.token).putString("name", s.name).putString("email", s.email).apply()
        _session.value = read()
    }

    fun clear() {
        sp.edit().clear().apply()
        _session.value = null
    }
}
