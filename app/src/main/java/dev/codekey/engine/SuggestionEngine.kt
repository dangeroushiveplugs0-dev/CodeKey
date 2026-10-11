package dev.codekey.engine

data class CodeSuggestion(val text: String, val detail: String = "Insert completion")
data class AssistantResult(val code: String, val explanation: String, val supported: Boolean)

/** Offline completions and a deliberately small English-to-code starter helper. */
class SuggestionEngine {
    private val completions = listOf(
        "function" to "Function declaration", "console.log()" to "Print a value (JavaScript)",
        "const " to "Constant (JavaScript)", "let " to "Variable (JavaScript)",
        "print()" to "Print a value (Python)", "def " to "Function (Python)",
        "class " to "Class declaration", "return " to "Return a result",
        "if " to "Conditional", "for " to "Loop", "while " to "Loop while a condition is true",
        "import " to "Import a module", "fun " to "Function (Kotlin)", "val " to "Read-only value (Kotlin)",
        "var " to "Variable", "public static void main" to "Java entry point",
        "document.querySelector()" to "Find an HTML element", "=> " to "Arrow function",
        "async " to "Asynchronous function", "await " to "Wait for an asynchronous result",
        "try {\n    \n} catch (error) {\n    \n}" to "Try/catch error handling",
        "console.error()" to "Log an error", "TODO: " to "Mark unfinished work"
    )

    fun suggest(prefix: String, language: Language, limit: Int = 4): List<CodeSuggestion> {
        val token = prefix.takeLastWhile { it.isLetterOrDigit() || it == '_' }
        if (token.isBlank()) return listOf(
            CodeSuggestion("{", "Braces"), CodeSuggestion("}", "Closing brace"),
            CodeSuggestion("()", "Parentheses"), CodeSuggestion("=>", "Arrow")
        ).take(limit)
        val results = completions.filter { (candidate, _) ->
            candidate.startsWith(token, ignoreCase = true) && languageAllows(candidate, language)
        }.take(limit).map { CodeSuggestion(it.first, it.second) }
        return results.ifEmpty {
            completions.filter { it.first.startsWith(token, ignoreCase = true) }
                .take(limit).map { CodeSuggestion(it.first, it.second) }
        }
    }

    fun generateFromEnglish(request: String, language: Language): AssistantResult {
        val prompt = request.trim().lowercase()
        val target = if (language == Language.AUTO) Language.JAVASCRIPT else language
        if (prompt.isBlank()) return AssistantResult("", "Describe what you want the code to do.", false)
        val kind = when {
            listOf("button", "click", "press").any(prompt::contains) -> "button"
            listOf("loop", "repeat", "count from").any(prompt::contains) -> "loop"
            listOf("function", "method", "reusable").any(prompt::contains) -> "function"
            listOf("variable", "store", "save a value").any(prompt::contains) -> "variable"
            listOf("condition", "check whether", "if ").any(prompt::contains) -> "condition"
            listOf("hello", "print", "display", "output").any(prompt::contains) -> "hello"
            else -> "unknown"
        }
        val code = when (kind) {
            "button" -> when (target) {
                Language.HTML -> "<button type=\"button\" onclick=\"alert('Hello!')\">Click me</button>"
                Language.PYTHON -> "import tkinter as tk\n\ndef on_click():\n    print(\"Hello!\")\n\nroot = tk.Tk()\ntk.Button(root, text=\"Click me\", command=on_click).pack()\nroot.mainloop()"
                Language.KOTLIN -> "Button(onClick = {\n    println(\"Hello!\")\n}) {\n    Text(\"Click me\")\n}"
                else -> "const button = document.createElement(\"button\");\nbutton.textContent = \"Click me\";\nbutton.addEventListener(\"click\", () => alert(\"Hello!\"));\ndocument.body.appendChild(button);"
            }
            "loop" -> when (target) {
                Language.PYTHON -> "for number in range(1, 6):\n    print(number)"
                Language.KOTLIN -> "for (number in 1..5) {\n    println(number)\n}"
                Language.JAVA -> "for (int number = 1; number <= 5; number++) {\n    System.out.println(number);\n}"
                else -> "for (let number = 1; number <= 5; number++) {\n    console.log(number);\n}"
            }
            "function" -> when (target) {
                Language.PYTHON -> "def greet(name):\n    return \"Hello, \" + name + \"!\""
                Language.KOTLIN -> "fun greet(name: String): String {\n    return \"Hello, \" + name + \"!\"\n}"
                Language.JAVA -> "static String greet(String name) {\n    return \"Hello, \" + name + \"!\";\n}"
                else -> "function greet(name) {\n    return 'Hello, ' + name + '!';\n}"
            }
            "variable" -> when (target) {
                Language.PYTHON -> "name = \"Alex\""
                Language.KOTLIN -> "val name = \"Alex\""
                Language.JAVA -> "String name = \"Alex\";"
                Language.C, Language.CPP -> "const char* name = \"Alex\";"
                else -> "const name = \"Alex\";"
            }
            "condition" -> when (target) {
                Language.PYTHON -> "if score >= 10:\n    print(\"You passed\")\nelse:\n    print(\"Try again\")"
                else -> "if (score >= 10) {\n    console.log(\"You passed\");\n} else {\n    console.log(\"Try again\");\n}"
            }
            "hello" -> when (target) {
                Language.PYTHON -> "print(\"Hello, world!\")"
                Language.KOTLIN -> "println(\"Hello, world!\")"
                Language.JAVA -> "System.out.println(\"Hello, world!\");"
                Language.BASH -> "echo \"Hello, world!\""
                Language.HTML -> "<h1>Hello, world!</h1>"
                Language.SQL -> "SELECT 'Hello, world!' AS greeting;"
                else -> "console.log(\"Hello, world!\");"
            }
            else -> when (target) {
                Language.PYTHON -> "# TODO: clarify the steps you want"
                Language.HTML -> "<!-- TODO: describe the page content -->"
                Language.CSS -> "/* TODO: describe the style you want */"
                Language.SQL -> "-- TODO: describe the data you want to query"
                else -> "// TODO: clarify the request, e.g. create a function that greets a user"
            }
        }
        val explanation = when (kind) {
            "button" -> "Creates a clickable button. The web example shows a greeting when clicked; platform versions use their UI toolkit."
            "loop" -> "Repeats an action for numbers 1 through 5. Change the range to repeat a different number of times."
            "function" -> "Defines a reusable greet function that accepts a name and returns a greeting."
            "variable" -> "Stores a sample name. Replace Alex with the value you want to keep."
            "condition" -> "Checks whether score is at least 10 and selects a branch. Define score before running this example."
            "hello" -> "Displays a greeting using the selected language's common output syntax."
            else -> "This offline helper supports a starter set of patterns, not arbitrary instructions yet. Try print hello, make a button, create a function, or repeat a loop."
        }
        return AssistantResult(code, explanation, kind != "unknown")
    }

    private fun languageAllows(candidate: String, language: Language): Boolean = when (language) {
        Language.PYTHON -> candidate !in setOf("console.log()", "const ", "let ", "fun ")
        Language.JAVASCRIPT, Language.TYPESCRIPT -> candidate !in setOf("print()", "def ", "fun ", "val ")
        Language.KOTLIN -> candidate !in setOf("console.log()", "print()", "def ")
        else -> true
    }
}
