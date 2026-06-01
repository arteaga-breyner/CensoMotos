package com.putumayo.censomotos.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Maneja el reconocimiento de voz y extrae datos de la moto del texto hablado.
 *
 * Ejemplo de entrada: "Yamaha 125 color roja modelo 2022"
 * Maneja variaciones del habla y acento del Putumayo.
 */
class VoiceRecognizer(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: VoiceListener? = null

    interface VoiceListener {
        fun onResult(texto: String)
        fun onError(mensaje: String)
        fun onReady()           // micrófono listo para escuchar
        fun onEndOfSpeech()     // usuario terminó de hablar
    }

    fun iniciar(listener: VoiceListener) {
        this.listener = listener

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("El reconocimiento de voz no está disponible en este dispositivo")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listener.onReady()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                listener.onEndOfSpeech()
            }
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sin permiso de micrófono"
                    SpeechRecognizer.ERROR_NETWORK -> "Error de red (requiere conexión para reconocimiento de voz)"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de red agotado"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió. Intente de nuevo"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                    SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz. Intente de nuevo"
                    else -> "Error desconocido ($error)"
                }
                listener.onError(msg)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val texto = matches?.firstOrNull() ?: ""
                listener.onResult(texto)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO")   // Español colombiano
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-CO")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        speechRecognizer?.startListening(intent)
    }

    fun detener() {
        speechRecognizer?.stopListening()
    }

    fun liberar() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

// ── Extractor de entidades ────────────────────────────────────────────────────

/**
 * Extrae campos de la moto a partir del texto reconocido por voz.
 * Maneja variaciones ortográficas y de pronunciación del Putumayo.
 */
object VoiceEntityExtractor {

    data class DatosExtraidos(
        val marca: String?,
        val cilindraje: Int?,
        val modelo: Int?,
        val color: String?
    ) {
        fun estaCompleto() = marca != null && cilindraje != null && modelo != null && color != null
    }

    // Sinónimos / variaciones para marcas
    private val MARCAS_MAP = mapOf(
        listOf("yamaha", "yama", "yamaja") to "Yamaha",
        listOf("honda", "onda") to "Honda",
        listOf("suzuki", "suzuqui", "suzuki", "zuzuki") to "Suzuki",
        listOf("bajaj", "baja", "baja j", "bajá") to "Bajaj",
        listOf("kawasaki", "kawazaki", "cabasaki") to "Kawasaki",
        listOf("akt", "ackt") to "AKT",
        listOf("tvs", "teveese") to "TVS",
        listOf("auteco", "auto eco") to "Auteco",
        listOf("royal enfield", "royal", "enfield") to "Royal Enfield",
        listOf("ktm") to "KTM",
        listOf("pulsar") to "Bajaj",   // Pulsar es marca Bajaj
        listOf("hero", "jero") to "Hero",
        listOf("bera") to "Bera",
        listOf("otro", "otra") to "Otro"
    )

    // Sinónimos para colores
    private val COLORES_MAP = mapOf(
        listOf("roja", "rojo", "red", "bermellón") to "Roja",
        listOf("azul", "blue", "azúl") to "Azul",
        listOf("negra", "negro", "black") to "Negra",
        listOf("blanca", "blanco", "white") to "Blanca",
        listOf("gris", "gray", "grey", "plomo") to "Gris",
        listOf("verde", "green") to "Verde",
        listOf("amarilla", "amarillo", "yellow") to "Amarilla",
        listOf("naranja", "anaranjada", "orange") to "Naranja",
        listOf("plateada", "plateado", "silver") to "Plateada",
        listOf("café", "cafe", "marrón", "marron", "brown") to "Café",
        listOf("morada", "morado", "purple", "violeta") to "Morada",
        listOf("rosa", "rosada", "pink") to "Rosa"
    )

    /**
     * Extrae datos de la moto del texto hablado.
     * Ejemplos válidos:
     *   "Yamaha 125 color roja modelo 2022"
     *   "honda ciento cincuenta negra dos mil veinte"
     *   "bajaj pulsar 200 azul 2021"
     */
    fun extraer(texto: String): DatosExtraidos {
        val textoNorm = texto.lowercase().trim()

        return DatosExtraidos(
            marca = extraerMarca(textoNorm),
            cilindraje = extraerCilindraje(textoNorm),
            modelo = extraerModelo(textoNorm),
            color = extraerColor(textoNorm)
        )
    }

    private fun extraerMarca(texto: String): String? {
        for ((variaciones, marcaCanonica) in MARCAS_MAP) {
            for (v in variaciones) {
                if (texto.contains(v)) return marcaCanonica
            }
        }
        return null
    }

    private fun extraerCilindraje(texto: String): Int? {
        // Buscar número seguido de "cc" o "cilindros" o solo número entre 50-500
        val patrones = listOf(
            Regex("""(\d{2,3})\s*(?:cc|cilindros|centímetros)"""),
            Regex("""(\d{2,3})\s+(?:cc|cilindros)"""),
            // Números escritos en texto
            Regex("""(cien(?:to)?|ciento\s+(?:veinticinco|cincuenta|setenta\s+y\s+cinco)|doscientos(?:\s+cincuenta)?|dos\s+cincuenta|ciento\s+veinticinco)""")
        )

        // Primero intenta con dígitos
        val matchDigitos = Regex("""(?<!\d)(\d{2,3})(?!\d)""").find(texto)
        if (matchDigitos != null) {
            val num = matchDigitos.groupValues[1].toIntOrNull()
            if (num != null && num in 50..500) return num
        }

        // Números en texto común
        val numerosTexto = mapOf(
            "cien" to 100, "ciento" to 100,
            "ciento veinticinco" to 125, "ciento cincuenta" to 150,
            "doscientos" to 200, "doscientos cincuenta" to 250,
            "dos cincuenta" to 250
        )
        for ((frase, valor) in numerosTexto) {
            if (texto.contains(frase)) return valor
        }

        return null
    }

    private fun extraerModelo(texto: String): Int? {
        // Busca el año después de palabras clave
        val patronAño = Regex("""(?:modelo|año|del|del año)\s+(\d{4})""")
        val matchClave = patronAño.find(texto)
        if (matchClave != null) {
            val año = matchClave.groupValues[1].toIntOrNull()
            if (año != null && año in 1990..2026) return año
        }

        // Busca cualquier año de 4 dígitos en rango válido
        val patronGeneral = Regex("""\b(2\d{3}|199\d)\b""")
        val matches = patronGeneral.findAll(texto).toList()
        // Toma el último año encontrado (el modelo suele venir al final)
        return matches.lastOrNull()?.groupValues?.get(1)?.toIntOrNull()
            ?.takeIf { it in 1990..2026 }
    }

    private fun extraerColor(texto: String): String? {
        for ((variaciones, colorCanónico) in COLORES_MAP) {
            for (v in variaciones) {
                // Buscar después de "color" primero
                if (texto.contains("color $v") || texto.contains("color: $v")) {
                    return colorCanónico
                }
            }
        }
        // Si no encontró con "color", buscar la palabra suelta
        for ((variaciones, colorCanónico) in COLORES_MAP) {
            for (v in variaciones) {
                if (texto.contains(v)) return colorCanónico
            }
        }
        return null
    }
}
