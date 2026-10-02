package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pe.aido.cuadre.core.VerificationEngine
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet

class VerificationEngineTest {

    private fun income(amount: Double, at: Long, who: String? = null, wallet: Wallet = Wallet.YAPE) =
        PaymentEvent(wallet, amount, who, PaymentDirection.INCOMING, at, "raw")

    @Test fun confirmsFirstIncoming() {
        val c = VerificationEngine.confirm(income(50.0, 1_000L), emptyList())
        assertNotNull(c)
        assertEquals(50.0, c!!.event.amount, 0.001)
    }

    @Test fun rejectsNonIncome() {
        val other = PaymentEvent(Wallet.YAPE, 50.0, null, PaymentDirection.OTHER, 1_000L, "raw")
        assertNull(VerificationEngine.confirm(other, emptyList()))
    }

    @Test fun dedupesDoublePostedNotification() {
        val first = VerificationEngine.confirm(income(50.0, 1_000L), emptyList())!!
        // same payment re-posted 5s later
        val dup = VerificationEngine.confirm(income(50.0, 6_000L), listOf(first))
        assertNull(dup)
    }

    @Test fun twoDistinctPaymentsSameAmountOutsideWindow() {
        val first = VerificationEngine.confirm(income(50.0, 1_000L), emptyList())!!
        val second = VerificationEngine.confirm(income(50.0, 1_000L + 120_000L), listOf(first))
        assertNotNull(second) // 2 min apart -> genuinely two sales
    }

    @Test fun dailyCloseSumsByWallet() {
        val list = listOf(
            VerificationEngine.confirm(income(50.0, 1_000L), emptyList())!!,
            VerificationEngine.confirm(income(30.0, 200_000L), emptyList())!!,
            VerificationEngine.confirm(income(10.0, 400_000L, wallet = Wallet.PLIN_BBVA), emptyList())!!,
        )
        val close = VerificationEngine.dailyClose(list)
        assertEquals(80.0, close[Wallet.YAPE]!!, 0.001)
        assertEquals(10.0, close[Wallet.PLIN_BBVA]!!, 0.001)
        assertEquals(90.0, VerificationEngine.total(list), 0.001)
    }
}
