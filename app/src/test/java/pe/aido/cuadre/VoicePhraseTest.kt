package pe.aido.cuadre

import org.junit.Assert.assertEquals
import org.junit.Test
import pe.aido.cuadre.alerts.VoicePhrase

class VoicePhraseTest {

    @Test fun fullPayment() {
        assertEquals(
            "Yape. 25 soles. De Rosa Huamán. Código 4, 1, 8.",
            VoicePhrase.build("Yape", 25.0, "Rosa Huamán", "418"),
        )
    }

    @Test fun centsAndNoPayer() {
        assertEquals("Plin. 14 soles con 50 céntimos.", VoicePhrase.build("Plin", 14.5, null, null))
    }

    @Test fun singularSolAndOnlyCents() {
        assertEquals("Yape. 1 sol.", VoicePhrase.build("Yape", 1.0, null, null))
        assertEquals("Yape. 1 sol con 5 céntimos.", VoicePhrase.build("Yape", 1.05, null, null))
        assertEquals("Yape. 90 céntimos.", VoicePhrase.build("Yape", 0.9, null, null))
    }

    @Test fun bigAmountsHaveNoThousandsSeparator() {
        // "1,250" makes some TTS engines read "uno coma doscientos cincuenta".
        assertEquals("Yape. 1250 soles.", VoicePhrase.build("Yape", 1250.0, null, null))
    }
}
