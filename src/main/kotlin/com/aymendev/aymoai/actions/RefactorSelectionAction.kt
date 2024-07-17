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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.aymendev.aymoai.config.Config


class RefactorSelectionAction : AnAction() {
    private var apiKey = Config.aymoApiKey
    private val viewModel = AymoAiViewModel(apiKey)

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val selectedText = editor?.selectionModel?.selectedText
        event.presentation.isEnabledAndVisible = !selectedText.isNullOrEmpty()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return

        if (apiKey.isBlank()) {
            Messages.showMessageDialog(
                "API Key not found in environment variables",
                "Error",
                Messages.getErrorIcon()
            )
            return
        }


        ProgressManager.getInstance().run(object : Task.Backgroundable(project, GENERATING_UNIT_TEST) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                indicator.text = RAFACTORING
                viewModel.refactTheCode(
                    Config.refactorSelectedCodeRq,
                    selectedText
                ) { success, result, message ->
                    ApplicationManager.getApplication().invokeLater {
                        if (success) {
                            if (result != null) {
                                showRefactorDialog(project, editor, result)
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


    private fun showRefactorDialog(project: Project, editor: Editor, refactoredCode: ProgramingLanguageFile) {
        val dialog = RefactorCodeDialog(project, refactoredCode)
        if (dialog.showAndGet()) {
            // User clicked "Refactor"
            viewModel.replaceSelection(editor, refactoredCode.content)
            Messages.showMessageDialog(
                project,
                "Code refactored successfully",
                SUCCESS_TITLE,
                Messages.getInformationIcon()
            )
        }
    }
}
