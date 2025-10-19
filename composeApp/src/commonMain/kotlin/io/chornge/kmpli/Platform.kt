package io.chornge.kmpli

import io.ktor.client.HttpClient

interface Platform {
    suspend fun httpGetBytes(url: String): ByteArray
    fun extractZip(zipBytes: ByteArray, projectName: String): String
    fun replacePlaceholders(dirPath: String, name: String, pid: String, oldPid: String)
    fun printLine(message: String)
    fun urlEncode(value: String): String
}

expect fun Platform(): Platform

expect fun HttpClient(): HttpClient
