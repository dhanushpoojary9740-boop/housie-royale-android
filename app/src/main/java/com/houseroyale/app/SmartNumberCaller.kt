
package com.houseroyale.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import java.util.Locale

class SmartNumberCaller(
    context: Context,
    private val onVoiceReadyChanged: (Boolean) -> Unit = {}
) {
    private val toneGenerator =
        ToneGenerator(AudioManager.STREAM_MUSIC, 75)

    private var voiceReady = false
    private var speechRate = 1.0f
    private var soundEnabled = true

    private lateinit var textToSpeech: TextToSpeech

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            voiceReady = status == TextToSpeech.SUCCESS

            if (voiceReady) {
                val result = textToSpeech.setLanguage(Locale("en", "IN"))

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    textToSpeech.setLanguage(Locale.US)
                }

                textToSpeech.setSpeechRate(speechRate)
            }

            onVoiceReadyChanged(voiceReady)
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 1.5f)

        if (voiceReady) {
            textToSpeech.setSpeechRate(speechRate)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun isVoiceReady(): Boolean = voiceReady

    fun announce(number: Int, voiceEnabled: Boolean) {
        if (soundEnabled) {
            toneGenerator.startTone(
                ToneGenerator.TONE_PROP_BEEP,
                140
            )
        }

        if (voiceEnabled && voiceReady) {
            textToSpeech.speak(
                "Number $number",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "housie_number_$number"
            )
        }
    }

    fun stopAnnouncement() {
        textToSpeech.stop()
    }

    fun release() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        toneGenerator.release()
        voiceReady = false
    }
}
