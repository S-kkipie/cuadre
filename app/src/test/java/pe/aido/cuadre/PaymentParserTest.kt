package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.aido.cuadre.core.PaymentParser
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.Wallet

private const val YAPE = "com.bcp.innovacxion.yapeapp"

class PaymentParserTest {

    @Test fun parsesIncomingWithAmountAndPayer() {
        val e = PaymentParser.parse(YAPE, "Yape", "Juan Perez te yapeó S/ 50.00", 1_000L)
        assertNotNull(e)
        assertEquals(Wallet.YAPE, e!!.wallet)
        assertEquals(50.0, e.amount, 0.001)
        assertEquals(PaymentDirection.INCOMING, e.direction)
        assertTrue(e.isUsableIncome)
        assertEquals("Juan Perez", e.counterparty)
    }

    @Test fun ignoresOutgoing() {
        val e = PaymentParser.parse(YAPE, "Yape", "Yapeaste S/ 20 a Maria", 1_000L)
        assertNull(e)
    }

    @Test fun unknownPackageIsIgnored() {
        val e = PaymentParser.parse("com.whatsapp", "msg", "te yapearon S/ 10", 1_000L)
        assertNull(e)
    }

    @Test fun unreadableAmountReturnsNullNotGuess() {
        val e = PaymentParser.parse(YAPE, "Yape", "Te yapearon un monto", 1_000L)
        assertNull(e)
    }

    @Test fun handlesThousandsSeparator() {
        assertEquals(1250.50, PaymentParser.extractAmount("S/ 1,250.50")!!, 0.001)
        assertEquals(1250.50, PaymentParser.extractAmount("S/1.250,50")!!, 0.001)
        assertEquals(8.0, PaymentParser.extractAmount("S/8")!!, 0.001)
    }

    // Real capture, Yape on Android 17 (2026-10-02).
    @Test fun realYapeIncoming() {
        val e = PaymentParser.parse(
            YAPE, "Confirmación de Pago", "Yape! ADRIAN MAMANI te envió un pago por S/ 1", 1_000L,
        )
        assertNotNull(e)
        assertEquals(1.0, e!!.amount, 0.001)
        assertEquals("ADRIAN MAMANI", e.counterparty)
        assertTrue(e.isUsableIncome)
    }

    // Real capture: Yape marketing push arriving in the same burst as payments (2026-10-02).
    @Test fun realYapeMarketingIsIgnored() {
        assertNull(
            PaymentParser.parse(
                YAPE, "¿Sin dinero en tu cuenta Yape?",
                "¡Pide un crédito en minutos! Entra al menú >> Sección \"Créditos\" y solicítalo hoy mismo. " +
                    "Sujeto a evaluación crediticia.",
                1_000L,
            ),
        )
    }

    // Real capture, BBVA Plin incoming (2026-10-02).
    @Test fun realBbvaPlinIncoming() {
        val e = PaymentParser.parse(
            "com.bbva.nxt_peru", "¡Recibiste un Plin! 💸", "ADRIAN ISSAC MAMANI te plineó S/1 .", 1_000L,
        )
        assertNotNull(e)
        assertEquals(Wallet.PLIN_BBVA, e!!.wallet)
        assertEquals(1.0, e.amount, 0.001)
        assertEquals("ADRIAN ISSAC MAMANI", e.counterparty)
        // The body alone must read as incoming, not only thanks to the title.
        assertNotNull(PaymentParser.parse("com.bbva.nxt_peru", null, "ADRIAN ISSAC MAMANI te plineó S/1 .", 1_000L))
    }

    // Real capture, BBVA Plin outgoing (2026-10-02). Must never count as income.
    @Test fun realBbvaPlinOutgoingIsIgnored() {
        val e = PaymentParser.parse(
            "com.bbva.nxt_peru", "¡Enviaste un Plin!", "Plineaste S/1 a Adrian I Mamani Q .", 1_000L,
        )
        assertNull(e)
        // Even without the title, the body alone must read as outgoing.
        assertNull(PaymentParser.parse("com.bbva.nxt_peru", null, "Plineaste S/1 a Adrian I Mamani Q .", 1_000L))
    }

    @Test fun incomingWithoutPayerStillCountsIncome() {
        val e = PaymentParser.parse(YAPE, "Yape", "Te yapearon S/ 12.50", 1_000L)
        assertNotNull(e)
        assertEquals(12.50, e!!.amount, 0.001)
        assertNull(e.counterparty) // payer unknown, but income is still usable
        assertTrue(e.isUsableIncome)
    }
}
