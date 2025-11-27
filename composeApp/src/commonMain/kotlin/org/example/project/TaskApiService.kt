package org.example.project

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// API response structure - this gets serialized from JSON
@Serializable
data class TaskAnalysisResponse(
    val taskType: String,
    val estimatedDuration: Int,
    val mentalLoad: String,
    val deadline: String? = null,
    val priority: Float,
    val reasoning: String
)

// Internal app data - NOT serialized
data class TaskData(
    val description: String,
    val taskType: String,
    val estimatedDuration: Int,
    val mentalLoad: String,
    val deadline: String? = null,
    val priority: Float,
    val reasoning: String
)

@Serializable
data class AnthropicRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<AnthropicMessage>
)

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)

@Serializable
data class AnthropicResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: List<ContentBlock>,
    val model: String? = null,
    val stop_reason: String? = null,
    val stop_sequence: String? = null,
    val usage: Usage? = null
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String
)

@Serializable
data class Usage(
    val input_tokens: Int? = null,
    val output_tokens: Int? = null
)

@Serializable
data class ErrorResponse(
    val type: String? = null,
    val error: ErrorDetail? = null
)

@Serializable
data class ErrorDetail(
    val type: String? = null,
    val message: String? = null
)

class TaskApiService {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    // API_KEY in local.properties in form: API_KEY=your api key
    private val apiKey = BuildConfig.API_KEY

    suspend fun analyzeTask(taskDescription: String): TaskData {
        val prompt = buildPrompt(taskDescription)

        try {
            val response: HttpResponse = client.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(AnthropicRequest(
                    model = "claude-sonnet-4-5",
                    max_tokens = 1024,
                    messages = listOf(
                        AnthropicMessage(
                            role = "user",
                            content = prompt
                        )
                    )
                ))
            }

            val responseBody = response.bodyAsText()
            println("API Response: $responseBody")

            if (response.status.value >= 400) {
                val errorResponse = try {
                    json.decodeFromString<ErrorResponse>(responseBody)
                } catch (e: Exception) {
                    null
                }
                throw Exception(errorResponse?.error?.message ?: "API Error: ${response.status}")
            }

            val anthropicResponse: AnthropicResponse = json.decodeFromString(responseBody)
            val responseText = anthropicResponse.content.firstOrNull()?.text
                ?: throw Exception("Empty response from API")

            return parseTaskData(taskDescription, responseText)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun buildPrompt(taskDescription: String): String {
        return """
Analyze this task and extract structured information: "$taskDescription"

Provide your analysis in this exact JSON format:
{
  "taskType": "one of: work, study, personal, household, creative, exercise, social",
  "estimatedDuration": number in minutes (realistic estimate),
  "mentalLoad": "low, medium, or high",
  "deadline": "if mentioned, in format YYYY-MM-DD HH:mm, otherwise null",
  "priority": number between 0.0 and 1.0,
  "reasoning": "brief explanation of your analysis"
}

Consider:
- Mental load: How much focus/energy does this require?
- Duration: Be realistic - include breaks for longer tasks
- Priority: Based on urgency, importance, and mental load
- Task type: Categorize appropriately

Respond ONLY with the JSON, no other text.
        """.trimIndent()
    }

    private fun parseTaskData(originalDescription: String, apiResponse: String): TaskData {
        return try {
            val jsonStart = apiResponse.indexOf("{")
            val jsonEnd = apiResponse.lastIndexOf("}") + 1

            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                throw Exception("No JSON found in response")
            }

            val jsonStr = apiResponse.substring(jsonStart, jsonEnd)
            val parsed = json.decodeFromString<TaskAnalysisResponse>(jsonStr)

            TaskData(
                description = originalDescription,
                taskType = parsed.taskType,
                estimatedDuration = parsed.estimatedDuration,
                mentalLoad = parsed.mentalLoad,
                deadline = parsed.deadline,
                priority = parsed.priority,
                reasoning = parsed.reasoning
            )
        } catch (e: Exception) {
            println("Parse error: ${e.message}")
            TaskData(
                description = originalDescription,
                taskType = "personal",
                estimatedDuration = 30,
                mentalLoad = "medium",
                deadline = null,
                priority = 0.5f,
                reasoning = "Failed to parse API response: ${e.message}"
            )
        }
    }

    fun formatTaskSummary(task: TaskData): String {
        val durationStr = if (task.estimatedDuration >= 60) {
            "${task.estimatedDuration / 60}h ${task.estimatedDuration % 60}m"
        } else {
            "${task.estimatedDuration}m"
        }

        return buildString {
            appendLine("📋 Task Analysis:")
            appendLine("• Type: ${task.taskType}")
            appendLine("• Duration: $durationStr")
            appendLine("• Mental Load: ${task.mentalLoad}")
            appendLine("• Priority: ${"%.0f".format(task.priority * 100)}%")
            if (task.deadline != null) {
                appendLine("• Deadline: ${task.deadline}")
            }
            appendLine("\n💡 ${task.reasoning}")
        }
    }
}