package com.aymendev.aymoai.viewModel

import com.aymendev.aymoai.util.FileHelper
import com.aymendev.aymoai.util.RequestHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.google.gson.Gson

class ScanProjectViewModel {
    fun convertToHtml(report: Map<String, String>): String {
        return report.entries.mapIndexed { index, entry ->
            val pageNumber = index + 1
            val (chunk, reportContent) = entry
            """
        <div class="page">
            <div class="page-header">Page $pageNumber</div>
            <div class="info-code">
                <pre>
                    <code>
                        ${chunk.replace("\n", "<br/>")}
                    </code>
                </pre>
            </div>
            <div class="info-paragraph">
                <h2>Report</h2>
                ${reportContent.replace("\n", "<br/>")}
            </div>
        </div>
        """
        }.joinToString(
            separator = "",
            prefix = """
            <html>
            <style>
                html, body {
                    font-family: Verdana, sans-serif;
                    background-color: #37878D;
                    font-size: 12px;
                    line-height: 1.5;
                }
                .info-paragraph {
                    background-color: #37878D;
                    border-radius: 15px;
                    border-left: 5px solid #F89494;
                    padding: 5px;
                    font-family: Arial, sans-serif;
                    font-size: 1.1em;
                    line-height: 1.6;
                    color: white;
                }
                .info-paragraph h2 {
                    font-size: 1.5em;
                    color: #FF8949;
                    margin-top: 0;
                }
                h1 {
                    text-align: left;
                }
                p {
                    text-indent: 50px;
                    text-align: justify;
                    letter-spacing: 3px;
                    border-radius: 15px;
                }
                code {
                    font-family: monospace;
                    color: white;
                    border-radius: 15px;
                }
                pre {
                    background-color: #2A7A81;
                    border: thick double #32a1ce;
                    border-radius: 15px;
                    font-size: 0.8em;
                    margin: 15px;
                }
                .page-header {
                    position: fixed;
                    top: 0;
                    width: 100%;
                    text-align: right;
                    background-color: #F89494;
                    color: white;
                    padding: 5px 10px;
                    font-size: 0.8em;
                }
                .page {
                    page-break-after: always;
                }
            </style>
            <body>
        """.trimIndent(),
            postfix = "</body></html>"
        )
    }

    fun scanFolder(folder: VirtualFile, indicator: ProgressIndicator): MutableList<Pair<String, String>> {
        val gson = Gson()
        val gitignorePatterns = FileHelper.readGitignorePatterns(folder)
        val files = FileHelper.collectFiles(folder, gitignorePatterns)
        val chunks = FileHelper.createChunks(files)
        val requests = RequestHelper.createRequests(chunks)
        val client = RequestHelper.createHttpClientBasedOnFileSize(chunks.sumOf { it.length }.toLong())

        val results = mutableListOf<Pair<String, String>>()
        requests.forEachIndexed { index, requestBodyMap ->
            val requestBodyJson = gson.toJson(requestBodyMap)
            val request = RequestHelper.buildRequest(requestBodyJson)
            RequestHelper.processRequestSynchronously(client, request, results, requests.size, index, indicator, chunks[index])
        }
        return results
    }

     fun formatReport(results: List<Pair<String, String>>): String {
        val stringBuilder = StringBuilder()
        results.forEach { (chunk, report) ->
            stringBuilder.append("Code:\n$chunk\n\n")
            stringBuilder.append("Report:\n$report\n")
            stringBuilder.append("---------------------------------------------------\n")
        }
        return stringBuilder.toString()
    }
}
