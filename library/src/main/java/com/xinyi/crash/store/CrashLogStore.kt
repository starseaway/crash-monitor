package com.xinyi.crash.store

import com.xinyi.crash.report.CrashReport
import com.xinyi.ember.Ember
import com.xinyi.ember.file.FileLogger
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 崩溃日志存储器
 *
 * 线程安全，同步写入文件。
 *
 * @param logDirectory 崩溃日志根目录
 * @param maxFileSize 单文件大小上限（字节）
 * @param retainMonthCount 自动清理时保留最近几个月日志；为 0 时不自动清理
 * @param clearScanIntervalMillis 自动清理扫描间隔（毫秒）；为 0 时不周期扫描
 *
 * @author 杨耿雷
 * @date 2026/7/14 16:28
 */
internal class CrashLogStore(
    private val logDirectory: File,
    maxFileSize: Long,
    private val retainMonthCount: Int = 0,
    private val clearScanIntervalMillis: Long = 0L
) {

    private companion object {

        /** 日志标签 */
        private const val CRASH_TAG = "CrashMonitor"
    }

    /**
     * 文件日志记录器
     */
    private val fileLogger = FileLogger(logDirectory.absolutePath, maxFileSize)

    /**
     * 是否已关闭
     */
    private val isClosed = AtomicBoolean(false)

    /**
     * 过期日志扫描调度器；保留月数与扫描间隔均大于 0 时启用
     */
    private val clearScheduler = createClearScheduler()

    /**
     * 创建并启动过期日志扫描调度器
     *
     * 保留月数或扫描间隔无效时，不创建调度线程。
     */
    private fun createClearScheduler(): ScheduledExecutorService? {
        if (retainMonthCount <= 0 || clearScanIntervalMillis <= 0L) {
            return null
        }

        return Executors.newSingleThreadScheduledExecutor(ClearScanThreadFactory()).apply {
            scheduleWithFixedDelay(
                {
                    if (!isClosed.get()) {
                        runCatching {
                            clearLogFiles(retainMonthCount)
                        }
                    }
                },
                // 启动后立即扫描一次，之后按固定间隔重复扫描
                0L,
                clearScanIntervalMillis,
                TimeUnit.MILLISECONDS
            )
        }
    }

    /**
     * 输出并持久化崩溃报告
     *
     * @param report 结构化崩溃报告
     */
    fun write(report: CrashReport) {
        val content = report.text
        // 先同步写文件，再走 Ember 输出管道
        fileLogger.write(content)
        Ember.e(CRASH_TAG, content)
    }

    /**
     * 报告生成失败时输出最小兜底日志
     *
     * @param throwable 原始崩溃异常
     * @param reportError 生成报告时抛出的异常
     */
    fun writeFallback(throwable: Throwable, reportError: Throwable) {
        val content = buildString {
            append("Crash Monitor 生成完整报告失败\n")
            append("原始崩溃异常: ").append(throwable.javaClass.name)
                .append(": ").append(throwable.message).append('\n')
            append("生成报告异常: ").append(reportError.javaClass.name)
                .append(": ").append(reportError.message)
        }
        fileLogger.write(content)
        Ember.e(CRASH_TAG, content, throwable)
    }

    /**
     * 清除几个月前的日志目录
     *
     * @param monthCount 保留最近几个月日志
     */
    fun clearLogFiles(monthCount: Int) {
        if (isClosed.get()) {
            return
        }
        fileLogger.clearLogFiles(monthCount)
    }

    /**
     * 获取当前日志文件路径
     *
     * @return 当前正在写入的日志文件绝对路径；文件不可用时返回空字符串
     */
    fun getLogFilePath(): String {
        return fileLogger.getLogFilePath()
    }

    /**
     * 获取崩溃日志目录路径
     */
    fun getLogDirectoryPath(): String {
        return logDirectory.absolutePath
    }

    /**
     * 停止自动清理并关闭文件流
     */
    fun close() {
        if (!isClosed.compareAndSet(false, true)) {
            return
        }
        clearScheduler?.shutdownNow()
        fileLogger.close()
    }

    /**
     * 过期日志扫描线程工厂
     */
    private class ClearScanThreadFactory : ThreadFactory {

        private val threadIndex = AtomicInteger(0)

        override fun newThread(runnable: Runnable): Thread {
            return Thread(runnable, "CrashMonitor-Log-Clear-${threadIndex.getAndIncrement()}").apply {
                isDaemon = true
            }
        }
    }
}