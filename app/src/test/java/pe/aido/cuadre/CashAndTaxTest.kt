package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.aido.cuadre.core.CashClose
import pe.aido.cuadre.core.IncomeCsv
import pe.aido.cuadre.core.NuevoRus
import pe.aido.cuadre.core.VerificationEngine.ConfirmedPayment
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.PaymentEvent
import pe.aido.cuadre.domain.Wallet
import java.time.LocalDateTime
import java.time.ZoneId

private val lima = ZoneId.of("America/Lima")

private fun pay(wallet: Wallet, amount: Double, at: String = "2026-10-02T10:00", who: String? = null, code: String? = null) =
    ConfirmedPayment(
        "${wallet.name}:$amount:$at",
        PaymentEvent(wallet, amount, who, PaymentDirection.INCOMING,
            LocalDateTime.parse(at).atZone(lima).toInstant().toEpochMilli(), "raw", code),
    )

class CashCloseTest {

    @Test fun expectedCashIsOpeningPlusCashSales() {
        val day = listOf(pay(Wallet.YAPE, 50.0), pay(Wallet.EFECTIVO, 20.0), pay(Wallet.EFECTIVO, 7.5), pay(Wallet.PLIN_BBVA, 10.0))
        val c = CashClose.compute(day, openingCash = 100.0, countedCash = 120.0)
        assertEquals(27.5, c.cashSales, 0.001)
        assertEquals(127.5, c.expectedCash, 0.001)
        assertEquals(-7.5, c.difference, 0.001)          // missing S/ 7.50
        assertEquals(60.0, c.digitalTotal, 0.001)
        assertEquals(87.5, c.salesTotal, 0.001)          // all sales, opening float excluded
    }

    @Test fun exactAndOver() {
        val day = listOf(pay(Wallet.EFECTIVO, 10.0))
        assertEquals(0.0, CashClose.compute(day, 0.0, 10.0).difference, 0.001)
        assertEquals(2.0, CashClose.compute(day, 0.0, 12.0).difference, 0.001)
    }

    @Test fun shareTextReadsLikeAReceipt() {
        val day = listOf(pay(Wallet.YAPE, 50.0), pay(Wallet.EFECTIVO, 20.0))
        val text = CashClose.shareText("vie 2 oct", CashClose.compute(day, 0.0, 15.0))
        assertTrue(text.contains("Cuadre · vie 2 oct"))
        assertTrue(text.contains("Yape: S/ 50.00"))
        assertTrue(text.contains("Efectivo: S/ 20.00"))
        assertTrue(text.contains("Total vendido: S/ 70.00"))
        assertTrue(text.contains("Faltan S/ 5.00"))
    }
}

class NuevoRusTest {

    @Test fun category1UpTo5000() {
        val s = NuevoRus.status(monthIncome = 4300.0, yearIncome = 30000.0)
        assertEquals(1, s.category)
        assertEquals(5000.0, s.categoryLimit, 0.001)
        assertTrue(s.nearLimit)                             // >= 80% of the category limit
        assertFalse(s.overMonthly)
    }

    @Test fun category2Between5000And8000() {
        val s = NuevoRus.status(6000.0, 30000.0)
        assertEquals(2, s.category)
        assertEquals(8000.0, s.categoryLimit, 0.001)
        assertFalse(s.nearLimit)
    }

    @Test fun overTheRegimeLimits() {
        assertTrue(NuevoRus.status(8500.0, 30000.0).overMonthly)
        assertTrue(NuevoRus.status(1000.0, 97000.0).overAnnual)
        assertTrue(NuevoRus.status(1000.0, 80000.0).nearAnnual)
    }
}

class IncomeCsvTest {

    @Test fun csvHasHeaderAndOneLinePerPaymentOldestFirst() {
        val csv = IncomeCsv.build(
            listOf(
                pay(Wallet.YAPE, 25.0, "2026-10-02T14:32", "ROSA HUAMAN", "418"),
                pay(Wallet.EFECTIVO, 1250.5, "2026-10-01T09:05"),
            ),
            lima,
        ).lines()
        assertEquals("fecha,hora,medio,pagador,monto,codigo", csv[0])
        assertEquals("2026-10-01,09:05,Efectivo,,1250.50,", csv[1])
        assertEquals("2026-10-02,14:32,Yape,Rosa Huaman,25.00,418", csv[2])
    }

    @Test fun csvEscapesCommasInNames() {
        val csv = IncomeCsv.build(listOf(pay(Wallet.YAPE, 5.0, who = "PEREZ, JUAN")), lima).lines()
        assertEquals("2026-10-02,10:00,Yape,\"Perez, Juan\",5.00,", csv[1])
    }
}
