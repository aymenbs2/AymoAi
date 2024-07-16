package com.aymendev.aymoai.actions

import com.aymendev.aymoai.data.model.ProgramingLanguageFile
import com.aymendev.aymoai.util.Constants.ERROR_NO_FILE_SELECTED
import com.aymendev.aymoai.util.Constants.ERROR_TITLE
import com.aymendev.aymoai.util.Constants.GENERATING_UNIT_TEST
import com.aymendev.aymoai.util.Constants.RAFACTORING
import com.aymendev.aymoai.util.Constants.SUCCESS_TITLE
import com.aymendev.aymoai.viewModel.AymoAiViewModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

import com.aymendev.aymoai.config.Config
import com.intellij.openapi.application.ReadAction


class RefactCodeAction : AnAction() {
    private var apiKey = Config.aymoApiKey
    private val viewModel = AymoAiViewModel(apiKey)


    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)

        val fileContent = ReadAction.compute<String?, Throwable> {
            file.let {
                if (it != null) {
                    FileDocumentManager.getInstance().getDocument(it)?.text
                } else ""
            }
        }
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, GENERATING_UNIT_TEST) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                indicator.text = RAFACTORING
                viewModel.refactTheCode(Config.refactorCodeRq, fileContent) { success, result, message ->
                    ApplicationManager.getApplication().invokeLater {
                        if (success) {
                            if (result != null && file != null) {
                                showRefactorDialog(project, file, result)
                            } else
                                Messages.showMessageDialog(project, message, ERROR_TITLE, Messages.getErrorIcon())

                        } else {
                            Messages.showMessageDialog(project, message, ERROR_TITLE, Messages.getErrorIcon())
                        }
                    }
                }
            }
        })
    }


    private fun showRefactorDialog(project: Project, file: VirtualFile, refactoredCode: ProgramingLanguageFile) {
        val dialog = RefactorCodeDialog(project, refactoredCode)
        if (dialog.showAndGet()) {
            // User clicked "Refactor"
            viewModel.updateFileContent(file, refactoredCode)
            Messages.showMessageDialog(
                project,
                "Code refactored successfully",
                SUCCESS_TITLE,
                Messages.getInformationIcon()
            )
        }
    }
}
