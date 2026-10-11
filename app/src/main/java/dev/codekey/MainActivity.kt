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
import dev.codekey.engine.EnglishCodeAssistant
import dev.codekey.engine.Language

class MainActivity : Activity() {
    private val assistant = EnglishCodeAssistant()
    private lateinit var requestInput: EditText
    private lateinit var languagePicker: Spinner
    private lateinit var codeOutput: TextView
    private lateinit var explanationOutput: TextView
    private var generatedCode = ""
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
        val result = assistant.generate(requestInput.text.toString(), language)
        generatedCode = result.code
        codeOutput.text = if (result.code.isBlank()) "Enter an instruction first." else result.code
        explanationOutput.text = result.explanation
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
