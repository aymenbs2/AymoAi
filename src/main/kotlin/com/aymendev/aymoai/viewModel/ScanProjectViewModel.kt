package com.aymendev.aymoai.viewModel

import com.aymendev.aymoai.util.FileHelper
import com.aymendev.aymoai.util.RequestHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.google.gson.Gson

class ScanProjectViewModel {

    fun convertToHtml(report: Map<String, String>, date: String): String {
        return report.entries.mapIndexed { index, entry ->
            val pageNumber = index + 1
            val (chunk, reportContent) = entry
            """
        <div class="container">
            <div class="header">
                <div class="page-number">Page $pageNumber</div>
            </div>
            <h1>Report</h1>
            <p>${reportContent.replace("\n", "<br/>")}</p>
            <pre><code>${chunk.replace("\n", "<br/>")}</code></pre>
        </div>
        """
        }.joinToString(
            separator = "",
            prefix = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>AymoAi Report</title>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    background-color: #f4f4f9;
                    margin: 0;
                    padding: 20px;
                }
                .header {
                    background-color: #333;
                    color: #fff;
                    padding: 10px 20px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                .header .page-number {
                    font-size: 14px;
                }
                .header .date {
                    font-size: 14px;
                }
                .container {
                    max-width: 800px;
                    margin: 20px auto;
                    background-color: #fff;
                    padding: 20px;
                    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                    border-radius: 8px;
                }
                h1 {
                    color: #333;
                }
                p {
                    line-height: 1.6;
                    color: #666;
                }
                pre {
                    background-color: #282c34;
                    color: #abb2bf;
                    padding: 10px;
                    border-radius: 5px;
                    overflow-x: auto;
                }
                code {
                    font-family: "Courier New", Courier, monospace;
                }
            </style>
        </head>
        <body>
        """.trimIndent(),
            postfix = """
               $date
         </body>
        </html>
        """.trimIndent()
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
            RequestHelper.processRequestSynchronously(
                client,
                request,
                results,
                requests.size,
                index,
                indicator,
                chunks[index]
            )
        }
        return results
    }


}
