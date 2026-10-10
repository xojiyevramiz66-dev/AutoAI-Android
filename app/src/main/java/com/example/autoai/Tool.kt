package com.example.autoai

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ToolRegistry {
    private val memory = MemoryStore()

    fun execute(name: String, args: JSONObject): String {
        return when (name) {
            "calculator" -> calculator(args.optString("expression"))
            "save_memory" -> {
                val key = args.optString("key")
                val value = args.optString("value")
                memory.add(key, value)
                "saved"
            }
            "get_memory" -> memory.get(args.optString("key")) ?: "not found"
            "current_time" ->
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            "finish" -> args.optString("answer")
            else -> "unknown tool: $name"
        }
    }

    private fun calculator(expression: String): String {
        // MVP: безопасный калькулятор только для цифр и базовых операторов.
        if (!expression.matches(Regex("[0-9+\-*/(). %]+"))) {
            return "Недопустимое выражение"
        }
        return try {
            SimpleMath.eval(expression).toString()
        } catch (_: Exception) {
            "Ошибка вычисления"
        }
    }
}

private object SimpleMath {
    fun eval(s: String): Double {
        val t = s.replace(" ", "")
        // Минимальный парсер: + - * / и скобки.
        class P(val x: String) {
            var i = 0
            fun parse(): Double {
                val v = expr()
                if (i != x.length) error("syntax")
                return v
            }
            fun expr(): Double {
                var v = term()
                while (i < x.length && (x[i] == '+' || x[i] == '-')) {
                    val op = x[i++]
                    val r = term()
                    v = if (op == '+') v + r else v - r
                }
                return v
            }
            fun term(): Double {
                var v = factor()
                while (i < x.length && (x[i] == '*' || x[i] == '/')) {
                    val op = x[i++]
                    val r = factor()
                    v = if (op == '*') v * r else v / r
                }
                return v
            }
            fun factor(): Double {
                if (i < x.length && x[i] == '(') {
                    i++; val v = expr()
                    if (i >= x.length || x[i++] != ')') error("paren")
                    return v
                }
                val start = i
                if (i < x.length && x[i] == '-') i++
                while (i < x.length && (x[i].isDigit() || x[i] == '.')) i++
                return x.substring(start, i).toDouble()
            }
        }
        return P(t).parse()
    }
}
