package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.aido.cuadre.core.DailyHistory
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet
import java.time.LocalDate
import java.time.ZoneId

class DailyHistoryTest {

    private val lima = ZoneId.of("America/Lima")

    private fun paid(amount: Double, at: String) = ConfirmedPayment(
        id = "$amount@$at",
        event = PaymentEvent(
            Wallet.YAPE, amount, null, PaymentDirection.INCOMING,
            java.time.LocalDateTime.parse(at).atZone(lima).toInstant().toEpochMilli(), "raw",
        ),
    )

    @Test fun groupsByLocalDayNewestFirst() {
        val days = DailyHistory.byDay(
            listOf(
                paid(10.0, "2026-10-01T09:00"),
                paid(5.5, "2026-10-01T23:59"),
                paid(20.0, "2026-10-02T00:01"),
            ),
            lima,
        )
        assertEquals(listOf(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)), days.map { it.date })
        assertEquals(20.0, days[0].total, 0.001)
        assertEquals(1, days[0].count)
        assertEquals(15.5, days[1].total, 0.001)
        assertEquals(2, days[1].count)
    }

    @Test fun emptyHistory() {
        assertEquals(emptyList<DailyHistory.Day>(), DailyHistory.byDay(emptyList(), lima))
    }
}
