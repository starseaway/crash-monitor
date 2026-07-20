package com.xinyi.crash.report

import com.xinyi.crash.extensions.appendField
import com.xinyi.crash.extensions.appendLineCompat
import com.xinyi.crash.extensions.appendMapSection
import com.xinyi.crash.extensions.appendSection
import com.xinyi.crash.extensions.orNone
import com.xinyi.crash.extensions.orUnknown
import com.xinyi.crash.utils.CrashTimeFormatter

/**
 * 崩溃报告文本格式化器
 *
 * 将结构化的 [CrashReport] 格式化为便于阅读和归档的文本。
 *
 * @author 杨耿雷
 * @date 2026/7/14 15:40
 */
internal object CrashReportFormatter {

    /**
     * 格式化崩溃报告
     *
     * @param report 结构化崩溃报告
     * @return 完整崩溃日志文本
     */
    fun format(report: CrashReport): String {
        return buildString {
            appendLineCompat("********* Crash Monitor 崩溃日志 *********")

            appendField("崩溃 ID", report.crashId)
            appendField("发生时间", CrashTimeFormatter.format(report.timestampMillis))
            appendField("崩溃线程", "${report.threadName} (id=${report.threadId})")
            appendField("是否主线程", report.isMainThread.toString())
            appendField("应用是否前台", report.isAppForeground.toString())

            appendSection("异常信息")
            appendField("异常类型", report.exceptionType)
            appendField("异常消息", report.exceptionMessage.orNone())
            appendLineCompat("堆栈跟踪:")
            appendLineCompat(report.stackTrace.trimEnd())

            appendMapSection("应用信息", report.appInfo)
            appendMapSection("设备信息", report.deviceInfo)
            appendMapSection("运行时信息", report.runtimeInfo)

            appendSection("崩溃上下文")
            appendField("当前页面", report.currentActivity.orUnknown())
            report.customInfo.forEach { (key, value) ->
                appendField(key, value)
            }

            appendLineCompat("********* 日志结束 *********")
        }
    }
}