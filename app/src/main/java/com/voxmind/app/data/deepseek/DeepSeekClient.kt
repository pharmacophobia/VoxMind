package com.voxmind.app.data.deepseek

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.voxmind.app.data.models.AutoSortedCategory
import com.voxmind.app.data.models.ChatMessage
import com.voxmind.app.data.models.ExtractedReminderItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeepSeekClient(
    private val apiKeyProvider: () -> String,
    private val modelProvider: () -> String = { "deepseek-chat" }
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val endpoint = "https://api.deepseek.com/v1/chat/completions"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = executeChatCompletion(
                systemPrompt = "You are VoxMind assistant.",
                userPrompt = "Respond with 'Connection successful!'",
                model = modelProvider()
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun organizeThoughts(transcript: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val system = """
                You are VoxMind AI, an expert cognitive organizer and thought structurer. 
                The user has spoken a freeflow stream of consciousness transcription.
                Structure this raw transcript into clear, elegant, actionable markdown:
                ### 💡 Core Theme & Intent
                ### 🧠 Structured Thoughts & Insights (group related points)
                ### 🎯 Key Decisions & Action Items
                Keep the tone sharp, professional, and easy to skim.
            """.trimIndent()
            val result = executeChatCompletion(system, transcript, modelProvider())
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateBulletPoints(transcript: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val system = """
                You are VoxMind AI. Extract an actionable, concise bullet point list from the transcript.
                Output ONLY bullet points starting with '- '. 
                Do NOT include introductory phrases, markdown headers, or explanations.
                Each line must be a single actionable task or key point.
            """.trimIndent()
            val raw = executeChatCompletion(system, transcript, modelProvider())
            val bullets = raw.lines()
                .map { it.trim() }
                .filter { it.startsWith("-") || it.startsWith("*") || it.startsWith("•") }
                .map { it.removePrefix("-").removePrefix("*").removePrefix("•").trim() }
                .filter { it.isNotBlank() }

            if (bullets.isEmpty()) {
                // fallback split by lines
                val fallback = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
                Result.success(fallback)
            } else {
                Result.success(bullets)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun summarize(transcript: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val system = """
                You are VoxMind AI. Provide a high-impact executive summary of the spoken transcript:
                **TL;DR**: 1-2 sentence core message.
                
                **Key Takeaways**:
                - Key point 1
                - Key point 2
                - Key point 3
                
                **Context & Implications**: Brief summary of tone, timeline, and stakes.
            """.trimIndent()
            val result = executeChatCompletion(system, transcript, modelProvider())
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractReminders(
        transcript: String,
        currentTimeContext: String = ""
    ): Result<List<ExtractedReminderItem>> = withContext(Dispatchers.IO) {
        try {
            val nowContext = currentTimeContext.ifBlank {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm (EEEE)", Locale.getDefault())
                sdf.format(Date())
            }

            val system = """
                You are VoxMind AI, an autonomous scheduling agent powered by DeepSeek.
                Analyze the spoken transcript or written note and detect ALL reminders, deadlines, alarms, commitments, or scheduled tasks mentioned.
                Current Reference Time: $nowContext
                
                For EACH detected item, extract:
                1. "taskTitle": string — concise, actionable title (e.g. "Doctor Appointment", "Call Sarah", "Submit Taxes").
                2. "detectedDateOrTime": string — natural language time description (e.g. "Tomorrow at 2:00 PM", "in 30 minutes", "Friday 5 PM").
                3. "delayMinutes": integer — minutes from right now until the reminder should trigger.
                   - Compute based on Current Reference Time: $nowContext.
                   - "in 20 minutes" -> 20; "in 2 hours" -> 120; "tomorrow at this same time" -> 1440; "tonight at 8pm" -> calculate minutes between now and 8pm.
                   - If exact time cannot be determined, default to 60.
                4. "notes": string — context, details, or reasons mentioned in speech.
                5. "sendSms": boolean — set to true if the user mentions texting/SMS (e.g. "text me", "send text", "SMS mom", "text 555-1234"), OR if a phone number is detected.
                6. "smsRecipientPhone": string — phone number if mentioned, or "" if meant for default/self.
                7. "sendEmail": boolean — set to true if the user mentions emailing (e.g. "email me", "send email reminder", "email user@example.com"), OR if an email address is detected.
                8. "emailRecipient": string — email address if mentioned, or "" if meant for default/self.

                Return strictly a JSON array of objects with the schema:
                [
                  {
                    "taskTitle": "Call Sarah",
                    "detectedDateOrTime": "Today at 5:00 PM",
                    "delayMinutes": 120,
                    "notes": "Discuss project launch",
                    "sendSms": true,
                    "smsRecipientPhone": "555-0199",
                    "sendEmail": false,
                    "emailRecipient": ""
                  }
                ]
                Return ONLY the JSON array without markdown backticks or commentary. If none found, return [].
            """.trimIndent()
            val raw = executeChatCompletion(system, transcript, modelProvider())
            val cleanJson = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JsonParser.parseString(cleanJson).asJsonArray
            val items = mutableListOf<ExtractedReminderItem>()
            for (elem in jsonArray) {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    val title = obj.get("taskTitle")?.asString ?: "Reminder"
                    val time = obj.get("detectedDateOrTime")?.asString ?: "Later"
                    val notes = obj.get("notes")?.asString ?: ""
                    val delayMins = if (obj.has("delayMinutes") && !obj.get("delayMinutes").isJsonNull) {
                        try { obj.get("delayMinutes").asLong } catch (_: Exception) { null }
                    } else null
                    val sendSms = obj.get("sendSms")?.asBoolean ?: false
                    val smsPhone = obj.get("smsRecipientPhone")?.asString ?: ""
                    val sendEmail = obj.get("sendEmail")?.asBoolean ?: false
                    val emailRecip = obj.get("emailRecipient")?.asString ?: ""

                    items.add(
                        ExtractedReminderItem(
                            taskTitle = title,
                            detectedDateOrTime = time,
                            notes = notes,
                            delayMinutes = delayMins,
                            sendSms = sendSms,
                            smsRecipientPhone = smsPhone,
                            sendEmail = sendEmail,
                            emailRecipient = emailRecip
                        )
                    )
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun autoSortWritingIntoLists(
        rawWritings: List<String>,
        existingListTitles: List<String>
    ): Result<List<AutoSortedCategory>> = withContext(Dispatchers.IO) {
        try {
            if (rawWritings.isEmpty()) return@withContext Result.success(emptyList())

            val combinedWriting = rawWritings.mapIndexed { idx, w -> "--- Note ${idx + 1} ---\n$w" }.joinToString("\n\n")
            val existingTitlesFormatted = if (existingListTitles.isEmpty()) "None yet" else existingListTitles.joinToString(", ")

            val system = """
                You are VoxMind's Autonomous Knowledge Classifier and Data Sorter powered by DeepSeek.
                Your mission is to read through the user's freeform writings, transcriptions, and thought streams, 
                and intelligently extract and categorize every meaningful task, idea, purchase, note, reminder, or data item into relevant lists.
                
                Existing User Lists: [$existingTitlesFormatted]
                
                RULES:
                1. If an extracted item fits naturally into one of the existing lists (e.g. food items -> "Groceries", bug fixes -> "Work Tasks"), route it to that existing list title.
                2. If it represents a new domain or topic (e.g. "Hardware Projects", "Book & Movie Recommendations", "Fitness Goals", "Key Contacts & Followups"), generate an intuitive, concise new list title.
                3. Each extracted item should be self-contained, clean, and actionable (remove conversational filler like "um", "I should probably", "remind me to").
                4. Return strictly a JSON array of objects with the schema:
                   [
                     {
                       "listTitle": "Category Name",
                       "items": ["Item 1", "Item 2"]
                     }
                   ]
                5. Return ONLY the JSON array without markdown formatting or surrounding explanation.
            """.trimIndent()

            val raw = executeChatCompletion(system, combinedWriting, modelProvider())
            val cleanJson = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JsonParser.parseString(cleanJson).asJsonArray
            val categories = mutableListOf<AutoSortedCategory>()
            for (elem in jsonArray) {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    val title = obj.get("listTitle")?.asString?.trim() ?: continue
                    val itemsArr = obj.getAsJsonArray("items") ?: continue
                    val items = mutableListOf<String>()
                    for (it in itemsArr) {
                        val str = it.asString.trim()
                        if (str.isNotBlank()) {
                            items.add(str)
                        }
                    }
                    if (title.isNotBlank() && items.isNotEmpty()) {
                        categories.add(AutoSortedCategory(listTitle = title, items = items))
                    }
                }
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chat(messages: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JsonObject().apply {
                addProperty("model", modelProvider())
                val msgsArray = JsonArray()
                msgsArray.add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", "You are VoxMind AI, an intelligent personal assistant powered by DeepSeek. You help organize thoughts, schedule alarms/reminders, manage task lists, and refine voice transcriptions.")
                })
                for (m in messages) {
                    msgsArray.add(JsonObject().apply {
                        addProperty("role", m.role)
                        addProperty("content", m.content)
                    })
                }
                add("messages", msgsArray)
                addProperty("temperature", 0.7)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${apiKeyProvider()}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    throw IOException("DeepSeek API error ${response.code}: $errBody")
                }
                val respBody = response.body?.string() ?: throw IOException("Empty response from DeepSeek")
                val jsonResp = JsonParser.parseString(respBody).asJsonObject
                val choices = jsonResp.getAsJsonArray("choices")
                if (choices != null && choices.size() > 0) {
                    val msg = choices[0].asJsonObject.getAsJsonObject("message")
                    val content = msg.get("content")?.asString ?: ""
                    Result.success(content)
                } else {
                    Result.failure(IOException("No choices returned from DeepSeek"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeChatCompletion(systemPrompt: String, userPrompt: String, model: String): String {
        val payload = JsonObject().apply {
            addProperty("model", model)
            val msgsArray = JsonArray()
            msgsArray.add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            })
            msgsArray.add(JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", userPrompt)
            })
            add("messages", msgsArray)
            addProperty("temperature", 0.5)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${apiKeyProvider()}")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw IOException("DeepSeek API error ${response.code}: $errBody")
            }
            val respBody = response.body?.string() ?: throw IOException("Empty response")
            val jsonResp = JsonParser.parseString(respBody).asJsonObject
            val choices = jsonResp.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val msg = choices[0].asJsonObject.getAsJsonObject("message")
                return msg.get("content")?.asString ?: ""
            } else {
                throw IOException("No content returned in DeepSeek choice")
            }
        }
    }
}
