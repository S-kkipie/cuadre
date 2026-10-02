package pe.aido.cuadre.alerts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Says each confirmed payment out loud with the phone's own TTS engine (on device, offline).
 * Starts after a short pause so it doesn't talk over the confirmation chime.
 */
class PaymentVoice(context: Context) {

    private var ready = false
    private val queue = ArrayDeque<String>()
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val peru = Locale.forLanguageTag("es-PE")
            val result = tts.setLanguage(peru)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.forLanguageTag("es"))
            }
            tts.setSpeechRate(0.95f)
            ready = true
            synchronized(queue) { while (queue.isNotEmpty()) speakNow(queue.removeFirst()) }
        }
    }

    fun say(phrase: String) {
        synchronized(queue) {
            if (ready) speakNow(phrase) else queue.addLast(phrase)
        }
    }

    private fun speakNow(phrase: String) {
        tts.playSilentUtterance(CHIME_MILLIS, TextToSpeech.QUEUE_ADD, null)
        tts.speak(phrase, TextToSpeech.QUEUE_ADD, null, phrase.hashCode().toString())
    }

    private companion object {
        const val CHIME_MILLIS = 900L
    }
}
