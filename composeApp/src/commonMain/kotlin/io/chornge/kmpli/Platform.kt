package io.chornge.kmpli

import io.ktor.client.HttpClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

interface Platform {
    suspend fun httpGetBytes(url: String): ByteArray
    fun extractZip(zipBytes: ByteArray, projectName: String): String
    fun replacePlaceholders(dirPath: String, name: String, pid: String, oldPid: String)
    fun printLine(message: String)
    fun urlEncode(value: String): String
}

expect fun Platform(): Platform

expect fun NetClient(): HttpClient

@OptIn(ExperimentalForeignApi::class)
fun osName(): String = getenv("OSTYPE")?.toKString() ?: "unknown"
