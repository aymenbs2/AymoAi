package com.aymendev.aymoai.util


import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.FileWriter
import java.util.regex.Pattern
import javax.swing.JFileChooser

object FileHelper {
    private val resourceExtensions = setOf(
        "png", "jpg", "jpeg", "gif", "bmp", "tiff", "ico", "svg",
        "mp3", "wav", "ogg", "flac", "aac",
        "mp4", "avi", "mkv", "mov", "wmv",
        "ttf", "otf", "woff", "woff2",
        "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"
    )

    private val externalLibDirs = setOf(
        "node_modules", "libs", "build", "out",
        ".idea",
        ".apt_generated",
        ".classpath",
        ".factorypath",
        ".project",
        ".settings",
        ".springBeans",
        ".sts4-cache"
    )

    fun exportReportToHtmlFile(
        htmlContent: String,
        fileName: String = "aymoai_security_report.html",
        project: Project
    ) {
        val fileChooser = JFileChooser()
        val desktopPath = System.getProperty("user.home") + File.separator + "Desktop"
        fileChooser.currentDirectory = File(desktopPath)
        fileChooser.dialogTitle = "Save HTML Report"
        fileChooser.selectedFile = File(fileName)
        val userSelection = fileChooser.showSaveDialog(null)
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            val fileToSave = fileChooser.selectedFile
            try {
                FileWriter(fileToSave).use { writer ->
                    writer.write(htmlContent)
                }
                Messages.showMessageDialog(
                    project,
                    "The security report has been exported to HTML.",
                    "Export Successful",
                    Messages.getInformationIcon()
                )
            } catch (e: Exception) {
                Messages.showMessageDialog(
                    project,
                    "Failed to save the HTML report: ${e.message}",
                    "Export Failed",
                    Messages.getErrorIcon()
                )
            }
        }
    }

    fun readGitignorePatterns(folder: VirtualFile): List<String> {
        val gitignoreFile = File(folder.path, ".gitignore")
        return if (gitignoreFile.exists()) {
            gitignoreFile.readLines().mapNotNull { line ->
                line.trim().takeIf { it.isNotEmpty() && !it.startsWith("#") }
                    ?.let { convertGitignorePatternToRegex(it) }
            }
        } else {
            emptyList()
        }
    }

    fun collectFiles(folder: VirtualFile, ignorePatterns: List<String>): MutableList<VirtualFile> {
        val files = mutableListOf<VirtualFile>()
        folder.children.forEach { file ->
            val relativePath = file.path.substringAfterLast("/")
            val extension = file.extension?.lowercase()
            if (file.isDirectory && file.name !in externalLibDirs) {
                files.addAll(collectFiles(file, ignorePatterns))
            } else if (extension !in resourceExtensions && ignorePatterns.none { relativePath.matches(it.toRegex()) }) {
                files.add(file)
            }
        }
        return files
    }

    fun createChunks(files: List<VirtualFile>): List<String> {
        val chunkSize = 5000
        return files.filter {
            it.name !in externalLibDirs
        }.flatMap { file ->
            val fileContent = file.contentsToByteArray().toString(Charsets.UTF_8)
            fileContent.chunked(chunkSize)
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
}
