package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxOperator
import com.example.ui.theme.SyntaxPreprocessor
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType
import java.util.regex.Pattern

object CppSyntaxHighlighter {

  private val KEYWORDS = setOf(
    "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor",
    "break", "case", "catch", "class", "compl", "concept", "const", "consteval",
    "constexpr", "constinit", "const_cast", "continue", "co_await", "co_return",
    "co_yield", "decltype", "default", "delete", "do", "dynamic_cast", "else",
    "enum", "explicit", "export", "extern", "false", "for", "friend", "goto",
    "if", "inline", "mutable", "namespace", "new", "noexcept", "not", "not_eq",
    "nullptr", "operator", "or", "or_eq", "override", "private", "protected",
    "public", "reflexpr", "register", "reinterpret_cast", "requires", "return",
    "sizeof", "static", "static_assert", "static_cast", "struct", "switch",
    "template", "this", "thread_local", "throw", "true", "try", "typedef",
    "typeid", "typename", "union", "using", "virtual", "volatile", "while", "xor", "xor_eq"
  )

  private val TYPES = setOf(
    "int", "double", "float", "char", "bool", "void", "short", "long",
    "signed", "unsigned", "size_t", "int8_t", "int16_t", "int32_t", "int64_t",
    "uint8_t", "uint16_t", "uint32_t", "uint64_t", "string", "vector", "map",
    "set", "pair", "tuple", "unordered_map", "unordered_set", "queue", "stack",
    "deque", "list", "array", "shared_ptr", "unique_ptr", "weak_ptr", "thread"
  )

  private val STREAMS_AND_LIBRARIES = setOf(
    "cin", "cout", "cerr", "endl", "printf", "scanf", "sort", "reverse",
    "min", "max", "accumulate", "sqrt", "pow", "abs", "push_back", "pop_back",
    "size", "empty", "front", "back", "begin", "end", "find", "join"
  )

  fun highlight(code: String): AnnotatedString {
    return buildAnnotatedString {
      append(code)

      // 1. Comments: // ... or /* ... */
      val commentPattern = Pattern.compile("(//.*)|(/\\*[\\s\\S]*?\\*/)")
      val commentMatcher = commentPattern.matcher(code)
      while (commentMatcher.find()) {
        addStyle(
          SpanStyle(color = SyntaxComment, fontWeight = FontWeight.Normal),
          commentMatcher.start(),
          commentMatcher.end()
        )
      }

      // 2. Preprocessor: #include, #define, #ifndef, #endif
      val prepPattern = Pattern.compile("^\\s*#[a-zA-Z_]+(?:\\s+[\"<][^\">]+[\">])?", Pattern.MULTILINE)
      val prepMatcher = prepPattern.matcher(code)
      while (prepMatcher.find()) {
        addStyle(
          SpanStyle(color = SyntaxPreprocessor, fontWeight = FontWeight.SemiBold),
          prepMatcher.start(),
          prepMatcher.end()
        )
      }

      // 3. Strings: "..." and '...'
      val stringPattern = Pattern.compile("(\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")|('[^'\\\\]*(?:\\\\.[^'\\\\]*)*')")
      val stringMatcher = stringPattern.matcher(code)
      while (stringMatcher.find()) {
        addStyle(
          SpanStyle(color = SyntaxString),
          stringMatcher.start(),
          stringMatcher.end()
        )
      }

      // 4. Numbers: 123, 3.14, 0xFF
      val numberPattern = Pattern.compile("\\b(\\d+(\\.\\d+)?f?|0x[0-9a-fA-F]+)\\b")
      val numberMatcher = numberPattern.matcher(code)
      while (numberMatcher.find()) {
        addStyle(
          SpanStyle(color = SyntaxNumber, fontWeight = FontWeight.Medium),
          numberMatcher.start(),
          numberMatcher.end()
        )
      }

      // 5. Words: Keywords, Types, Functions
      val wordPattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
      val wordMatcher = wordPattern.matcher(code)
      while (wordMatcher.find()) {
        val word = wordMatcher.group()
        val start = wordMatcher.start()
        val end = wordMatcher.end()

        when {
          KEYWORDS.contains(word) -> {
            addStyle(
              SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold),
              start,
              end
            )
          }
          TYPES.contains(word) -> {
            addStyle(
              SpanStyle(color = SyntaxType, fontWeight = FontWeight.SemiBold),
              start,
              end
            )
          }
          STREAMS_AND_LIBRARIES.contains(word) -> {
            addStyle(
              SpanStyle(color = SyntaxFunction, fontWeight = FontWeight.Medium),
              start,
              end
            )
          }
          word == "std" -> {
            addStyle(
              SpanStyle(color = SyntaxType, fontWeight = FontWeight.SemiBold),
              start,
              end
            )
          }
        }
      }

      // 6. Operators: ::, <<, >>, ->, ++, --, ==, !=, <=, >=
      val opPattern = Pattern.compile("(::|<<|>>|->|\\+\\+|--|==|!=|<=|>=|&&|\\|\\|)")
      val opMatcher = opPattern.matcher(code)
      while (opMatcher.find()) {
        addStyle(
          SpanStyle(color = SyntaxOperator, fontWeight = FontWeight.Bold),
          opMatcher.start(),
          opMatcher.end()
        )
      }
    }
  }
}
