package com.xinyi.crash.page

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.xinyi.crash.config.CrashAction
import com.xinyi.crash.config.CrashConfig
import com.xinyi.crash.config.CrashUiMode

/**
 * 崩溃页启动 Intent 与 Extra Key
 *
 * 统一管理崩溃页启动所需的 Intent 构建逻辑及所有 Extra Key，
 * 供内置的 [CrashActivity]、或其子类，又或者完全自定义的崩溃 Activity 界面使用。
 *
 * @author 杨耿雷
 * @date 2026/7/16 9:55
 */
object CrashIntent {

    /** 界面模式：值为 [CrashUiMode.name] */
    const val EXTRA_UI_MODE = "extra_ui_mode"

    /** 默认动作：值为 [CrashAction.name] */
    const val EXTRA_CRASH_ACTION = "extra_crash_action"

    /** 崩溃 ID */
    const val EXTRA_CRASH_ID = "extra_crash_id"

    /** 异常类型 */
    const val EXTRA_EXCEPTION_TYPE = "extra_exception_type"

    /** 异常消息 */
    const val EXTRA_EXCEPTION_MESSAGE = "extra_exception_message"

    /** 崩溃线程名 */
    const val EXTRA_THREAD_NAME = "extra_thread_name"

    /** 应用版本名称 */
    const val EXTRA_APP_VERSION_NAME = "extra_app_version_name"

    /** 崩溃发生时间（毫秒时间戳） */
    const val EXTRA_TIMESTAMP_MILLIS = "extra_timestamp_millis"

    /** 堆栈摘要（截断后的文本，避免 Intent 过大） */
    const val EXTRA_STACK_SUMMARY = "extra_stack_summary"

    /** 当前崩溃日志文件绝对路径 */
    const val EXTRA_LOG_FILE_PATH = "extra_log_file_path"

    /** 崩溃日志目录绝对路径 */
    const val EXTRA_LOG_DIRECTORY = "extra_log_directory"

    /** Logo 资源 ID；为 0 时使用默认占位 */
    const val EXTRA_LOGO_RES_ID = "extra_logo_res_id"

    /** 提示文案；为空时使用默认文案 */
    const val EXTRA_MESSAGE = "extra_message"

    /** 过渡模式展示时长（毫秒） */
    const val EXTRA_DISPLAY_MILLIS = "extra_display_millis"

    /** 重启循环保护：窗口内最大次数 */
    const val EXTRA_MAX_RESTARTS = "extra_max_restarts"

    /** 重启循环保护：统计窗口（毫秒） */
    const val EXTRA_RESTART_WINDOW_MILLIS = "extra_restart_window_millis"

    /** 堆栈信息最大长度，避免 Binder 传输过大 */
    private const val MAX_STACK_SUMMARY_LENGTH = 2_000

    /**
     * 创建崩溃页启动 Intent
     */
    @JvmStatic
    fun create(
        context: Context,
        activityClass: Class<out Activity>,
        uiMode: CrashUiMode,
        crashAction: CrashAction,
        crashId: String,
        exceptionType: String,
        exceptionMessage: String?,
        threadName: String,
        appVersionName: String,
        timestampMillis: Long,
        stackTrace: String,
        logFilePath: String,
        logDirectory: String,
        logoResId: Int = 0,
        message: String? = null,
        displayMillis: Long = CrashConfig.DEFAULT_CRASH_UI_DISPLAY_MILLIS,
        maxRestartsInWindow: Int = CrashConfig.DEFAULT_MAX_RESTARTS_IN_WINDOW,
        restartWindowMillis: Long = CrashConfig.DEFAULT_RESTART_WINDOW_MILLIS
    ): Intent {
        return Intent(context, activityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(EXTRA_UI_MODE, uiMode.name)
            putExtra(EXTRA_CRASH_ACTION, crashAction.name)
            putExtra(EXTRA_CRASH_ID, crashId)
            putExtra(EXTRA_EXCEPTION_TYPE, exceptionType)
            putExtra(EXTRA_EXCEPTION_MESSAGE, exceptionMessage)
            putExtra(EXTRA_THREAD_NAME, threadName)
            putExtra(EXTRA_APP_VERSION_NAME, appVersionName)
            putExtra(EXTRA_TIMESTAMP_MILLIS, timestampMillis)
            putExtra(EXTRA_STACK_SUMMARY, truncateStack(stackTrace))
            putExtra(EXTRA_LOG_FILE_PATH, logFilePath)
            putExtra(EXTRA_LOG_DIRECTORY, logDirectory)
            putExtra(EXTRA_LOGO_RES_ID, logoResId)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_DISPLAY_MILLIS, displayMillis)
            putExtra(EXTRA_MAX_RESTARTS, maxRestartsInWindow)
            putExtra(EXTRA_RESTART_WINDOW_MILLIS, restartWindowMillis)
        }
    }

    /**
     * 截断堆栈，避免 Intent 传参过大
     *
     * @param stackTrace 堆栈跟踪字符串
     */
    private fun truncateStack(stackTrace: String): String {
        val trimmed = stackTrace.trim()
        if (trimmed.length <= MAX_STACK_SUMMARY_LENGTH) {
            return trimmed
        }
        return trimmed.take(MAX_STACK_SUMMARY_LENGTH) + "\n...（已截断，完整内容见日志文件）"
    }
}