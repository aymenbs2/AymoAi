package com.aymendev.aymoai.actions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile


import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import okhttp3.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.*
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.border.EmptyBorder

class ScanProjectAction : AnAction() {
    private val apiKey = System.getenv("AYMOAPI_KEY")

    // Define resource file extensions to be skipped
    private val resourceExtensions = setOf(
        "png", "jpg", "jpeg", "gif", "bmp", "tiff", "ico", "svg", // Images
        "mp3", "wav", "ogg", "flac", "aac", // Audio
        "mp4", "avi", "mkv", "mov", "wmv", // Video
        "ttf", "otf", "woff", "woff2", // Fonts
        "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx" // Documents
    )

    // Define directories to be skipped
    private val externalLibDirs = setOf(
        "node_modules", "libs", "build", "out"
    )

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        if (apiKey.isNullOrBlank()) {
            Messages.showMessageDialog(
                "OpenAI API Key not found in environment variables",
                "Error",
                Messages.getErrorIcon()
            )
            return
        }

        val selectedFolder = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (!selectedFolder.isDirectory) {
            Messages.showMessageDialog("Please select a folder to scan", "Error", Messages.getErrorIcon())
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Scanning Folder for Security Issues") {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Scanning Folder..."
                scanFolder(selectedFolder, indicator) { report ->
                    ApplicationManager.getApplication().invokeLater {
                        showSecurityReportDialog(project, report)
                    }
                }
            }
        })
    }

    private fun scanFolder(folder: VirtualFile, indicator: ProgressIndicator, callback: (String) -> Unit) {
        val client = OkHttpClient()
        val gson = Gson()

        // Read .gitignore file
        val gitignoreFile = File(folder.path, ".gitignore")
        val gitignorePatterns = if (gitignoreFile.exists()) {
            gitignoreFile.readLines().mapNotNull { line ->
                line.trim().takeIf { it.isNotEmpty() && !it.startsWith("#") }?.let {
                    convertGitignorePatternToRegex(it)
                }
            }
        } else {
            emptyList()
        }

        val files = mutableListOf<VirtualFile>()
        collectFiles(folder, gitignorePatterns, files)

        val chunkSize = 5000 // Approximate number of characters per chunk
        val chunks = files.flatMap { file ->
            val fileContent = file.contentsToByteArray().toString(Charsets.UTF_8)
            fileContent.chunked(chunkSize)
        }

        // Create request payloads for each chunk
        val requests = chunks.map { chunk ->
            mapOf(
                "model" to "gpt-3.5-turbo",
                "messages" to listOf(
                    mapOf(
                        "role" to "system",
                        "content" to "You are a security expert."
                    ),
                    mapOf(
                        "role" to "user",
                        "content" to "Analyze the following code for security vulnerabilities and potential bugs with details like 'on FileName theire is blala this can blala' in the final draw chart of security:\n$chunk"
                    )
                )
            )
        }

        // Process each chunk individually with retry mechanism
        val results = mutableListOf<String>()
        requests.forEachIndexed { index, requestBodyMap ->
            val requestBodyJson = gson.toJson(requestBodyMap)
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(requestBodyJson.toRequestBody("application/json".toMediaTypeOrNull()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()


            val initialBackoff = 1000L // initial backoff duration in milliseconds
            retryWithBackoff(client, request, initialBackoff, 5) { response, error ->
                response?.use { res ->
                    if (res.isSuccessful) {
                        val responseBody = res.body?.string()
                        if (responseBody != null) {
                            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                            val choices = jsonResponse.getAsJsonArray("choices")
                            val report = choices[0].asJsonObject
                                .getAsJsonObject("message")
                                .get("content").asString
                            synchronized(results) {
                                results.add(report)
                                if (results.size == requests.size) {
                                    callback(results.joinToString("\n\n"))
                                }
                            }
                        }
                    } else {

                        // Rate limit exceeded

                        ApplicationManager.getApplication().invokeLater {
                            Messages.showMessageDialog(
                                null,
                                "Failed to scan project: ${res.message}",
                                "Error",
                                Messages.getErrorIcon()
                            )
                        }
                    }
                }

                if (error != null) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showMessageDialog(
                            null,
                            "Failed to scan project: ${error.message}",
                            "Error",
                            Messages.getErrorIcon()
                        )
                    }
                }
            }

            // Update progress indicator
            indicator.fraction = (index + 1).toDouble() / requests.size
            indicator.text = "Scanning file chunk ${index + 1} of ${requests.size}..."

            // Update progress indicator
            indicator.fraction = (index + 1).toDouble() / requests.size
            indicator.text = "Scanning file chunk ${index + 1} of ${requests.size}..."
        }
    }

    private fun convertGitignorePatternToRegex(pattern: String): String {
        var regex = Pattern.quote(pattern)
        regex = regex.replace("\\*\\*".toRegex(), ".*")
        regex = regex.replace("\\*".toRegex(), "[^/]*")
        if (pattern.endsWith("/")) {
            regex = "$regex.*"
        }
        return regex
    }

    private fun retryWithBackoff(
        client: OkHttpClient,
        request: Request,
        backoff: Long,
        maxRetries: Int,
        callback: (Response?, IOException?) -> Unit
    ) {
        var attempt = 0

        fun makeRequest() {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (attempt < maxRetries) {
                        attempt++
                        Thread.sleep(backoff * attempt)
                        makeRequest()
                    } else {
                        callback(null, e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        callback(response, null)
                    } else if (response.code == 429 && attempt < maxRetries) { // Rate limit exceeded
                        attempt++
                        Thread.sleep(backoff * attempt)
                        makeRequest()
                    } else {
                        callback(response, IOException("Unexpected response code: ${response.code}"))
                    }
                }
            })
        }

        makeRequest()
    }

    private fun showSecurityReportDialog(project: Project, report: String) {
        val dialog = object : DialogWrapper(project) {
            init {
                init()
                title = "Security Report"
            }

            override fun createCenterPanel(): JComponent? {
                val panel = JPanel(BorderLayout())

                // Title
                val titleLabel = JLabel("Security Issues Report")
                titleLabel.font = Font(titleLabel.font.name, Font.BOLD, 18)
                titleLabel.icon = AllIcons.General.Warning
                titleLabel.foreground = JBColor.RED
                titleLabel.horizontalAlignment = SwingConstants.CENTER
                panel.add(titleLabel, BorderLayout.NORTH)

                // Report content with HTML formatting
                val htmlContent = convertToHtml(report)
                val editorPane = JEditorPane("text/html", htmlContent)
                editorPane.isEditable = false
                editorPane.background = JBColor.WHITE

                // Scroll pane
                val scrollPane = JBScrollPane(editorPane)
                scrollPane.preferredSize = Dimension(800, 600)

                // Content panel with padding
                val contentPanel = JPanel(VerticalLayout(10))
                contentPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
                contentPanel.add(scrollPane)

                panel.add(contentPanel, BorderLayout.CENTER)

                // Buttons
                val buttonPanel = JPanel()
                val closeButton = JButton("Close")
                closeButton.addActionListener { close(OK_EXIT_CODE) }
                buttonPanel.add(closeButton)

                panel.add(buttonPanel, BorderLayout.SOUTH)

                return panel
            }
        }

        dialog.show()
    }

    private fun convertToHtml(report: String): String {
        val lines = report.split("\n")
        val stringBuilder = StringBuilder()
        stringBuilder.append("<html><body>")
        lines.forEach { line ->
            stringBuilder.append("<p>").append(line).append("</p>")
        }
        stringBuilder.append("</body></html>")
        return stringBuilder.toString()
    }


    private fun collectFiles(folder: VirtualFile, ignorePatterns: List<String>, files: MutableList<VirtualFile>) {
        folder.children.forEach { file ->
            val relativePath = file.path.substringAfterLast("/")
            val extension = file.extension?.lowercase()
            if (file.isDirectory && file.name !in externalLibDirs) {
                collectFiles(file, ignorePatterns, files)
            } else if (extension !in resourceExtensions && ignorePatterns.none { relativePath.matches(it.toRegex()) }) {
                files.add(file)
            }
        }
    }
}