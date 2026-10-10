package com.example.autoai

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Agent(
    private val llm: LlmClient = LlmClient(),
    private val memory: MemoryStore = MemoryStore()
) {
    private val tools = ToolRegistry()

    suspend fun run(goal: String): String {
        memory.add("user_goal", goal)

        var state = "Начальная цель: $goal"
        repeat(8) { step ->
            val prompt = buildPrompt(goal, state, step)
            val raw = llm.decide(prompt)

            val action = try { JSONObject(raw) } catch (_: Exception) {
                return raw
            }

            val type = action.optString("type", "final")
            if (type == "final") {
                val answer = action.optString("answer", raw)
                memory.add("result", answer)
                return answer
            }

            val tool = action.optString("tool")
            val args = action.optJSONObject("args") ?: JSONObject()
            val result = tools.execute(tool, args)

            state += "\nШаг ${step + 1}: $tool -> $result"
            memory.add("tool", "$tool: $result")
        }
        return "Достигнут лимит шагов. Последнее состояние:\n$state"
    }

    private fun buildPrompt(goal: String, state: String, step: Int): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return """
Ты — автономный Android-агент AutoAI.
Текущая дата/время: $now.
Цель пользователя: $goal
Текущее состояние:
$state

Доступные инструменты:
- calculator: {"expression":"2+2"}
- save_memory: {"key":"...","value":"..."}
- get_memory: {"key":"..."}
- current_time: {}
- finish: {"answer":"..."}

На каждом шаге верни ТОЛЬКО JSON.
Для действия:
{"type":"tool","tool":"calculator","args":{"expression":"2+2"}}
Для завершения:
{"type":"final","answer":"..."}

Не выдумывай результат инструмента. Если задача уже решена — заверши её.
""".trimIndent()
    }
}
