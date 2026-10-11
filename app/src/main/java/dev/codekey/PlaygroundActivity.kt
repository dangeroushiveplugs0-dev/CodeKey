package dev.codekey

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A small mobile playground. Code execution is explicitly online and runs on Piston. */
class PlaygroundActivity : Activity() {
    private lateinit var languagePicker: Spinner
    private lateinit var sourceEditor: EditText
    private lateinit var outputView: TextView
    private lateinit var runButton: Button

    private data class Runtime(val label: String, val id: String, val version: String = "*")
    private val runtimes = listOf(
        Runtime("Python", "python"), Runtime("JavaScript", "javascript"),
        Runtime("TypeScript", "typescript"), Runtime("Java", "java"),
        Runtime("C", "c"), Runtime("C++", "c++"), Runtime("C#", "csharp"),
        Runtime("Go", "go"), Runtime("Rust", "rust"), Runtime("PHP", "php"),
        Runtime("Ruby", "ruby"), Runtime("Bash", "bash"), Runtime("Kotlin", "kotlin"),
        Runtime("Swift", "swift"), Runtime("R", "r"), Runtime("Perl", "perl"),
        Runtime("Lua", "lua"), Runtime("Dart", "dart"), Runtime("Scala", "scala"),
        Runtime("Elixir", "elixir")
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
            text = "Edit code, choose a language, run it, and inspect stdout or errors."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 6, 0, 10)
        })
        languagePicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@PlaygroundActivity, android.R.layout.simple_spinner_dropdown_item, runtimes.map { it.label })
        }
        root.addView(languagePicker)
        sourceEditor = EditText(this).apply {
            setText("print(\"Hello from CodeKey!\")")
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
            minLines = 10
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(31, 34, 43))
        }
        root.addView(sourceEditor, LinearLayout.LayoutParams(-1, (280 * resources.displayMetrics.density).toInt()))
        runButton = Button(this).apply {
            text = "Run code online"
            isAllCaps = false
            setOnClickListener { runCode() }
        }
        root.addView(runButton)
        root.addView(TextView(this).apply {
            text = "Execution is sent to the public Piston code runner when you tap Run. Do not run secrets or private code here. This needs an internet connection; the keyboard's normal typing remains local."
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
            minLines = 4
        }
        root.addView(outputView)
        root.addView(Button(this).apply {
            text = "Copy source"
            isAllCaps = false
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("CodeKey source", sourceEditor.text.toString()))
                android.widget.Toast.makeText(this@PlaygroundActivity, "Source copied", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun runCode() {
        val runtime = runtimes.getOrElse(languagePicker.selectedItemPosition) { runtimes.first() }
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
                    .put("stdin", "")
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
                    runButton.text = "Run code online"
                }
            }
        }.start()
    }
}
