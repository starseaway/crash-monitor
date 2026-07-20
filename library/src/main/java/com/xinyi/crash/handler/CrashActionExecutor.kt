package com.xinyi.crash.handler

import android.app.Application
import com.xinyi.crash.config.CrashAction
import com.xinyi.crash.config.CrashConfig
import com.xinyi.crash.extensions.orUnknown
import com.xinyi.crash.page.CrashIntent
import com.xinyi.crash.report.CrashReport
import java.lang.Thread.UncaughtExceptionHandler

/**
 * 崩溃后置动作执行器
 *
 * 在日志落盘与业务回调之后，按配置执行退出、重启、崩溃界面或系统默认处理。
 *
 * @property application Application 实例
 * @property config 崩溃监听配置
 * @property previousHandler 被本框架替换掉的前一个未捕获异常处理器；未设置时为 null
 *
 * @author 杨耿雷
 * @date 2026/7/15 8:35
 */
internal class CrashActionExecutor(
    private val application: Application,
    private val config: CrashConfig,
    private val previousHandler: UncaughtExceptionHandler?
) {

    /**
     * 执行配置的崩溃后置动作
     *
     * @param thread 崩溃线程
     * @param throwable 未捕获异常
     * @param report 结构化崩溃报告；报告组装失败时为 null
     * @param logFilePath 当前日志文件路径
     * @param logDirectory 日志目录路径
     */
    fun execute(
        thread: Thread,
        throwable: Throwable,
        report: CrashReport?,
        logFilePath: String,
        logDirectory: String
    ) {
        when (config.crashAction) {
            CrashAction.SYSTEM_DEFAULT -> delegateToPreviousOrExit(thread, throwable)
            CrashAction.EXIT, CrashAction.RESTART -> {
                if (config.showCrashUi) {
                    launchCrashUiOrFallback(report, logFilePath, logDirectory)
                } else {
                    executeDirectAction()
                }
            }
        }
    }

    /**
     * 无界面时直接执行退出或重启
     */
    private fun executeDirectAction() {
        when (config.crashAction) {
            CrashAction.RESTART -> restartDirectly()
            CrashAction.EXIT, CrashAction.SYSTEM_DEFAULT -> CrashProcessHelper.exitProcessNow()
        }
    }

    /**
     * 拉起崩溃界面
     *
     * 失败时回退为直接动作
     *
     * @param report 结构化崩溃报告
     * @param logFilePath 当前日志文件绝对路径
     * @param logDirectory 日志目录绝对路径
     */
    private fun launchCrashUiOrFallback(report: CrashReport?, logFilePath: String, logDirectory: String) {
        val intent = CrashIntent.create(
            context = application,
            activityClass = config.crashActivityClass,
            uiMode = config.crashUiMode,
            crashAction = config.crashAction,
            crashId = report?.crashId.orEmpty(),
            exceptionType = report?.exceptionType.orUnknown(),
            exceptionMessage = report?.exceptionMessage,
            threadName = report?.threadName.orEmpty(),
            appVersionName = report?.versionName.orEmpty(),
            timestampMillis = report?.timestampMillis ?: 0L,
            stackTrace = report?.stackTrace.orEmpty(),
            logFilePath = logFilePath,
            logDirectory = logDirectory,
            logoResId = config.crashUiLogoResId,
            message = config.crashUiMessage,
            displayMillis = config.crashUiDisplayMillis,
            maxRestartsInWindow = config.maxRestartsInWindow,
            restartWindowMillis = config.restartWindowMillis
        )
        val launched = runCatching {
            application.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!launched) {
            executeDirectAction()
            return
        }
        // 结束崩溃进程，由系统以崩溃界面为入口重建
        CrashProcessHelper.exitProcessNow()
    }

    /**
     * 无界面直接重启
     */
    private fun restartDirectly() {
        if (!CrashProcessHelper.canRestartSafely(
                application,
                config.maxRestartsInWindow,
                config.restartWindowMillis
            )
        ) {
            CrashProcessHelper.exitProcessNow()
            return
        }
        CrashProcessHelper.markRestartAttempt(application, config.restartWindowMillis)
        if (!CrashProcessHelper.launchAppMain(application)) {
            CrashProcessHelper.exitProcessNow()
            return
        }
        CrashProcessHelper.exitProcessNow()
    }

    /**
     * 将异常交给被本框架替换掉的前一个未捕获异常处理器
     *
     * > 不可用或调用失败时结束进程
     * 
     * @param thread 崩溃线程
     * @param throwable 未捕获异常
     */
    private fun delegateToPreviousOrExit(thread: Thread, throwable: Throwable) {
        val handler = previousHandler ?: run {
            CrashProcessHelper.exitProcessNow()
            return
        }

        val delegated = runCatching {
            handler.uncaughtException(thread, throwable)
        }.isSuccess

        if (delegated) {
            return
        }
        CrashProcessHelper.exitProcessNow()
    }
}