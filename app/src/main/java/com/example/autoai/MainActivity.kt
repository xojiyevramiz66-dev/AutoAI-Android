package com.example.autoai

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
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

    private var lastAgentResponseText: String = ""

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            val path = selectedUri.toString().lowercase()

            if (path.endsWith(".zip")) {
                Toast.makeText(this, "Распаковываю и анализирую ZIP-архив...", Toast.LENGTH_SHORT).show()
                val zipAnalysis = unzipAndAnalyze(this, selectedUri)
                val prompt = "Я загрузил ZIP-архив с проектом. Вот его структура и содержимое:\n\n$zipAnalysis\n\nПроанализируй этот проект, перечисли ключевые компоненты и объясни, как они работают."
                sendQueryToAgent(prompt)
            } else {
                val fileContent = readTextFromUri(selectedUri)
                if (!fileContent.isNullOrBlank()) {
                    val prompt = "Проанализируй этот файл (код MQL4/текст) и объясни, что он делает:\n\n$fileContent"
                    sendQueryToAgent(prompt)
                    Toast.makeText(this, "Файл загружен, отправляю агенту...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val photoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { selectedUri ->
            Toast.makeText(this, "Фото выбрано, обрабатываю...", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val imageBytes = contentResolver.openInputStream(selectedUri)?.use { it.readBytes() }
                    
                    if (imageBytes != null) {
                        val prompt = "Я прикрепил скриншот/график. Проанализируй его."
                        val response = LlmClient.sendPrompt(prompt) 
                        lastAgentResponseText = response

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Ответ по фото получен!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Ошибка чтения фото: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } ?: run {
            Toast.makeText(this, "Фото не было выбрано", Toast.LENGTH_SHORT).show()
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

        btnSend.setOnClickListener {
            val userText = etInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                sendQueryToAgent(userText)
                etInput.setText("")
            }
        }

        btnAttachFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        btnAttachPhoto.setOnClickListener {
            photoPickerLauncher.launch(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        }

        btnSaveMq4.setOnClickListener {
            if (lastAgentResponseText.isNotBlank()) {
                saveMq4File(this, lastAgentResponseText, "AutoAdvisor.mq4")
            } else {
                Toast.makeText(this, "Нет полученного кода для сохранения", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendQueryToAgent(queryText: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = LlmClient.sendPrompt(queryText) 
                lastAgentResponseText = response

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ответ получен!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                    stringBuilder.toString()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка чтения: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun unzipAndAnalyze(context: Context, zipUri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(zipUri)?.let { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var zipEntry = zipInputStream.nextEntry

                while (zipEntry != null) {
                    val fileName = zipEntry.name
                    stringBuilder.append("📁 Файл: $fileName\n")

                    if (fileName.endsWith(".mq4", true) || fileName.endsWith(".txt", true)) {
                        val contentBytes = zipInputStream.readBytes()
                        val textContent = String(contentBytes, Charsets.UTF_8)
                        stringBuilder.append("Код:\n${textContent.take(300)}...\n\n")
                    }

                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
                zipInputStream.close()
            }
        } catch (e: Exception) {
            return "Ошибка распаковки: ${e.message}"
        }
        return stringBuilder.toString()
    }

    private fun saveMq4File(context: Context, codeText: String, fileName: String) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                outputStream?.use { it.write(codeText.toByteArray()) }
                Toast.makeText(context, "Файл сохранен в Загрузки!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Не удалось создать файл", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
