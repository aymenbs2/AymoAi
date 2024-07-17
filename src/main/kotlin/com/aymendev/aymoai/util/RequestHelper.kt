package com.aymendev.aymoai.util

import com.aymendev.aymoai.config.Config
import com.aymendev.aymoai.util.Constants.SYSTEM_ROLE
import com.aymendev.aymoai.util.Constants.USER_ROLE
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.Logger
import okhttp3.*
import com.google.gson.JsonParser
import com.intellij.openapi.progress.ProgressIndicator
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object RequestHelper {
    private val logger = Logger.getInstance(RequestHelper::class.java)

    fun createHttpClientBasedOnFileSize(fileSize: Long): OkHttpClient {
        val timeout = (fileSize / 1024).coerceAtLeast(10).toInt()
        return OkHttpClient.Builder()
            .connectTimeout(timeout.toLong(), java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(timeout.toLong(), java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(timeout.toLong(), java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    fun createRequests(chunks: List<String>): List<Map<String, Any>> {
        return chunks.map { chunk ->
            mapOf(
                "model" to Config.model,
                "messages" to listOf(
                    mapOf("role" to SYSTEM_ROLE, "content" to Config.scanRole),
                    mapOf(
                        "role" to USER_ROLE, "content" to
                                "${Config.scanRequest}\n$chunk"
                    )
                )
            )
        }
    }

    fun buildRequest(requestBodyJson: String): Request {
        return Request.Builder()
            .url(Config.baseUrl)
            .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${Config.aymoApiKey}")
            .build()
    }

    fun processRequestSynchronously(
        client: OkHttpClient,
        request: Request,
        results: MutableList<Pair<String, String>>,
        totalRequests: Int,
        index: Int,
        indicator: ProgressIndicator,
        chunk: String
    ) {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                    val choices = jsonResponse.getAsJsonArray("choices")
                    val report = choices[0].asJsonObject
                        .getAsJsonObject("message")
                        .get("content").asString
                    synchronized(results) {
                        results.add(Pair(chunk, report))
                    }
                }
            } else {
                handleFailure(IOException("Failed to scan project: ${response.message}"))
            }
        } catch (e: IOException) {
            handleFailure(e)
        }

        // Update progress indicator
        indicator.fraction = (index + 1).toDouble() / totalRequests
        indicator.text = "Scanning file ${index + 1} of $totalRequests..."
    }

    private fun handleFailure(e: IOException) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showMessageDialog("Failed to scan project: ${e.message}", "Error", Messages.getErrorIcon())
            logger.error("Request failed", e)
        }
    }
}
