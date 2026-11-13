package com.glovbling.app

import java.util.concurrent.TimeUnit

object RootAccess {
    fun hasRootAccess(timeoutSeconds: Long = 8): Boolean = try {
        val process = ProcessBuilder("su", "-c", "id -u")
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            false
        } else {
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.exitValue() == 0 && output.lineSequence().any { it.trim() == "0" }
        }
    } catch (_: Exception) {
        false
    }
}
