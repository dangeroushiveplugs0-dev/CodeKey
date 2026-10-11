package dev.codekey

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.text.InputType
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import dev.codekey.engine.SuggestionEngine
import dev.codekey.engine.Language

class MainActivity : Activity() {
    private val assistant = SuggestionEngine()
    private lateinit var requestInput: EditText
    private lateinit var languagePicker: Spinner
    private lateinit var codeOutput: TextView
    private lateinit var explanationOutput: TextView
    private var generatedCode = ""
    private lateinit var onlineEndpoint: EditText
    private lateinit var onlineModel: EditText
    private lateinit var onlineKey: EditText
    private val languages = Language.entries.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 22, 18, 18)
            setBackgroundColor(Color.rgb(20, 22, 28))
        }
        content.addView(TextView(this).apply {
            text = "CodeKey"
            textSize = 30f
            setTextColor(Color.rgb(137, 180, 250))
            typeface = Typeface.DEFAULT_BOLD
        })
        content.addView(TextView(this).apply {
            text = "Write an instruction in plain English. Review the code and explanation before copying it."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 12)
        })
        content.addView(actionButton("Enable keyboard in Android Settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        content.addView(actionButton("Choose CodeKey keyboard") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })

        languagePicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, languages.map { it.label })
        }
        content.addView(languagePicker)
        requestInput = EditText(this).apply {
            hint = "Example: make a button that says hello"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 16f
            minLines = 2
            gravity = Gravity.TOP
            setPadding(12, 10, 12, 10)
        }
        content.addView(requestInput)
        content.addView(actionButton("Generate offline code") { generate() })
        content.addView(TextView(this).apply {
            text = "Optional online AI (OpenAI-compatible endpoint)"
            textSize = 16f
            setTextColor(Color.rgb(137, 180, 250))
            setPadding(0, 14, 0, 4)
        })
        val prefs = getSharedPreferences("codekey_settings", Context.MODE_PRIVATE)
        onlineEndpoint = settingsField("Endpoint URL", prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "")
        onlineModel = settingsField("Model name", prefs.getString("model", "") ?: "")
        onlineKey = settingsField("API key (not saved)", "", true)
        content.addView(onlineEndpoint)
        content.addView(onlineModel)
        content.addView(onlineKey)
        content.addView(actionButton("Send instruction to online AI") { generateOnline() })
        content.addView(TextView(this).apply {
            text = "Online mode sends the instruction you typed and the selected language to the endpoint you configure. Do not enter passwords or secrets. CodeKey does not save the API key."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 4, 0, 8)
        })

        val scroll = ScrollView(this)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        results.addView(TextView(this).apply {
            text = "Generated code"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(137, 180, 250))
            setPadding(0, 16, 0, 6)
        })
        codeOutput = TextView(this).apply {
            text = "Your code will appear here."
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            background = panelBackground()
        }
        results.addView(codeOutput)
        explanationOutput = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 10)
        }
        results.addView(explanationOutput)
        results.addView(actionButton("Copy code") {
            if (generatedCode.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("CodeKey generated code", generatedCode))
                Toast.makeText(this, "Code copied", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(this, "Generate code first", Toast.LENGTH_SHORT).show()
        })
        results.addView(TextView(this).apply {
            text = "\nOffline mode: no request text is sent to a server. The starter generator recognizes a limited set of patterns; review and test code before using it."
            textSize = 12f
            setTextColor(Color.GRAY)
        })
        scroll.addView(results)
        content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(content)
    }

    private fun generate() {
        val language = languages.getOrElse(languagePicker.selectedItemPosition) { Language.AUTO }
        val result = assistant.generateFromEnglish(requestInput.text.toString(), language)
        generatedCode = result.code
        codeOutput.text = if (result.code.isBlank()) "Enter an instruction first." else result.code
        explanationOutput.text = result.explanation
    }

    private fun settingsField(fieldHint: String, initial: String, secret: Boolean = false) = EditText(this).apply {
        hint = fieldHint
        setText(initial)
        setTextColor(Color.WHITE)
        setHintTextColor(Color.GRAY)
        textSize = 14f
        if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        setPadding(10, 8, 10, 8)
    }

    private fun generateOnline() {
        val endpoint = onlineEndpoint.text.toString().trim()
        val model = onlineModel.text.toString().trim()
        val apiKey = onlineKey.text.toString()
        val instruction = requestInput.text.toString().trim()
        if (endpoint.isBlank() || model.isBlank() || instruction.isBlank()) {
            Toast.makeText(this, "Enter an instruction, endpoint, and model name first.", Toast.LENGTH_LONG).show()
            return
        }
        getSharedPreferences("codekey_settings", Context.MODE_PRIVATE).edit()
            .putString("endpoint", endpoint).putString("model", model).apply()
        explanationOutput.text = "Sending only your instruction to the configured endpoint…"
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val language = languages.getOrElse(languagePicker.selectedItemPosition) { Language.AUTO }
                val systemPrompt = "You are CodeKey, a beginner-friendly coding assistant. Generate useful code for the requested language, explain it plainly, and state assumptions. Do not claim code was tested unless it was."
                val userPrompt = "Language: " + language.label + "\nRequest: " + instruction + "\nReturn an explanation followed by the code."
                val body = JSONObject()
                    .put("model", model)
                    .put("messages", JSONArray()
                        .put(JSONObject().put("role", "system").put("content", systemPrompt))
                        .put(JSONObject().put("role", "user").put("content", userPrompt)))
                    .put("temperature", 0.3)
                connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                if (apiKey.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer " + apiKey)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (status !in 200..299) throw IllegalStateException("HTTP " + status + ": " + response.take(300))
                val answer = JSONObject(response).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content")
                runOnUiThread {
                    generatedCode = answer
                    codeOutput.text = answer
                    explanationOutput.text = "Online AI response. Review the output before using it. Copy code copies the full response."
                    onlineKey.setText("")
                }
            } catch (error: Exception) {
                runOnUiThread {
                    explanationOutput.text = "Online request failed: " + (error.message ?: "unknown error") +
                        ". Check the endpoint, model name, network, and API key."
                    onlineKey.setText("")
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun actionButton(title: String, action: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = panelBackground()
        setPadding(12, 8, 12, 8)
        setOnClickListener { action() }
    }

    private fun panelBackground() = GradientDrawable().apply {
        setColor(Color.rgb(48, 52, 63))
        cornerRadius = 14f
    }
}
