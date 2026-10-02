package pe.aido.cuadre.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.aido.cuadre.core.VerificationEngine
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet

/**
 * Glue between the listener and storage. Trust decisions stay in [VerificationEngine];
 * this only loads nearby payments for dedupe and persists what the engine confirms.
 */
class PaymentRepository(private val dao: PaymentDao) {

    // Notifications can arrive back to back; serialize so dedupe sees every prior insert.
    private val lock = Mutex()

    suspend fun onEvent(event: PaymentEvent): ConfirmedPayment? = lock.withLock {
        val window = VerificationEngine.DEDUPE_WINDOW_MILLIS
        val nearby = dao.between(event.postedAtMillis - window, event.postedAtMillis + window)
            .map { it.toConfirmed() }
        val confirmed = VerificationEngine.confirm(event, nearby) ?: return@withLock null
        dao.insert(confirmed.toEntity())
        confirmed
    }

    fun observeBetween(from: Long, to: Long): Flow<List<ConfirmedPayment>> =
        dao.observeBetween(from, to).map { rows -> rows.map { it.toConfirmed() } }

    fun observeAll(): Flow<List<ConfirmedPayment>> =
        dao.observeAll().map { rows -> rows.map { it.toConfirmed() } }

    suspend fun paymentsBetween(from: Long, to: Long): List<ConfirmedPayment> =
        dao.between(from, to).map { it.toConfirmed() }
}

// Only confirmed incoming payments are ever stored, so direction is implied.
private fun PaymentEntity.toConfirmed() = ConfirmedPayment(
    id = id,
    event = PaymentEvent(
        wallet = Wallet.entries.firstOrNull { it.name == wallet } ?: Wallet.UNKNOWN,
        amount = amount,
        counterparty = counterparty,
        direction = PaymentDirection.INCOMING,
        postedAtMillis = postedAtMillis,
        rawText = rawText,
        securityCode = securityCode,
    ),
)

private fun ConfirmedPayment.toEntity() = PaymentEntity(
    id = id,
    wallet = event.wallet.name,
    amount = event.amount,
    counterparty = event.counterparty,
    postedAtMillis = event.postedAtMillis,
    rawText = event.rawText,
    securityCode = event.securityCode,
)
