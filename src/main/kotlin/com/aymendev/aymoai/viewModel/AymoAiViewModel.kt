package com.aymendev.aymoai.viewModel

import com.aymendev.aymoai.data.model.ProgramingLanguageFile
import com.aymendev.aymoai.network.MainAiServices
import com.aymendev.aymoai.util.Constants.ERROR_CODE_RAFACT
import com.aymendev.aymoai.util.Constants.ERROR_GENERATING_UNIT_TEST
import com.aymendev.aymoai.util.Constants.SUCCESS_UNIT_TEST_GENERATED
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil

class AymoAiViewModel(  apiKey: String) {
    private val mainAiServices = MainAiServices(apiKey)

    fun generateAndSaveUnitTest(
        file: VirtualFile,
        onCompletion: (Boolean, ProgramingLanguageFile?, String) -> Unit
    ) {
        val psiFileContent = ReadAction.compute<String, Throwable> {
            FileDocumentManager.getInstance().getDocument(file)?.text
        } ?: return

        mainAiServices.generateUnitTest(psiFileContent) { result ->
            ApplicationManager.getApplication().invokeLater {
                result.fold(
                    onSuccess = { unitTestContent ->
                        val tab = unitTestContent.split("///")
                        val programingLanguageFile = ProgramingLanguageFile(tab[1], tab[2], tab[3].replace("///${tab[1]}///${tab[2]};","").replace("```${tab[1]}",""))
                        onCompletion(true, programingLanguageFile, SUCCESS_UNIT_TEST_GENERATED)

                    },
                    onFailure = { error ->
                        onCompletion(false, null, "$ERROR_GENERATING_UNIT_TEST ${error.message}")
                    }
                )
            }
        }
    }


    fun chooseSavePathAndSaveFile(project: Project, virtualFile: VirtualFile, content: ProgramingLanguageFile) {
        val basePath = project.basePath ?: return
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath)
        val srcDir = baseDir?.findChild("src") ?: baseDir
        val descriptor = FileChooserDescriptor(
            false, // Choose directories only
            true,  // Allow choosing directories
            false,
            false,
            false,
            false
        ).withTitle("Select Directory to Save Unit Test")
            .withDescription("Select the directory where the generated unit test file should be saved")
            .withRoots(srcDir)
        FileChooser.chooseFile(descriptor, project, virtualFile) { selectedDirectory ->
            if (selectedDirectory != null) {
                saveFile(selectedDirectory, virtualFile, content)
                Messages.showMessageDialog(
                    project,
                    "Unit test saved successfully",
                    "Success",
                    Messages.getInformationIcon()
                )
            } else {
                Messages.showMessageDialog(
                    project,
                    "No directory selected. Unit test not saved.",
                    "Canceled",
                    Messages.getInformationIcon()
                )
            }
        }
    }

    private fun saveFile(selectedDirectory: VirtualFile, virtualFile: VirtualFile, file: ProgramingLanguageFile) {
        ApplicationManager.getApplication().runWriteAction {
            val testFile = selectedDirectory.findOrCreateChildData(
                this,
                "${virtualFile.nameWithoutExtension}Test.${file.fileExtension}"
            )
            VfsUtil.saveText(testFile, file.content.replace("```${file.name}", "").replace("```", ""))
        }
    }

     fun replaceSelection(editor: Editor, newText: String) {
        WriteCommandAction.runWriteCommandAction(editor.project) {
            val document = editor.document
            val selectionModel = editor.selectionModel
            val start = selectionModel.selectionStart
            val end = selectionModel.selectionEnd
            document.replaceString(start, end, newText)
        }
    }

    fun refactTheCode(req:String,file: String?, onCompletion: (Boolean, ProgramingLanguageFile?, String) -> Unit) {
        val psiFileContent = ReadAction.compute<String, Throwable> {
           file
        } ?: return

        mainAiServices.refactCode(req,psiFileContent) { result ->
            ApplicationManager.getApplication().invokeLater {
                result.fold(
                    onSuccess = { content ->

                        val tab = content.split("///")
                        val programingLanguageFile = ProgramingLanguageFile(tab[1], tab[2], tab[3].replace("///${tab[1]}///${tab[2]};","").replace("```${tab[1]}","").replace("```",""))
                        onCompletion(true, programingLanguageFile, SUCCESS_UNIT_TEST_GENERATED)
                    },
                    onFailure = { error ->
                        onCompletion(false,null, "$ERROR_CODE_RAFACT ${error.message}")
                    }
                )
            }
        }
    }

    fun updateFileContent(virtualFile: VirtualFile, content: ProgramingLanguageFile) {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        ApplicationManager.getApplication().runWriteAction {
            document?.setText(content.content)
        }
        FileDocumentManager.getInstance().saveDocument(document!!)
    }


}
