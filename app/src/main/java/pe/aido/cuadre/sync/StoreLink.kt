package pe.aido.cuadre.sync

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This phone's membership in a store (cuadre-backend). The device token is a long-lived secret
 * kept in app-private storage; revoking the phone in the owner's panel invalidates it.
 */
class StoreLink(context: Context) {
    private val sp = context.getSharedPreferences("store_link", Context.MODE_PRIVATE)

    data class Link(val deviceId: String, val token: String, val storeId: String, val storeName: String, val linkedAt: Long)

    private val _link = MutableStateFlow(read())
    val link: StateFlow<Link?> = _link

    private fun read(): Link? {
        val token = sp.getString("token", null) ?: return null
        return Link(
            deviceId = sp.getString("device_id", "").orEmpty(),
            token = token,
            storeId = sp.getString("store_id", "").orEmpty(),
            storeName = sp.getString("store_name", "").orEmpty(),
            linkedAt = sp.getLong("linked_at", 0L),
        )
    }

    fun save(p: SyncCodec.Paired, linkedAt: Long) {
        sp.edit()
            .putString("device_id", p.deviceId)
            .putString("token", p.token)
            .putString("store_id", p.storeId)
            .putString("store_name", p.storeName)
            .putLong("linked_at", linkedAt)
            .apply()
        _link.value = read()
    }

    fun clear() {
        sp.edit().clear().apply()
        _link.value = null
        // Unlinked: stop being reachable. The token is deleted and Firebase goes dormant again.
        runCatching {
            val fcm = FirebaseMessaging.getInstance()
            fcm.isAutoInitEnabled = false
            fcm.deleteToken()
        }
    }
}
