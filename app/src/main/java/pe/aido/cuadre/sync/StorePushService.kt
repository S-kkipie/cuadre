package pe.aido.cuadre.sync

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import pe.aido.cuadre.cuadre

/**
 * Receives payments confirmed by the store's other phones (data-only FCM messages) and hands
 * them to [pe.aido.cuadre.CuadreApp.receiveFromStore]: stored once, announced like a local one.
 */
class StorePushService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val app = applicationContext.cuadre
        // A push for a store this phone already left is ignored.
        if (app.storeLink.link.value == null) return
        val remote = SyncCodec.fromPush(message.data) ?: return
        // onMessageReceived runs off the main thread and must finish quickly; one Room insert does.
        runBlocking { app.receiveFromStore(remote) }
    }

    override fun onNewToken(token: String) {
        if (applicationContext.cuadre.storeLink.link.value != null) PushRegisterWorker.enqueue(applicationContext)
    }
}
