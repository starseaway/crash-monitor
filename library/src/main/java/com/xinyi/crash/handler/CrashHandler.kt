package com.xinyi.crash.handler

import com.xinyi.crash.report.CrashReport
import com.xinyi.crash.report.CrashReportBuilder
import com.xinyi.crash.store.CrashLogStore
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 未捕获异常处理器
 *
 * @param reportBuilder 崩溃报告构建器
 * @param logStore 崩溃日志存储器
 * @param callback 崩溃回调接口
 * @param actionExecutor 崩溃后置动作执行器
 *
 * @author 杨耿雷
 * @date 2026/7/15 8:52
 */
internal class CrashHandler(
    private val reportBuilder: CrashReportBuilder,
    private val logStore: CrashLogStore,
    private val callback: CrashCallback?,
    private val actionExecutor: CrashActionExecutor
) : UncaughtExceptionHandler {

    /**
     * 是否正在处理崩溃
     */
    private val isHandling = AtomicBoolean(false)

    /**
     * 捕获到未处理异常时调用
     *
     * @param thread 崩溃线程
     * @param throwable 未捕获异常
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        var report: CrashReport? = null
        if (isHandling.compareAndSet(false, true)) {
            report = runCatching {
                reportBuilder.build(thread, throwable)
            }.onFailure { reportError ->
                runCatching { logStore.writeFallback(throwable, reportError) }
            }.getOrNull()

            if (report != null) {
                runCatching { logStore.write(report) }
                // 回调只做轻量补充，后置退出/重启由框架接管
                runCatching { callback?.onCrash(report) }
            }
        }
        actionExecutor.execute(
            thread = thread,
            throwable = throwable,
            report = report,
            logFilePath = logStore.getLogFilePath(),
            logDirectory = logStore.getLogDirectoryPath()
        )
    }
}