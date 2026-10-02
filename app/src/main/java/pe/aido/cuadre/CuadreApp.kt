package pe.aido.cuadre

import android.app.Application
import android.content.Context
import androidx.room.Room
import pe.aido.cuadre.alerts.PaymentAlerts
import pe.aido.cuadre.alerts.PaymentVoice
import pe.aido.cuadre.data.Prefs
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.data.CuadreDatabase
import pe.aido.cuadre.data.MIGRATION_1_2
import pe.aido.cuadre.data.MIGRATION_2_3
import pe.aido.cuadre.data.MIGRATION_3_4
import pe.aido.cuadre.data.PaymentRepository
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.sync.StoreLink
import pe.aido.cuadre.sync.SyncCodec
import pe.aido.cuadre.sync.SyncWorker

class CuadreApp : Application() {
    val database: CuadreDatabase by lazy {
        Room.databaseBuilder(this, CuadreDatabase::class.java, "cuadre.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }
    val repository: PaymentRepository by lazy { PaymentRepository(database.paymentDao()) }
    val prefs: Prefs by lazy { Prefs(this) }
    val alerts: PaymentAlerts by lazy { PaymentAlerts(this, prefs, PaymentVoice(this)) }

    override fun onCreate() {
        super.onCreate()
        alerts.createChannel()
    }

    val storeLink: StoreLink by lazy { StoreLink(this) }

    /** Store a parsed event and, if the core confirms it as a new payment, announce and share it. */
    suspend fun capture(event: PaymentEvent): ConfirmedPayment? =
        repository.onEvent(event)?.also {
            alerts.announce(it)
            shareWithStore()
        }

    suspend fun addCash(amount: Double, atMillis: Long): ConfirmedPayment =
        repository.addCash(amount, atMillis).also { shareWithStore() }

    /**
     * A payment another phone of the store confirmed (via push). Stored under the same id, so it
     * is never doubled; announced like a local one (full-screen / notification / voice).
     */
    suspend fun receiveFromStore(remote: SyncCodec.RemotePayment) {
        if (repository.insertRemote(remote.payment)) alerts.announce(remote.payment)
    }

    private fun shareWithStore() {
        if (storeLink.link.value != null) SyncWorker.enqueue(this)
    }
}

val Context.cuadre: CuadreApp
    get() = applicationContext as CuadreApp

val Context.repository: PaymentRepository
    get() = cuadre.repository
