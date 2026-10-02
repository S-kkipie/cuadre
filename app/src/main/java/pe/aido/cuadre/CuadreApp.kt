package pe.aido.cuadre

import android.app.Application
import android.content.Context
import androidx.room.Room
import pe.aido.cuadre.alerts.PaymentAlerts
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.data.CuadreDatabase
import pe.aido.cuadre.data.MIGRATION_1_2
import pe.aido.cuadre.data.PaymentRepository
import pe.aido.cuadre.domain.PaymentEvent

class CuadreApp : Application() {
    val database: CuadreDatabase by lazy {
        Room.databaseBuilder(this, CuadreDatabase::class.java, "cuadre.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    val repository: PaymentRepository by lazy { PaymentRepository(database.paymentDao()) }
    val alerts: PaymentAlerts by lazy { PaymentAlerts(this) }

    override fun onCreate() {
        super.onCreate()
        alerts.createChannel()
    }

    /** Store a parsed event and, if the core confirms it as a new payment, announce it. */
    suspend fun capture(event: PaymentEvent): ConfirmedPayment? =
        repository.onEvent(event)?.also(alerts::announce)
}

val Context.cuadre: CuadreApp
    get() = applicationContext as CuadreApp

val Context.repository: PaymentRepository
    get() = cuadre.repository
