package com.aymendev.aymoai.actions

import com.aymendev.aymoai.config.Config
import com.aymendev.aymoai.util.ClipboardUtils
import com.aymendev.aymoai.util.DialogUtils
import com.aymendev.aymoai.util.FileHelper
import com.aymendev.aymoai.viewModel.ScanProjectViewModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.actionSystem.CommonDataKeys

class ScanProjectAction : AnAction() {
    private val apiKey = Config.aymoApiKey
    private val viewModel = ScanProjectViewModel()

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (apiKey.isBlank()) {
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

        val title = "Scanning Folder for Security Issues AymoAi"
        val canBeCancelled = true

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            val indicator = ProgressManager.getInstance().progressIndicator
            indicator.text = "Scanning Folder..."
            val report = viewModel.scanFolder(selectedFolder, indicator)
            ApplicationManager.getApplication().invokeLater {
                val reportHtml = viewModel.convertToHtml(report.toMap())
                DialogUtils.showSecurityReportDialog(
                    okText = "Export HTML",
                    project = project,
                    report = reportHtml
                ) {
                    FileHelper.exportReportToHtmlFile(htmlContent = reportHtml, project = project)
                }
            }
        }, title, canBeCancelled, project)
    }

}
