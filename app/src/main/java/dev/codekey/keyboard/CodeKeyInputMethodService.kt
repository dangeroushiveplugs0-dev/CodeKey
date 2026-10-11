package dev.codekey.keyboard

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import dev.codekey.engine.Language
import dev.codekey.engine.SuggestionEngine

class CodeKeyInputMethodService : InputMethodService() {
    private val engine = SuggestionEngine()
    private var language = Language.AUTO
    private var passwordField = false
    private lateinit var suggestionsRow: LinearLayout
    private lateinit var root: LinearLayout
    private val bg = Color.rgb(22, 24, 30)
    private val keyBg = Color.rgb(48, 52, 63)
    private val accent = Color.rgb(137, 180, 250)

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5, 7, 5, 7)
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        header.addView(label("CODEKEY", accent, 12f).apply { setPadding(8, 8, 12, 8) })
        header.addView(key("AI", accent) { openAssistant() }, weighted())
        header.addView(key(language.label) { cycleLanguage() }, weighted())
        root.addView(header, rowParams())

        suggestionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        root.addView(suggestionsRow, rowParams())

        listOf(
            listOf("tab", "(", ")", "{", "}", "[", "]", "⌫"),
            listOf(":", ";", "'", "\"", "=", "=>", "_", "↵"),
            listOf("123", ".", ",", "/", "!", "?", "space", "⏎")
        ).forEach { values ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            values.forEach { value ->
                row.addView(key(value) { pressKey(value) }, weighted())
            }
            root.addView(row, rowParams())
        }
        refreshSuggestions()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Avoid coding-specific suggestions in password fields.
        val inputType = info?.inputType ?: 0
        val variation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        val klass = inputType and android.text.InputType.TYPE_MASK_CLASS
        val password = klass == android.text.InputType.TYPE_CLASS_TEXT &&
            variation in setOf(
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )
        passwordField = password || (klass == android.text.InputType.TYPE_CLASS_NUMBER &&
            variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        if (passwordField) {
            suggestionsRow.removeAllViews()
            suggestionsRow.addView(label("Suggestions disabled for password fields", Color.LTGRAY, 12f))
        } else refreshSuggestions()
    }

    private fun pressKey(value: String) {
        val ic = currentInputConnection ?: return
        when (value) {
            "⌫" -> {
                val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
                val after = ic.getTextAfterCursor(1, 0)?.toString().orEmpty()
                val closing = mapOf("(" to ")", "[" to "]", "{" to "}", "\"" to "\"", "'" to "'")
                if (before in closing.keys && after == closing[before]) ic.deleteSurroundingText(1, 1)
                else ic.deleteSurroundingText(1, 0)
            }
            "↵", "⏎" -> ic.commitText("\n", 1)
            "space" -> ic.commitText(" ", 1)
            "tab" -> ic.commitText("    ", 1)
            "123" -> showToast("Number row coming soon")
            "(" , "[" , "{" -> insertPair(value, mapOf("(" to ")", "[" to "]", "{" to "}")[value]!!)
            "\"" , "'" -> insertPair(value, value)
            else -> {
                val after = ic.getTextAfterCursor(1, 0)?.toString().orEmpty()
                if (value in listOf(")", "]", "}") && after == value) {
                    val cursor = cursorPosition(ic)
                    if (cursor >= 0) ic.setSelection(cursor + 1, cursor + 1)
                } else ic.commitText(value, 1)
            }
        }
        refreshSuggestions()
    }

    private fun refreshSuggestions() {
        if (!::suggestionsRow.isInitialized) return
        suggestionsRow.removeAllViews()
        if (passwordField) return
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val token = before.takeLastWhile { it.isLetterOrDigit() || it == '_' }
        val results = engine.suggest(token, language)
        if (results.isEmpty()) {
            suggestionsRow.addView(label("Type code to see suggestions", Color.LTGRAY, 12f))
        } else results.forEach { suggestion ->
            suggestionsRow.addView(key(suggestion.text, accent) {
                val currentBefore = ic.getTextBeforeCursor(80, 0)?.toString().orEmpty()
                val currentToken = currentBefore.takeLastWhile { it.isLetterOrDigit() || it == '_' }
                if (currentToken.isNotEmpty()) ic.deleteSurroundingText(currentToken.length, 0)
                ic.commitText(suggestion.text, 1)
                refreshSuggestions()
            }, weighted())
        }
    }

    private fun insertPair(open: String, close: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(open + close, 1)
        val cursor = cursorPosition(ic)
        if (cursor >= 0) ic.setSelection(cursor - close.length, cursor - close.length)
    }

    private fun cursorPosition(ic: android.view.inputmethod.InputConnection): Int =
        ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)?.selectionStart ?: -1

    private fun cycleLanguage() {
        val languages = Language.entries
        language = languages[(languages.indexOf(language) + 1) % languages.size]
        if (::root.isInitialized) {
            val header = root.getChildAt(0) as? LinearLayout
            (header?.getChildAt(2) as? Button)?.text = language.label
        }
        refreshSuggestions()
    }

    private fun openAssistant() {
        val intent = android.content.Intent(this, Class.forName("dev.codekey.MainActivity"))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun key(text: String, color: Int = Color.WHITE, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            setTextColor(color)
            textSize = if (text.length > 8) 10f else 14f
            isAllCaps = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(2, 0, 2, 0)
            background = GradientDrawable().apply {
                setColor(keyBg)
                cornerRadius = 12f
            }
            setOnClickListener { action() }
        }

    private fun label(text: String, color: Int, size: Float) = TextView(this).apply {
        this.text = text
        setTextColor(color)
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
    }

    private fun weighted() = LinearLayout.LayoutParams(0, 43, 1f).apply {
        setMargins(2, 2, 2, 2)
    }
    private fun rowParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, 2, 0, 2) }

    private fun showToast(message: String) =
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
}
