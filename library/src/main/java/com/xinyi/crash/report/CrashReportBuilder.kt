package com.xinyi.crash.report

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Looper
import android.os.Process
import android.os.StatFs
import com.xinyi.crash.extensions.PLACEHOLDER_UNKNOWN
import com.xinyi.crash.extensions.orUnknown
import com.xinyi.crash.extensions.toDisplayString
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 崩溃报告构建器
 *
 * 根据未捕获异常与运行环境，组装标准化 [CrashReport]。
 *
 * @author 杨耿雷
 * @date 2026/7/14 16:05
 */
internal class CrashReportBuilder(private val application: Application, private val activityTracker: ActivityTracker) {

    /**
     * 运行时自定义上下文集合
     */
    private val runtimeCustomInfo = ConcurrentHashMap<String, String>()

    /**
     * 写入运行时可变更的自定义上下文
     */
    fun putCustomInfo(key: String, value: String) {
        require(key.isNotBlank()) { "custom info key must not be blank" }
        runtimeCustomInfo[key] = value
    }

    /**
     * 移除运行时自定义上下文
     */
    fun removeCustomInfo(key: String) {
        runtimeCustomInfo.remove(key)
    }

    /**
     * 用初始化配置中的自定义信息作为初始值
     */
    fun seedCustomInfo(initial: Map<String, String>) {
        runtimeCustomInfo.clear()
        runtimeCustomInfo.putAll(initial)
    }

    /**
     * 根据未捕获异常创建结构化崩溃报告
     *
     * @param thread 崩溃线程
     * @param throwable 未捕获异常
     */
    fun build(thread: Thread, throwable: Throwable): CrashReport {
        return CrashReport(
            crashId = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            threadName = thread.name,
            threadId = resolveThreadId(thread),
            isMainThread = thread === Looper.getMainLooper().thread,
            isAppForeground = activityTracker.isAppInForeground,
            throwable = throwable,
            exceptionType = throwable.javaClass.name,
            exceptionMessage = throwable.message,
            stackTrace = stackTraceOf(throwable),
            appInfo = collectAppInfo(),
            deviceInfo = collectDeviceInfo(),
            runtimeInfo = collectRuntimeInfo(),
            currentActivity = activityTracker.currentActivityName,
            customInfo = runtimeCustomInfo.toMap()
        )
    }

    /**
     * 解析线程 ID
     *
     * API 36 起应使用 [Thread.threadId]；更低版本继续使用已弃用但仍可用的 [Thread.getId]。
     *
     * @param thread 目标线程
     */
    private fun resolveThreadId(thread: Thread): Long {
        return if (Build.VERSION.SDK_INT >= 36) {
            thread.threadId()
        } else {
            @Suppress("DEPRECATION")
            thread.id
        }
    }

    /**
     * 收集应用信息
     */
    private fun collectAppInfo(): Map<String, String> {
        val packageManager = application.packageManager
        val packageName = application.packageName
        val packageInfo = runCatching {
            packageManager.getPackageInfo(packageName, 0)
        }.getOrNull()
        val appName = runCatching {
            packageManager.getApplicationLabel(application.applicationInfo).toString()
        }.getOrDefault(PLACEHOLDER_UNKNOWN)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong()
        }
        val isDebug = application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val processName = resolveProcessName()

        return linkedMapOf(
            "应用名称" to appName,
            "包名" to packageName,
            "进程名" to processName,
            "是否主进程" to (processName == packageName).toString(),
            CrashReport.KEY_VERSION_NAME to packageInfo?.versionName.orUnknown(),
            "版本号" to versionCode.toDisplayString(),
            "进程 ID" to Process.myPid().toString(),
            "是否调试版本" to isDebug.toString()
        )
    }

    /**
     * 收集设备信息
     */
    private fun collectDeviceInfo(): Map<String, String> {
        val cpuAbi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.joinToString()
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }
        return linkedMapOf(
            "设备型号" to Build.MODEL.orUnknown(),
            "设备品牌" to Build.BRAND.orUnknown(),
            "设备厂商" to Build.MANUFACTURER.orUnknown(),
            "系统显示版本" to Build.DISPLAY.orUnknown(),
            "构建指纹" to shortenFingerprint(),
            "系统版本" to "Android ${Build.VERSION.RELEASE}",
            "SDK 版本" to Build.VERSION.SDK_INT.toString(),
            "CPU 架构" to cpuAbi,
            "系统语言" to Locale.getDefault().toString(),
            "时区" to TimeZone.getDefault().id
        )
    }

    /**
     * 收集运行时信息
     */
    private fun collectRuntimeInfo(): Map<String, String> {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return linkedMapOf(
            "已用内存" to formatBytesAsMb(usedMemory),
            "当前可用内存" to formatBytesAsMb(runtime.freeMemory()),
            "最大内存" to formatBytesAsMb(runtime.maxMemory()),
            "活动线程估算数" to Thread.activeCount().toString(),
            "磁盘剩余空间" to formatBytesAsMb(resolveAvailableDiskBytes())
        )
    }

    /**
     * 解析当前进程名
     */
    private fun resolveProcessName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }

        val pid = Process.myPid()
        val activityManager =
            application.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val processName = activityManager?.runningAppProcesses
            ?.firstOrNull { it.pid == pid }
            ?.processName
        if (!processName.isNullOrBlank()) {
            return processName
        }

        return runCatching {
            File("/proc/self/cmdline").readText().substringBefore('\u0000').trim()
        }.getOrNull().orUnknown().let { name ->
            if (name == PLACEHOLDER_UNKNOWN) application.packageName else name
        }
    }

    /**
     * 解析应用私有目录可用磁盘空间
     */
    private fun resolveAvailableDiskBytes(): Long {
        return runCatching {
            val path = application.filesDir.absolutePath
            val statFs = StatFs(path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                statFs.availableBytes
            } else {
                @Suppress("DEPRECATION")
                statFs.availableBlocks.toLong() * statFs.blockSize.toLong()
            }
        }.getOrDefault(-1L)
    }

    /**
     * 将字节数格式化为 MB 文本
     */
    private fun formatBytesAsMb(bytes: Long): String {
        if (bytes < 0L) {
            return PLACEHOLDER_UNKNOWN
        }
        val megaBytes = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.2f MB", megaBytes)
    }

    /**
     * 精简构建指纹，避免日志过长
     */
    private fun shortenFingerprint(fingerprint: String = Build.FINGERPRINT): String {
        return if (fingerprint.length <= 96) {
            fingerprint
        } else {
            fingerprint.take(96) + "..."
        }
    }

    /**
     * 收集堆栈信息
     */
    private fun stackTraceOf(throwable: Throwable): String {
        val writer = StringWriter()
        PrintWriter(writer).use { printWriter ->
            throwable.printStackTrace(printWriter)
        }
        return writer.toString()
    }
}