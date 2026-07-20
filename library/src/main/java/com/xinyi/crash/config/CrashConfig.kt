package com.xinyi.crash.config

import android.app.Activity
import com.xinyi.crash.page.CrashActivity
import com.xinyi.ember.file.FileLogger
import java.io.File

/**
 * 崩溃监听初始化配置
 *
 * 仅在 [com.xinyi.crash.CrashMonitor.init] 时生效，不支持运行时热更新整份配置。
 *
 * > 运行时可单独补充的业务上下文，请查看 [com.xinyi.crash.CrashMonitor.putCustomInfo]。
 *
 * @author 杨耿雷
 * @date 2026/7/14 14:35
 */
class CrashConfig private constructor(
    /** 是否启用崩溃监听 */
    val isEnabled: Boolean,
    /** 崩溃日志目录；为 null 时使用应用私有目录 `filesDir/crash` */
    val logDirectory: File?,
    /** 单个崩溃日志文件大小上限（字节） */
    val maxFileSize: Long,
    /** 本地日志保留月数；为 0 时不自动清理 */
    val retainMonthCount: Int,
    /** 自动清理扫描间隔（毫秒）；为 0 时不周期扫描 */
    val clearScanIntervalMillis: Long,
    /** 落盘与回调完成后的默认处理动作 */
    val crashAction: CrashAction,
    /** [CrashAction.RESTART] 在统计窗口内允许的最大重启次数 */
    val maxRestartsInWindow: Int,
    /** 重启次数统计窗口（毫秒） */
    val restartWindowMillis: Long,
    /** 是否展示崩溃界面；为 false 时直接执行 [crashAction] */
    val showCrashUi: Boolean,
    /** 崩溃界面模式；仅 [showCrashUi] 为 true 时生效 */
    val crashUiMode: CrashUiMode,
    /**
     * 崩溃页 Activity
     *
     * > 仅 [showCrashUi] 为 true 时使用。
     *
     * 默认为 [CrashActivity]；可替换为自定义 Activity，也可继承后覆写。
     */
    val crashActivityClass: Class<out Activity>,
    /** 过渡模式展示时长（毫秒） */
    val crashUiDisplayMillis: Long,
    /** 崩溃界面 Logo 资源 ID；为 0 时保留占位框 */
    val crashUiLogoResId: Int,
    /** 崩溃界面提示文案；为空时使用库内默认文案 */
    val crashUiMessage: String?,
    /** 追加到崩溃报告中的自定义信息 */
    val customInfo: Map<String, String>
) {

    companion object {

        /** 默认保留最近 3 个月的崩溃日志 */
        const val DEFAULT_RETAIN_MONTHS = 3

        /** 默认窗口内最多重启 3 次 */
        const val DEFAULT_MAX_RESTARTS_IN_WINDOW = 3

        /** 默认重启统计窗口 60 秒 */
        const val DEFAULT_RESTART_WINDOW_MILLIS = 60_000L

        /** 默认每 24 小时扫描一次过期崩溃日志（毫秒） */
        const val DEFAULT_CLEAR_SCAN_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L

        /** 默认过渡页展示 1.5 秒 */
        const val DEFAULT_CRASH_UI_DISPLAY_MILLIS = 1_500L

        /**
         * 创建默认配置
         */
        @JvmStatic
        fun defaultConfig(): CrashConfig {
            return Builder().build()
        }
    }

    /**
     * 配置构建器
     *
     * 默认启用监听，崩溃后执行 [CrashAction.EXIT] 且不展示界面。
     */
    class Builder {

        private var isEnabled: Boolean = true
        private var logDirectory: File? = null
        private var maxFileSize: Long = FileLogger.DEFAULT_MAX_FILE_SIZE
        private var retainMonthCount: Int = DEFAULT_RETAIN_MONTHS
        private var clearScanIntervalMillis: Long = DEFAULT_CLEAR_SCAN_INTERVAL_MILLIS
        private var crashAction: CrashAction = CrashAction.EXIT
        private var maxRestartsInWindow: Int = DEFAULT_MAX_RESTARTS_IN_WINDOW
        private var restartWindowMillis: Long = DEFAULT_RESTART_WINDOW_MILLIS
        private var showCrashUi: Boolean = false
        private var crashUiMode: CrashUiMode = CrashUiMode.INTERACTIVE
        private var crashActivityClass: Class<out Activity> = CrashActivity::class.java
        private var crashUiDisplayMillis: Long = DEFAULT_CRASH_UI_DISPLAY_MILLIS
        private var crashUiLogoResId: Int = 0
        private var crashUiMessage: String? = null
        private val customInfo: MutableMap<String, String> = linkedMapOf()

        fun setEnabled(enabled: Boolean): Builder {
            isEnabled = enabled
            return this
        }

        /**
         * 设置崩溃日志目录
         *
         * @param directory 日志根目录；传入 null 表示使用默认目录
         */
        fun setLogDirectory(directory: File?): Builder {
            logDirectory = directory
            return this
        }

        fun setMaxFileSize(maxFileSize: Long): Builder {
            this.maxFileSize = maxFileSize
            return this
        }

        /**
         * 设置本地日志保留月数
         *
         * @param monthCount 保留月数；0 表示不自动清理
         */
        fun setRetainMonthCount(monthCount: Int): Builder {
            require(monthCount >= 0) { "retainMonthCount must be greater than or equal to 0" }
            retainMonthCount = monthCount
            return this
        }

        /**
         * 设置过期日志自动清理的扫描间隔
         *
         * 与 [setRetainMonthCount] 同时大于 0 时，会在初始化后立即异步清理一次，
         * 之后按该间隔周期扫描。
         *
         * @param intervalMillis 扫描间隔毫秒数；0 表示不周期扫描
         */
        fun setClearScanIntervalMillis(intervalMillis: Long): Builder {
            require(intervalMillis >= 0L) { "clearScanIntervalMillis must be greater than or equal to 0" }
            clearScanIntervalMillis = intervalMillis
            return this
        }

        /**
         * 设置崩溃后的默认处理动作
         *
         * @param action 处理动作
         */
        fun setCrashAction(action: CrashAction): Builder {
            crashAction = action
            return this
        }

        /**
         * 设置重启循环保护参数
         *
         * @param maxRestarts 窗口内最大重启次数，必须大于 0
         * @param windowMillis 统计窗口毫秒数，必须大于 0
         */
        fun setRestartLoopProtection(maxRestarts: Int, windowMillis: Long): Builder {
            require(maxRestarts > 0) { "maxRestarts must be greater than 0" }
            require(windowMillis > 0L) { "windowMillis must be greater than 0" }
            maxRestartsInWindow = maxRestarts
            restartWindowMillis = windowMillis
            return this
        }

        /**
         * 是否展示崩溃界面
         *
         * 为 false 时按 [setCrashAction] 直接退出或重启，不进入界面。
         * [CrashAction.SYSTEM_DEFAULT] 始终不进入框架界面。
         *
         * @param show true 表示展示
         */
        fun setShowCrashUi(show: Boolean): Builder {
            showCrashUi = show
            return this
        }

        /**
         * 设置崩溃界面模式
         *
         * @param mode [CrashUiMode.INTERACTIVE] 或 [CrashUiMode.TRANSITION]
         */
        fun setCrashUiMode(mode: CrashUiMode): Builder {
            crashUiMode = mode
            return this
        }

        /**
         * 设置崩溃页 Activity
         *
         * > 完全自定义时，可通过 [com.xinyi.crash.page.CrashIntent] 读取框架传入的配置与崩溃摘要。
         *
         * @param activityClass 崩溃页 Activity 类
         */
        fun setCrashActivity(activityClass: Class<out Activity>): Builder {
            crashActivityClass = activityClass
            return this
        }

        /**
         * 设置过渡模式展示时长
         *
         * @param millis 展示时长（毫秒），必须大于或等于 0
         */
        fun setCrashUiDisplayMillis(millis: Long): Builder {
            require(millis >= 0L) { "crashUiDisplayMillis must be greater than or equal to 0" }
            crashUiDisplayMillis = millis
            return this
        }

        /**
         * 设置崩溃界面 Logo
         *
         * @param resId Logo 资源 ID；为 0 时保留占位框
         */
        fun setCrashUiLogo(resId: Int): Builder {
            crashUiLogoResId = resId
            return this
        }

        /**
         * 设置崩溃界面提示文案
         *
         * @param message 提示文案；为空时使用库内默认文案
         */
        fun setCrashUiMessage(message: String?): Builder {
            crashUiMessage = message
            return this
        }

        /**
         * 添加自定义崩溃上下文
         *
         * > 不要写入密码、令牌等敏感信息。
         *
         * @param key 信息名称
         * @param value 信息内容
         */
        fun putCustomInfo(key: String, value: String): Builder {
            require(key.isNotBlank()) { "custom info key must not be blank" }
            customInfo[key] = value
            return this
        }

        /**
         * 清空自定义崩溃上下文
         */
        fun clearCustomInfo(): Builder {
            customInfo.clear()
            return this
        }

        /**
         * 构建配置
         *
         * @return 初始化用配置
         */
        fun build(): CrashConfig {
            return CrashConfig(
                isEnabled = isEnabled,
                logDirectory = logDirectory,
                maxFileSize = FileLogger.normalizeMaxFileSize(maxFileSize),
                retainMonthCount = retainMonthCount,
                clearScanIntervalMillis = clearScanIntervalMillis,
                crashAction = crashAction,
                maxRestartsInWindow = maxRestartsInWindow,
                restartWindowMillis = restartWindowMillis,
                showCrashUi = showCrashUi,
                crashUiMode = crashUiMode,
                crashActivityClass = crashActivityClass,
                crashUiDisplayMillis = crashUiDisplayMillis,
                crashUiLogoResId = crashUiLogoResId,
                crashUiMessage = crashUiMessage,
                customInfo = customInfo.toMap()
            )
        }
    }
}