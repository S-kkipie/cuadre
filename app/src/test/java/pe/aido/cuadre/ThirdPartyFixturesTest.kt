package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pe.aido.cuadre.core.PaymentParser
import pe.aido.cuadre.domain.Wallet

private const val YAPE = "com.bcp.innovacxion.yapeapp"
private const val BBVA = "com.bbva.nxt_peru"

/**
 * Notification wordings documented by other open-source Yape/Plin listeners. NOT verified on
 * our devices yet: when a real capture contradicts one of these, the real capture wins.
 * Sources: github.com/diamelizsurquillo-cell/yape, github.com/Keny4747/motocaja (2026-10).
 */
class ThirdPartyFixturesTest {

    @Test fun yapeWithMaskedNameAndSecurityCode() {
        val e = PaymentParser.parse(
            YAPE, "Confirmación de Pago", "Lidia Cas* te envió un pago por S/ 1. El cód. de seguridad es: 296", 1L,
        )
        assertNotNull(e)
        assertEquals(1.0, e!!.amount, 0.001)
        assertEquals("Lidia Cas*", e.counterparty)
        assertEquals("296", e.securityCode)
    }

    @Test fun yapeConfirmationPrefixedInBody() {
        val e = PaymentParser.parse(
            YAPE, null, "Confirmación de Pago: Yape! Jeferson Dilas te envió un pago por S/ 1.00", 1L,
        )
        assertEquals("Jeferson Dilas", e!!.counterparty)
        assertNull(e.securityCode)
    }

    @Test fun bbvaQrCharge() {
        val e = PaymentParser.parse(BBVA, null, "Cobro con QR: SANDRO RISSO MORON te hizo un pago de S/ 23.9", 1L)
        assertNotNull(e)
        assertEquals(23.9, e!!.amount, 0.001)
        assertEquals("SANDRO RISSO MORON", e.counterparty)
    }

    @Test fun legacyYapeNameAfterAmount() {
        val a = PaymentParser.parse(YAPE, "Yape", "Te yapeó S/ 10.00 de Juan Perez", 1L)
        assertEquals(10.0, a!!.amount, 0.001)
        assertEquals("Juan Perez", a.counterparty)

        val b = PaymentParser.parse(YAPE, "Yape", "¡Te han yapeado S/ 50.00 de Pedro Ramos!", 1L)
        assertEquals(50.0, b!!.amount, 0.001)
        assertEquals("Pedro Ramos", b.counterparty)

        val c = PaymentParser.parse(YAPE, "Yape", "Te yapeó S/ 5,50 de Pedro.", 1L)
        assertEquals(5.5, c!!.amount, 0.001)
        assertEquals("Pedro", c.counterparty)
    }

    @Test fun brandPrefixDoesNotEatRealNames() {
        assertEquals("Plinio Garcia", PaymentParser.parse(YAPE, null, "Plinio Garcia te yapeó S/ 5", 1L)!!.counterparty)
        assertEquals("Yapeth Rojas", PaymentParser.parse(YAPE, null, "Yape! Yapeth Rojas te envió un pago por S/ 5", 1L)!!.counterparty)
    }

    @Test fun plinIncomingVariants() {
        listOf(
            "Juan Perez te ha plineado S/ 20",
            "Juan Perez te han plineado S/ 20",
            "Juan Perez te plinearon S/ 20",
            "Juan Perez te hizo un Plin de S/ 20",
            "Te llegó un Plin de S/ 20",
            "Te hicieron un Plin de S/ 20",
        ).forEach { text ->
            val e = PaymentParser.parse(BBVA, null, text, 1L)
            assertNotNull("not read as income: $text", e)
            assertEquals(text, 20.0, e!!.amount, 0.001)
        }
    }

    @Test fun plinOutgoingVariantsAreIgnored() {
        listOf(
            "Tu Plin fue enviado: S/ 20 a Juan Perez",
            "Plin enviado S/ 20 a Juan Perez",
            "Realizaste un Plin de S/ 20",
            "Hiciste un Plin de S/ 20 a Juan Perez",
        ).forEach { text -> assertNull("counted as income: $text", PaymentParser.parse(BBVA, null, text, 1L)) }
    }

    @Test fun plinBankPackagesAreWatched() {
        listOf(
            "pe.com.interbank.mobilebanking", "pe.com.interbank.mpay.customer",
            "pe.com.scotiabank.blpm.android.client", "pe.com.banbif.pnappmobile",
            "com.cmac.cajamovilaqp", "com.cajaarq.p51", "com.cmacica.prd",
            "com.cajahuancayo.cajahuancayo.appcajahuancayo", "pe.confianza.cliente",
            "com.alfinbanco.appclientes", "pe.com.tarjetasperuanasprepago.tppapp",
            "com.mibanco.bancamovil", "pe.pichincha.bm",
        ).forEach { assertNotEquals(it, Wallet.UNKNOWN, Wallet.fromPackage(it)) }
    }
}
