package com.example.jarvis

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etBackend: EditText
    private lateinit var swMock: Switch
    private lateinit var btnSave: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        etBackend = findViewById(R.id.etBackend)
        swMock = findViewById(R.id.swMock)
        btnSave = findViewById(R.id.btnSaveSettings)

        etBackend.setText(prefs.getString("backend_url", ""))
        swMock.isChecked = prefs.getBoolean("use_mock", false)

        btnSave.setOnClickListener {
            val url = etBackend.text.toString().trim()
            val mock = swMock.isChecked
            prefs.edit().putString("backend_url", url).putBoolean("use_mock", mock).apply()
            finish()
        }
    }
}
