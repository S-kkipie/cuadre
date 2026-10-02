package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.aido.cuadre.core.ConfirmationPolicy

class ConfirmationPolicyTest {

    @Test fun justArrivedIsFresh() {
        assertTrue(ConfirmationPolicy.isFresh(postedAtMillis = 100_000L, nowMillis = 100_500L))
        assertTrue(ConfirmationPolicy.isFresh(postedAtMillis = 100_000L, nowMillis = 160_000L))
    }

    @Test fun olderThanWindowIsNotFresh() {
        // A payment from minutes ago must never look like the customer in front of you just paid.
        assertFalse(ConfirmationPolicy.isFresh(postedAtMillis = 100_000L, nowMillis = 160_001L))
        assertFalse(ConfirmationPolicy.isFresh(postedAtMillis = 0L, nowMillis = 3_600_000L))
    }

    @Test fun smallClockSkewStillFresh() {
        // postTime can be slightly ahead of our clock; that is not a reason to distrust it.
        assertTrue(ConfirmationPolicy.isFresh(postedAtMillis = 102_000L, nowMillis = 100_000L))
    }

    @Test fun newestPaymentIsShownFirst() {
        fun p(at: Long) = pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment(
            "id$at",
            pe.aido.cuadre.domain.PaymentEvent(
                pe.aido.cuadre.domain.Wallet.YAPE, 1.0, null, pe.aido.cuadre.domain.PaymentDirection.INCOMING, at, "raw",
            ),
        )
        // The customer in front of you paid last; an older queued payment must not hide it.
        assertEquals("id300", ConfirmationPolicy.next(listOf(p(100), p(300), p(200)))?.id)
        assertEquals(null, ConfirmationPolicy.next(emptyList()))
    }

    @Test fun ageLabel() {
        assertEquals("ahora", ConfirmationPolicy.ageLabel(100_000L, 100_400L))
        assertEquals("hace 12 s", ConfirmationPolicy.ageLabel(100_000L, 112_000L))
        assertEquals("hace 3 min", ConfirmationPolicy.ageLabel(0L, 200_000L))
        assertEquals("hace 2 h", ConfirmationPolicy.ageLabel(0L, 7_500_000L))
    }
}
