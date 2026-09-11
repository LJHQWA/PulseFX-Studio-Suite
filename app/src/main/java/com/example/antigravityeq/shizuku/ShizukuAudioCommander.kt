package com.example.antigravityeq.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuAudioCommander {
    private const val TAG = "ShizukuAudioCommander"
    const val REQUEST_CODE = 2026

    private val _isShizukuAvailable = MutableStateFlow(false)
    val isShizukuAvailable: StateFlow<Boolean> = _isShizukuAvailable.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isShizukuAvailable.value = false
        _hasPermission.value = false
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE) {
            _hasPermission.value = (grantResult == PackageManager.PERMISSION_GRANTED)
            if (_hasPermission.value) {
                disableHardwareOffload()
            }
        }
    }

    fun init() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            checkShizukuState()
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku init error (standalone mode active): ${e.message}")
        }
    }

    fun cleanup() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku cleanup error", e)
        }
    }

    fun checkShizukuState() {
        try {
            val ping = Shizuku.pingBinder()
            _isShizukuAvailable.value = ping
            if (ping) {
                val perm = if (Shizuku.isPreV11()) {
                    false
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
                _hasPermission.value = perm
                if (perm) {
                    disableHardwareOffload()
                }
            } else {
                _hasPermission.value = false
            }
        } catch (e: Exception) {
            _isShizukuAvailable.value = false
            _hasPermission.value = false
        }
    }

    fun requestPermission() {
        if (_isShizukuAvailable.value && !_hasPermission.value) {
            try {
                Shizuku.requestPermission(REQUEST_CODE)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request Shizuku permission", e)
            }
        }
    }

    fun disableHardwareOffload() {
        Thread {
            try {
                execElevated("setprop audio.offload.disable 1; setprop vendor.audio.offload.disable 1; setprop persist.vendor.audio.offload.disable 1")
                Log.i(TAG, "Hardware audio offload disabled via Shizuku elevated shell.")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to disable audio offload via Shizuku: ${e.message}")
            }
        }.start()
    }

    suspend fun scanAudioFlingerSessionsElevated(): List<Int> = withContext(Dispatchers.IO) {
        val sessionIds = mutableSetOf<Int>()
        if (!_isShizukuAvailable.value || !_hasPermission.value) {
            return@withContext emptyList()
        }

        try {
            val output = execElevated("dumpsys media.audio_flinger")
            val sessionIdRegex = Regex("(?i)session(?:\\s+id)?:?\\s+(\\d+)")
            val sessionsRegex = Regex("(?i)sessions:\\s*([\\d\\s]+)")
            val trackRegex = Regex("(?i)session\\s+(\\d+)")

            output.lineSequence().forEach { line ->
                sessionIdRegex.findAll(line).forEach { match ->
                    match.groups[1]?.value?.toIntOrNull()?.let { sessionIds.add(it) }
                }
                sessionsRegex.find(line)?.let { match ->
                    val sessionsList = match.groups[1]?.value ?: ""
                    sessionsList.split("\\s+".toRegex()).forEach { token ->
                        token.toIntOrNull()?.let { sessionIds.add(it) }
                    }
                }
                trackRegex.findAll(line).forEach { match ->
                    match.groups[1]?.value?.toIntOrNull()?.let { sessionIds.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Elevated AudioFlinger scan error", e)
        }

        sessionIds.filter { it > 0 }
    }

    fun execElevated(command: String): String {
        val sb = StringBuilder()
        var process: java.lang.Process? = null
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as? java.lang.Process

            if (process != null) {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                reader.close()
                process.waitFor()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Command execution failed: $command", e)
        } finally {
            process?.destroy()
        }
        return sb.toString()
    }
}
