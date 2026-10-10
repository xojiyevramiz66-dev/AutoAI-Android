package com.example.autoai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AutoAIApp() }
    }
}

class AgentViewModel : ViewModel() {
    private val agent = Agent()
    var input by mutableStateOf("")
    var running by mutableStateOf(false)
    val messages = mutableStateListOf<Pair<String, String>>()

    fun runGoal() {
        val goal = input.trim()
        if (goal.isEmpty() || running) return
        input = ""
        messages += "Ты" to goal
        running = true
        viewModelScope.launch {
            try {
                val result = agent.run(goal)
                messages += "AutoAI" to result
            } catch (e: Exception) {
                messages += "Ошибка" to (e.message ?: "unknown error")
            } finally { running = false }
        }
    }
}

@Composable
fun AutoAIApp() {
    val vm = remember { AgentViewModel() }
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("AutoAI", style = MaterialTheme.typography.headlineMedium)
            Text("Автономный агент • MVP")

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.messages) { (who, text) ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(who, style = MaterialTheme.typography.labelLarge)
                            Text(text)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = vm.input,
                onValueChange = { vm.input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Цель для агента") },
                enabled = !vm.running
            )

            Button(
                onClick = vm::runGoal,
                modifier = Modifier.fillMaxWidth(),
                enabled = !vm.running && vm.input.isNotBlank()
            ) {
                Text(if (vm.running) "Работаю..." else "Запустить агента")
            }
        }
    }
}
