package com.example.autoai

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var etInput: EditText
    private lateinit var btnSend: Button
    private lateinit var btnAttachFile: Button
    private lateinit var btnAttachPhoto: Button
    private lateinit var btnSaveMq4: Button
    private lateinit var tvAgentOutput: TextView

    private var lastAgentResponseText: String = ""

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val path = uri.toString().lowercase()
            if (path.endsWith(".zip")) {
                Toast.makeText(this, "ZIP файл выбран", Toast.LENGTH_SHORT).show()
                val zipAnalysis = unzipAndAnalyze(uri)
                val prompt = "Я загрузил ZIP-архив с проектом. Вот его содержимое: $zipAnalysis"
                sendQueryToAgent(prompt)
            } else {
                val fileContent = readTextFromUri(uri)
                if (!fileContent.isNullOrEmpty()) {
                    val prompt = "Проанализируй этот файл:\n$fileContent"
                    sendQueryToAgent(prompt)
                }
            }
        }
    }

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(this, "Фото выбрано", Toast.LENGTH_SHORT).show()
            val prompt = "Я прикрепил фото для анализа интерфейса/кода. Подскажи, что здесь нужно исправить."
            sendQueryToAgent(prompt)
        } else {
            Toast.makeText(this, "Фото не выбрано", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)
        btnAttachFile = findViewById(R.id.btnAttachFile)
        btnAttachPhoto = findViewById(R.id.btnAttachPhoto)
        btnSaveMq4 = findViewById(R.id.btnSaveMq4)
        
        // Привязываем TextView с правильным ID из вашей разметки (tvAgentOutput)
        tvAgentOutput = findViewById(R.id.tvAgentOutput)

        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                sendQueryToAgent(userText)
                etInput.setText("")
            } else {
                Toast.makeText(this, "Введите текст запроса", Toast.LENGTH_SHORT).show()
            }
        }

        btnAttachFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        btnAttachPhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        btnSaveMq4.setOnClickListener {
            if (lastAgentResponseText.isNotEmpty()) {
                saveMq4File(this@MainActivity, lastAgentResponseText)
            } else {
                Toast.makeText(this, "Нет данных от агента для сохранения", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendQueryToAgent(query: String) {
        Toast.makeText(this, "Запрос отправлен...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Вызов агента / LLM клиента
                val response = Agent.decide(query) ?: "Пустой ответ"
                lastAgentResponseText = response

                withContext(Dispatchers.Main) {
                    // Выводим ответ модели в tvAgentOutput без стирания пользовательского ввода
                    tvAgentOutput.text = response
                    Toast.makeText(this@MainActivity, "Ответ получен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = "Ошибка запроса: ${e.localizedMessage}"
                    tvAgentOutput.text = errorMsg
                    Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line).append("\n")
                line = reader.readLine()
            }
            reader.close()
            stringBuilder.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun unzipAndAnalyze(uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val zipInputStream = ZipInputStream(inputStream)
                var zipEntry = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val fileName = zipEntry.name
                    stringBuilder.append("Файл: $fileName\n")
                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
                zipInputStream.close()
            }
        } catch (e: Exception) {
            return "Ошибка распаковки архива"
        }
        return stringBuilder.toString()
    }

    private fun saveMq4File(context: Context, content: String) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "AgentOutput.mq4")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    outputStream.write(content.toByteArray())
                    outputStream.close()
                }
                Toast.makeText(context, "Файл сохранен в Загрузки", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Не удалось создать файл", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка сохранения: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
