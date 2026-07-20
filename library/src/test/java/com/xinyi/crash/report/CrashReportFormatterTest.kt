package com.xinyi.crash.report

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportFormatterTest {

    @Test
    fun format_containsCoreCrashContext() {
        val report = CrashReport(
            crashId = "test-crash-id",
            timestampMillis = 0L,
            threadName = "main",
            threadId = 1L,
            isMainThread = true,
            isAppForeground = true,
            throwable = IllegalStateException("test"),
            exceptionType = IllegalStateException::class.java.name,
            exceptionMessage = "test",
            stackTrace = "stack trace",
            appInfo = linkedMapOf("包名" to "com.example"),
            deviceInfo = linkedMapOf("设备型号" to "device"),
            runtimeInfo = linkedMapOf("最大内存" to "1.00 MB"),
            currentActivity = "com.example.MainActivity",
            customInfo = linkedMapOf("渠道" to "debug")
        )

        val text = CrashReportFormatter.format(report)

        assertTrue(text.contains("崩溃 ID: test-crash-id"))
        assertTrue(text.contains("是否主线程: true"))
        assertTrue(text.contains("应用是否前台: true"))
        assertTrue(text.contains("异常类型: java.lang.IllegalStateException"))
        assertTrue(text.contains("当前页面: com.example.MainActivity"))
        assertTrue(text.contains("渠道: debug"))
        assertFalse(text.contains("简要分析"))
    }
}
