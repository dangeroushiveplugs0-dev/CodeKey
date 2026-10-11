package dev.codekey

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(Color.rgb(20, 22, 28))
        }
        root.addView(TextView(this).apply {
            text = "CodeKey"
            textSize = 32f
            setTextColor(Color.rgb(137, 180, 250))
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "A coding keyboard for learning as you type"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 28)
        })
        root.addView(actionButton("1 · Enable CodeKey in Settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(actionButton("2 · Choose CodeKey keyboard") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        })
        root.addView(actionButton("Try the keyboard") {
            val edit = android.widget.EditText(this).apply {
                hint = "Try typing function, print, or console..."
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                textSize = 16f
            }
            root.addView(edit, 2)
            edit.requestFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
        })
        root.addView(TextView(this).apply {
            text = "\nOffline autocomplete is built in. AI generation is optional and will be configured in a later milestone. Typed text is not sent to a server by this version."
            textSize = 14f
            setTextColor(Color.LTGRAY)
        })
        setContentView(root)
    }

    private fun actionButton(title: String, action: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            setColor(Color.rgb(48, 52, 63))
            cornerRadius = 18f
        }
        setPadding(16, 12, 16, 12)
        setOnClickListener { action() }
    }
}
