package pe.aido.cuadre

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pe.aido.cuadre.data.PaymentDao
import pe.aido.cuadre.data.PaymentEntity
import pe.aido.cuadre.data.PaymentRepository
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet

/** In-memory DAO with the same IGNORE-on-conflict semantics as Room. */
private class FakePaymentDao : PaymentDao {
    val rows = MutableStateFlow<List<PaymentEntity>>(emptyList())

    override suspend fun insert(payment: PaymentEntity): Long {
        if (rows.value.any { it.id == payment.id }) return -1
        rows.value = rows.value + payment
        return rows.value.size.toLong()
    }

    private fun inRange(from: Long, to: Long) =
        rows.value.filter { it.postedAtMillis in from..to }.sortedByDescending { it.postedAtMillis }

    override suspend fun between(from: Long, to: Long) = inRange(from, to)
    override suspend fun totalBetween(from: Long, to: Long) = inRange(from, to).sumOf { it.amount }
    override fun observeBetween(from: Long, to: Long): Flow<List<PaymentEntity>> =
        rows.map { inRange(from, to) }
    override fun observeAll(): Flow<List<PaymentEntity>> =
        rows.map { it.sortedByDescending { p -> p.postedAtMillis } }
    override suspend fun deleteCash(id: String): Int {
        val before = rows.value.size
        rows.value = rows.value.filterNot { it.id == id && it.wallet == "EFECTIVO" }
        return before - rows.value.size
    }
}

class PaymentRepositoryTest {

    private fun income(amount: Double, at: Long, who: String? = "Juan Perez", wallet: Wallet = Wallet.YAPE) =
        PaymentEvent(wallet, amount, who, PaymentDirection.INCOMING, at, "raw")

    @Test fun storesConfirmedIncome() = runTest {
        val dao = FakePaymentDao()
        val repo = PaymentRepository(dao)
        assertNotNull(repo.onEvent(income(50.0, 1_000L)))
        assertEquals(1, dao.rows.value.size)
        assertEquals("YAPE", dao.rows.value.single().wallet)
        assertEquals("Juan Perez", dao.rows.value.single().counterparty)
    }

    @Test fun repostedNotificationIsNotCountedTwice() = runTest {
        val dao = FakePaymentDao()
        val repo = PaymentRepository(dao)
        repo.onEvent(income(50.0, 1_000L))
        assertNull(repo.onEvent(income(50.0, 1_500L))) // same payment, re-posted
        assertEquals(1, dao.rows.value.size)
    }

    @Test fun differentPaymentsAreBothStored() = runTest {
        val dao = FakePaymentDao()
        val repo = PaymentRepository(dao)
        repo.onEvent(income(50.0, 1_000L))
        repo.onEvent(income(20.0, 1_500L))
        repo.onEvent(income(50.0, 1_000L + 120_000L)) // same amount, outside dedupe window
        assertEquals(3, dao.rows.value.size)
    }

    @Test fun nonIncomeIsIgnored() = runTest {
        val dao = FakePaymentDao()
        val repo = PaymentRepository(dao)
        val other = PaymentEvent(Wallet.YAPE, 50.0, null, PaymentDirection.OTHER, 1_000L, "raw")
        assertNull(repo.onEvent(other))
        assertEquals(0, dao.rows.value.size)
    }

    @Test fun identicalCashSalesAreNotDeduplicated() = runTest {
        // Two S/ 2.50 cash sales a few seconds apart are two sales, unlike a re-posted notification.
        val dao = FakePaymentDao()
        val repo = PaymentRepository(dao)
        repo.addCash(2.5, 1_000L)
        repo.addCash(2.5, 1_500L)
        assertEquals(2, dao.rows.value.size)
        assertEquals(Wallet.EFECTIVO, repo.paymentsBetween(0L, 10_000L).first().event.wallet)
    }

    @Test fun onlyCashCanBeDeleted() = runTest {
        val dao = FakePaymentDao()
        val repo = PaymentRepository(dao)
        val yape = repo.onEvent(income(50.0, 1_000L))!!
        val cash = repo.addCash(5.0, 2_000L)
        assertEquals(false, repo.deleteCash(yape.id))
        assertEquals(true, repo.deleteCash(cash.id))
        assertEquals(listOf(yape.id), dao.rows.value.map { it.id })
    }

    @Test fun securityCodeRoundTrips() = runTest {
        val repo = PaymentRepository(FakePaymentDao())
        repo.onEvent(income(1.0, 1_000L).copy(securityCode = "296"))
        assertEquals("296", repo.paymentsBetween(0L, 10_000L).single().event.securityCode)
    }

    @Test fun observeMapsBackToConfirmedPayments() = runTest {
        val dao = FakePaymentDao()
        val repo = PaymentRepository(dao)
        repo.onEvent(income(12.5, 1_000L, who = null))
        val stored = repo.paymentsBetween(0L, 10_000L)
        assertEquals(Wallet.YAPE, stored.single().event.wallet)
        assertEquals(12.5, stored.single().event.amount, 0.001)
        assertNull(stored.single().event.counterparty)
    }
}
