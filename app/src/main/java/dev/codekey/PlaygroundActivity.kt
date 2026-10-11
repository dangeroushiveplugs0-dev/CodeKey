package dev.codekey

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

/** Code editor with saved drafts, explicit online execution, and a network-blocked local web preview. */
class PlaygroundActivity : Activity() {
    private lateinit var languagePicker: Spinner
    private lateinit var sourceEditor: EditText
    private lateinit var stdinEditor: EditText
    private lateinit var outputView: TextView
    private lateinit var runButton: Button
    private lateinit var previewButton: Button
    private lateinit var preview: WebView
    private val prefs by lazy { getSharedPreferences("codekey_playground", MODE_PRIVATE) }

    private data class Runtime(val label: String, val id: String, val version: String = "*")
    private val runtimes = listOf(
        Runtime("Python", "python"), Runtime("JavaScript", "javascript"),
        Runtime("TypeScript", "typescript"), Runtime("Java", "java"),
        Runtime("C", "c"), Runtime("C++", "c++"), Runtime("C#", "csharp"),
        Runtime("Go", "go"), Runtime("Rust", "rust"), Runtime("PHP", "php"),
        Runtime("Ruby", "ruby"), Runtime("Bash", "bash"), Runtime("Kotlin", "kotlin"),
        Runtime("Swift", "swift"), Runtime("R", "r"), Runtime("Perl", "perl"),
        Runtime("Lua", "lua"), Runtime("Dart", "dart"), Runtime("Scala", "scala"),
        Runtime("Elixir", "elixir"), Runtime("HTML / CSS / JS (local preview)", "web", "local")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 18, 16, 18)
            setBackgroundColor(Color.rgb(20, 22, 28))
        }
        root.addView(TextView(this).apply {
            text = "Code Playground"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(137, 180, 250))
        })
        root.addView(TextView(this).apply {
            text = "Drafts save on this device. Choose Web Preview for local HTML, CSS, and JavaScript."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 6, 0, 10)
        })
        languagePicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@PlaygroundActivity, android.R.layout.simple_spinner_dropdown_item, runtimes.map { it.label })
            setSelection(prefs.getInt("language_index", 0).coerceIn(0, runtimes.lastIndex))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    prefs.edit().putInt("language_index", position).apply()
                    val webMode = runtimes[position].id == "web"
                    runButton.text = if (webMode) "Preview locally" else "Run code online"
                    previewButton.visibility = if (webMode) View.VISIBLE else View.GONE
                    if (webMode) {
                        stdinEditor.visibility = View.GONE
                        sourceEditor.hint = "Enter a complete HTML document, including optional <style> and <script> tags"
                        if (sourceEditor.text.isBlank()) sourceEditor.setText(defaultWebPage())
                    } else {
                        stdinEditor.visibility = View.VISIBLE
                        sourceEditor.hint = "Write your code here"
                        if (sourceEditor.text.isBlank()) sourceEditor.setText(defaultCode())
                    }
                }
            }
        }
        root.addView(languagePicker)
        sourceEditor = EditText(this).apply {
            setText(prefs.getString("source", defaultCode()))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
            minLines = 10
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(31, 34, 43))
            addTextChangedListener(simpleWatcher { prefs.edit().putString("source", it).apply() })
        }
        root.addView(sourceEditor, LinearLayout.LayoutParams(-1, (280 * resources.displayMetrics.density).toInt()))

        stdinEditor = EditText(this).apply {
            hint = "Optional stdin / program input"
            setText(prefs.getString("stdin", ""))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            typeface = Typeface.MONOSPACE
            textSize = 13f
            minLines = 2
            maxLines = 4
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(31, 34, 43))
            addTextChangedListener(simpleWatcher { prefs.edit().putString("stdin", it).apply() })
        }
        root.addView(stdinEditor, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))

        runButton = Button(this).apply {
            text = "Run code online"
            isAllCaps = false
            setOnClickListener {
                if (currentRuntime().id == "web") showPreview() else runCode()
            }
        }
        root.addView(runButton)
        previewButton = Button(this).apply {
            text = "Refresh local preview"
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { showPreview() }
        }
        root.addView(previewButton)
        root.addView(TextView(this).apply {
            text = "Online Run sends source and stdin to the public Piston runner. Web Preview is processed on-device; external network requests and navigation are blocked inside the preview."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = "Console / result"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(137, 180, 250))
        })
        outputView = TextView(this).apply {
            text = "Output will appear here."
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(31, 34, 43))
            minLines = 3
        }
        root.addView(outputView)
        preview = WebView(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    if (uri.host == "codekey.local") return null
                    return WebResourceResponse(
                        "text/plain", "UTF-8", 403, "Blocked by CodeKey preview",
                        emptyMap(), ByteArrayInputStream(ByteArray(0))
                    )
                }
            }
        }
        root.addView(preview, LinearLayout.LayoutParams(-1, (300 * resources.displayMetrics.density).toInt()))
        root.addView(actionButton("Copy source") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("CodeKey source", sourceEditor.text.toString()))
            Toast.makeText(this, "Source copied", Toast.LENGTH_SHORT).show()
        })
        root.addView(actionButton("Hide preview") { preview.visibility = View.GONE })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun currentRuntime() = runtimes.getOrElse(languagePicker.selectedItemPosition) { runtimes.first() }

    private fun showPreview() {
        val source = sourceEditor.text.toString()
        if (source.isBlank()) {
            outputView.text = "Enter HTML before previewing."
            return
        }
        preview.visibility = View.VISIBLE
        preview.loadDataWithBaseURL("https://codekey.local/", source, "text/html", "UTF-8", null)
        outputView.text = "Local preview loaded. External network requests and navigation are blocked."
    }

    private fun runCode() {
        val runtime = currentRuntime()
        val source = sourceEditor.text.toString()
        if (source.isBlank()) {
            outputView.text = "Enter code before running it."
            return
        }
        runButton.isEnabled = false
        runButton.text = "Running…"
        outputView.text = "Sending code to the runner…"
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val payload = JSONObject()
                    .put("language", runtime.id)
                    .put("version", runtime.version)
                    .put("files", JSONArray().put(JSONObject().put("content", source)))
                    .put("stdin", stdinEditor.text.toString())
                    .put("args", JSONArray())
                    .put("compile_timeout", 10000)
                    .put("run_timeout", 5000)
                connection = URL("https://emkc.org/api/v2/piston/execute").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                if (status !in 200..299) throw IllegalStateException("Runner HTTP $status: " + body.take(500))
                val result = JSONObject(body)
                val run = result.optJSONObject("run")
                val compile = result.optJSONObject("compile")
                val output = buildString {
                    if (compile != null && compile.optString("output").isNotBlank()) append("COMPILE OUTPUT\n").append(compile.optString("output")).append("\n")
                    append("STDOUT\n").append(run?.optString("stdout").orEmpty())
                    if (run?.optString("stderr").orEmpty().isNotBlank()) append("\nSTDERR\n").append(run?.optString("stderr"))
                    if (run?.has("code") == true) append("\n\nExit code: ").append(run.optInt("code"))
                    if (run?.optString("output").orEmpty().isBlank() && compile?.optString("output").orEmpty().isBlank()) append("(No output)")
                }
                runOnUiThread { outputView.text = output }
            } catch (error: Exception) {
                runOnUiThread {
                    outputView.text = "Could not run code: ${error.message ?: "unknown error"}\n\nCheck your connection, selected language, and runner availability."
                }
            } finally {
                connection?.disconnect()
                runOnUiThread {
                    runButton.isEnabled = true
                    runButton.text = if (currentRuntime().id == "web") "Preview locally" else "Run code online"
                }
            }
        }.start()
    }

    private fun defaultCode() = "print(\"Hello from CodeKey!\")"

    private fun defaultWebPage() = """<!doctype html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
body { font-family: sans-serif; padding: 1rem; color: #202633; }
button { padding: .6rem 1rem; border: 0; border-radius: .6rem; background: #89b4fa; }
</style>
</head>
<body>
<h2>Hello from CodeKey</h2>
<p>Edit this HTML, CSS, and JavaScript, then preview it on-device.</p>
<button onclick="document.getElementById('result').textContent = 'JavaScript works!'">Test JavaScript</button>
<p id="result"></p>
</body>
</html>"""

    private fun actionButton(title: String, action: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun simpleWatcher(onChanged: (String) -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChanged(s?.toString().orEmpty())
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}
