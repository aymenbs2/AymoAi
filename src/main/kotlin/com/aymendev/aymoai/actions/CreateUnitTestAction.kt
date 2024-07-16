package com.aymendev.aymoai.actions

import com.aymendev.aymoai.config.Config
import com.aymendev.aymoai.util.Constants.ERROR_NO_FILE_SELECTED
import com.aymendev.aymoai.util.Constants.ERROR_TITLE
import com.aymendev.aymoai.util.Constants.GENERATING_UNIT_TEST
import com.aymendev.aymoai.viewModel.AymoAiViewModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task


class CreateUnitTestAction : AnAction() {
    private var apiKey = Config.aymoApiKey
    private val viewModel = AymoAiViewModel(apiKey)

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE)

        if (file == null) {
            Messages.showMessageDialog(project, ERROR_NO_FILE_SELECTED, ERROR_TITLE, Messages.getErrorIcon())
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, GENERATING_UNIT_TEST) {
            override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                indicator.text = GENERATING_UNIT_TEST
                println("generatiinf unut test")
                viewModel.generateAndSaveUnitTest(file) { success,resultFile, message ->
                    ApplicationManager.getApplication().invokeLater {
                        println("finish generatiinf unut test")

                        if (success) {
                            if (resultFile != null) {
                                viewModel.chooseSavePathAndSaveFile(project ,file,resultFile)
                            }
                            else
                                Messages.showMessageDialog(project, message, ERROR_TITLE, Messages.getErrorIcon())

                        } else {
                            Messages.showMessageDialog(project, message, ERROR_TITLE, Messages.getErrorIcon())
                        }
                    }
                }
            }
        })
    }
}
