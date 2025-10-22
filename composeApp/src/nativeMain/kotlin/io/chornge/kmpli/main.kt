package io.chornge.kmpli

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>): Unit = runBlocking {
    Kmpli().parse(args) // ← Resides in commonMain
}
