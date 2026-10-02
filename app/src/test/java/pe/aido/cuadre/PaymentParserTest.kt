package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.aido.cuadre.core.PaymentParser
import pe.aido.cuadre.domain.PaymentDirection
import pe.aido.cuadre.domain.Wallet

private const val YAPE = "com.bcp.innovacxp.yapeapp"

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

    @Test fun incomingWithoutPayerStillCountsIncome() {
        val e = PaymentParser.parse(YAPE, "Yape", "Te yapearon S/ 12.50", 1_000L)
        assertNotNull(e)
        assertEquals(12.50, e!!.amount, 0.001)
        assertNull(e.counterparty) // payer unknown, but income is still usable
        assertTrue(e.isUsableIncome)
    }
}
