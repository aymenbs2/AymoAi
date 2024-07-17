package com.aymendev.aymoai.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*

object DialogUtils {

    fun showSecurityReportDialog(project: Project,okText:String="Export PDF",title:String="AymoAi Security Issue Report", report: String,onOkClicked:()->Unit) {
        val dialog = object : DialogWrapper(project) {
            init {
                init()
                this.title = title
            }

            override fun createCenterPanel(): JComponent {
                return createReportPanel(report)
            }
            override fun createActions(): Array<Action> {
                val okAction = object : DialogWrapperAction(okText) {
                    override fun doAction(e: ActionEvent?) {
                        onOkClicked()
                        doOKAction()
                    }
                }
                return arrayOf(okAction, cancelAction)
            }

        }
        dialog.show()
    }

    private fun createReportPanel(report: String): JPanel {
        val panel = JPanel(BorderLayout())
        val titleLabel = JLabel("AymoAi Security Issue Report").apply {
            font = Font(font.name, Font.BOLD, 18)
            icon = AllIcons.General.Warning
            foreground = JBColor.RED
            horizontalAlignment = SwingConstants.CENTER
        }
        panel.add(titleLabel, BorderLayout.NORTH)
        val editorPane = JEditorPane("text/html", report).apply {
            isEditable = false
        }

        val scrollPane = JBScrollPane(editorPane).apply {
            preferredSize = Dimension(800, 600)
        }
        // Scroll to the top when the panel is first created
        SwingUtilities.invokeLater {
            scrollPane.verticalScrollBar.value = 0
        }

        val contentPanel = JPanel(VerticalLayout(10)).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            add(scrollPane)
        }
        panel.add(contentPanel, BorderLayout.CENTER)

        return panel
    }



}
