package com.example.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.view.MotionEvent\nimport android.content.Intent\nimport android.content.SharedPreferences
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import android.widget.EditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var btnTalk: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView

    private var speechRecognizer: SpeechRecognizer? = null\n    private lateinit var prefs: SharedPreferences
    private var tts: TextToSpeech? = null

    // Configure your backend URL here (Flask / VPS / local PC). Example: http://192.168.1.10:5005/ask
    // BACKEND_URL is read from SharedPreferences

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnTalk = findViewById(R.id.btnTalk)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)

        // Request RECORD_AUDIO permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 123)
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru")
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { runOnUiThread { tvStatus.text = "Слушаю..." } }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { runOnUiThread { tvStatus.text = "Обработка..." } }
            override fun onError(error: Int) { runOnUiThread { tvStatus.text = "Ошибка распознавания: $error" } }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.joinToString(" ") ?: ""
                runOnUiThread {
                    appendLog("Ты: $text")
                }
                if (text.isNotBlank()) {
                    queryBackend(text)
                } else {
                    runOnUiThread { tvStatus.text = "Ничего не распознано" }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)

        btnTalk.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startListening()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopListening()
                    true
                }
                else -> false
            }
        }
    }

    private fun appendLog(text: String) {
        tvLog.append(text + "\n")
        val sv = findViewById<android.widget.ScrollView>(R.id.scroll)
        sv.post { sv.fullScroll(View.FOCUS_DOWN) }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizer?.startListening(intent)
        tvStatus.text = "Подключение микрофона..."
    }

    private fun stopListening() {
        try { speechRecognizer?.stopListening() } catch (e: Exception) {}
    }

    private fun queryBackend(text: String) {
        tvStatus.text = "Отправляю запрос..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject()
                json.put("text", text)
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder().url(BACKEND_URL).post(body).build()
                val resp = client.newCall(req).execute()
                val respBody = resp.body?.string() ?: ""
                val answer = try {
                    JSONObject(respBody).optString("answer", respBody)
                } catch (e: Exception) {
                    respBody
                }
                runOnUiThread {
                    appendLog("Jarvis: $answer")
                    tvStatus.text = "Готов"
                }
                speak(answer)
            } catch (e: Exception) {
                runOnUiThread {
                    appendLog("Ошибка: ${e.message}")
                    tvStatus.text = "Ошибка сети"
                }
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS")
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        tts?.shutdown()
    }
}
