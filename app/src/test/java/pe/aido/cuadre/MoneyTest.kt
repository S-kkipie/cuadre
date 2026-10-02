package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.aido.cuadre.core.parseSoles

class MoneyTest {
    @Test fun acceptsWhatCashiersType() {
        assertEquals(2.5, parseSoles("2.50")!!, 0.001)
        assertEquals(2.5, parseSoles("2,50")!!, 0.001)
        assertEquals(15.0, parseSoles("S/ 15")!!, 0.001)
        assertEquals(1250.5, parseSoles("1,250.50")!!, 0.001)
        assertEquals(1250.5, parseSoles("1.250,50")!!, 0.001)
    }

    @Test fun rejectsNonsense() {
        assertNull(parseSoles(""))
        assertNull(parseSoles("abc"))
        assertNull(parseSoles("0"))
        assertNull(parseSoles("-5"))
    }
}
