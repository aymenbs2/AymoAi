package com.aymendev.aymoai.actions

import com.aymendev.aymoai.data.model.ProgramingLanguageFile
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import javax.swing.*

class RefactorCodeDialog(project: Project, private val refactoredCode: ProgramingLanguageFile) : DialogWrapper(project) {
    private val textArea: JTextArea = JTextArea(refactoredCode.content)

    init {
        init()
        title = "Refactor Code"
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        textArea.isEditable = false
        val scrollPane = com.intellij.ui.components.JBScrollPane(textArea)
        panel.add(scrollPane, BorderLayout.CENTER)
        return panel
    }

    override fun createActions(): Array<Action> {
        return arrayOf(
            myCancelAction.apply { putValue(Action.NAME, "Cancel") },
            myOKAction.apply { putValue(Action.NAME, "Refactor") }
        )
    }

    fun getRefactoredCode(): String {
        return textArea.text
    }
}
