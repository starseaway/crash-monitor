package com.xinyi.crash.report

/**
 * 一次未捕获异常生成的结构化崩溃报告
 *
 * @property crashId 本次崩溃的唯一标识
 * @property timestampMillis 崩溃发生时间（毫秒）
 * @property threadName 崩溃线程名称
 * @property threadId 崩溃线程 ID
 * @property isMainThread 崩溃线程是否为主线程
 * @property isAppForeground 崩溃时应用是否位于前台
 * @property throwable 未捕获异常对象
 * @property exceptionType 异常类全限定名
 * @property exceptionMessage 异常消息，可能为空
 * @property stackTrace 完整异常堆栈
 * @property appInfo 应用信息快照
 * @property deviceInfo 设备信息快照
 * @property runtimeInfo 运行时信息快照
 * @property currentActivity 当前前台 Activity 类名，无法获取时为空
 * @property customInfo 使用方配置的自定义信息
 *
 * @author 杨耿雷
 * @date 2026/7/14 15:02
 */
class CrashReport internal constructor(
    val crashId: String,
    val timestampMillis: Long,
    val threadName: String,
    val threadId: Long,
    val isMainThread: Boolean,
    val isAppForeground: Boolean,
    val throwable: Throwable,
    val exceptionType: String,
    val exceptionMessage: String?,
    val stackTrace: String,
    val appInfo: Map<String, String>,
    val deviceInfo: Map<String, String>,
    val runtimeInfo: Map<String, String>,
    val currentActivity: String?,
    val customInfo: Map<String, String>
) {

    companion object {
        
        /** [appInfo] 中版本名称字段的键 */
        internal const val KEY_VERSION_NAME = "版本名称"
    }

    /**
     * 应用版本名称；取自 [appInfo]，缺失时为空串
     */
    val versionName: String
        get() = appInfo[KEY_VERSION_NAME].orEmpty()

    /**
     * 格式化后的完整崩溃日志文本
     */
    val text: String by lazy(LazyThreadSafetyMode.NONE) {
        CrashReportFormatter.format(this)
    }

    /**
     * 返回格式化后的完整崩溃日志
     */
    override fun toString(): String = text
}