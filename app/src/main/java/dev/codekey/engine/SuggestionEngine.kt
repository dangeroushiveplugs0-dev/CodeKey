package dev.codekey.engine

data class CodeSuggestion(val text: String, val detail: String = "Insert completion")

/** Small offline completion engine. It never transmits typed text. */
class SuggestionEngine {
    private val completions = listOf(
        "function" to "Function declaration",
        "console.log()" to "Print a value (JavaScript)",
        "const " to "Constant (JavaScript)",
        "let " to "Variable (JavaScript)",
        "print()" to "Print a value (Python)",
        "def " to "Function (Python)",
        "class " to "Class declaration",
        "return " to "Return a result",
        "if " to "Conditional",
        "for " to "Loop",
        "while " to "Loop while a condition is true",
        "import " to "Import a module",
        "fun " to "Function (Kotlin)",
        "val " to "Read-only value (Kotlin)",
        "var " to "Variable",
        "public static void main" to "Java entry point",
        "document.querySelector()" to "Find an HTML element",
        "=> " to "Arrow function",
        "async " to "Asynchronous function",
        "await " to "Wait for an asynchronous result",
        "try {\n    \n} catch (error) {\n    \n}" to "Try/catch error handling",
        "console.error()" to "Log an error",
        "TODO: " to "Mark unfinished work"
    )

    fun suggest(prefix: String, language: Language, limit: Int = 4): List<CodeSuggestion> {
        val token = prefix.takeLastWhile { it.isLetterOrDigit() || it == '_' }
        if (token.isBlank()) return listOf(
            CodeSuggestion("{", "Braces"),
            CodeSuggestion("}", "Closing brace"),
            CodeSuggestion("()", "Parentheses"),
            CodeSuggestion("=>", "Arrow")
        ).take(limit)

        val results = completions.filter { (candidate, _) ->
            candidate.startsWith(token, ignoreCase = true) &&
                languageAllows(candidate, language)
        }.take(limit).map { CodeSuggestion(it.first, it.second) }
        return results.ifEmpty {
            completions.filter { it.first.startsWith(token, ignoreCase = true) }
                .take(limit).map { CodeSuggestion(it.first, it.second) }
        }
    }

    private fun languageAllows(candidate: String, language: Language): Boolean {
        return when (language) {
            Language.PYTHON -> candidate !in setOf("console.log()", "const ", "let ", "fun ")
            Language.JAVASCRIPT, Language.TYPESCRIPT -> candidate !in setOf("print()", "def ", "fun ", "val ")
            Language.KOTLIN -> candidate !in setOf("console.log()", "print()", "def ")
            else -> true
        }
    }
}
