package com.aymendev.aymoai.util


import java.awt.datatransfer.StringSelection
import java.awt.Toolkit

object ClipboardUtils {
    fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }
}
