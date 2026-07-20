package com.xinyi.crash

import android.app.Application
import com.xinyi.crash.config.CrashConfig
import com.xinyi.crash.handler.CrashActionExecutor
import com.xinyi.crash.handler.CrashCallback
import com.xinyi.crash.handler.CrashHandler
import com.xinyi.crash.report.ActivityTracker
import com.xinyi.crash.report.CrashReportBuilder
import com.xinyi.crash.store.CrashLogStore
import java.io.File
import java.lang.Thread.UncaughtExceptionHandler

/**
 * Crash Monitor 崩溃监听框架入口
 *
 * - 使用 [init] 初始化并注册异常处理器
 * - 使用 [builder] 创建初始化配置
 * - 使用 [putCustomInfo] 在运行时补充用户、渠道等上下文
 * - 使用 [clearLogFiles] 主动清理过期崩溃日志
 * - 使用 [unregister] 注销异常处理器，并恢复被本框架替换掉的前一个未捕获异常处理器
 *
 * @author 杨耿雷
 * @date 2026/7/14 16:51
 */
object CrashMonitor {

    /**
     * 锁对象
     * 
     * 用于同步注册状态，避免多线程同时操作注册状态导致的问题。
     */
    private val mLock = Any()

    /**
     * 注册后的运行时状态
     * 
     */
    @Volatile
    private var mRegistration: Registration? = null

    /**
     * 创建初始化配置构建器
     *
     * @return 崩溃监听配置构建器
     */
    @JvmStatic
    fun builder(): CrashConfig.Builder {
        return CrashConfig.Builder()
    }

    /**
     * 使用默认配置初始化
     *
     * @param application Application 实例
     */
    @JvmStatic
    fun init(application: Application) {
        init(application, CrashConfig.defaultConfig(), null)
    }

    /**
     * 初始化崩溃监听
     *
     * 重复调用会先注销旧的异常处理器，再按新配置重新注册。
     *
     * @param application Application 实例
     * @param config 初始化配置
     */
    @JvmStatic
    fun init(application: Application, config: CrashConfig) {
        init(application, config, null)
    }

    /**
     * 初始化崩溃监听
     *
     * 建议先完成 Ember 初始化。回调仅适合轻量补充，不应自行杀进程或重启。
     *
     * @param application Application 实例
     * @param config 初始化配置
     * @param callback 业务回调；落盘完成后、后置动作前触发
     */
    @JvmStatic
    fun init(application: Application, config: CrashConfig, callback: CrashCallback?) {
        synchronized(mLock) {
            // 重复初始化时先注销旧实例，避免处理器与清理调度器泄漏
            unregisterLocked()
            if (!config.isEnabled) {
                return
            }

            val tracker = createActivityTracker(application)
            val logStore = createLogStore(application, config)
            val reportBuilder = createReportBuilder(application, tracker, config)

            // 保存被本框架替换掉的前一个未捕获异常处理器，供 SYSTEM_DEFAULT 动作委派
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            val actionExecutor = CrashActionExecutor(application, config, previousHandler)
            val handler = CrashHandler(reportBuilder, logStore, callback, actionExecutor)

            // 覆盖进程默认未捕获异常处理器
            Thread.setDefaultUncaughtExceptionHandler(handler)
            mRegistration = Registration(
                application = application,
                tracker = tracker,
                previousHandler = previousHandler,
                handler = handler,
                reportBuilder = reportBuilder,
                logStore = logStore
            )
        }
    }

    /**
     * 框架是否已注册
     */
    @JvmStatic
    fun isRegistered(): Boolean {
        return mRegistration != null
    }

    /**
     * 运行时补充自定义崩溃上下文
     *
     * 适合登录后写入用户 ID、会话号等会变化的信息。
     *
     * > 总之根据自身需求而定，目的都是为了在崩溃后，能够更快的判断问题。
     *
     * @param key 信息名称
     * @param value 信息内容
     */
    @JvmStatic
    fun putCustomInfo(key: String, value: String) {
        mRegistration?.reportBuilder?.putCustomInfo(key, value)
    }

    /**
     * 移除运行时自定义崩溃上下文
     *
     * @param key 信息名称
     */
    @JvmStatic
    fun removeCustomInfo(key: String) {
        mRegistration?.reportBuilder?.removeCustomInfo(key)
    }

    /**
     * 获取当前正在写入的崩溃日志文件路径
     *
     * @return 日志文件绝对路径；未注册或文件不可用时返回空字符串
     */
    @JvmStatic
    fun getLogFilePath(): String {
        return mRegistration?.logStore?.getLogFilePath().orEmpty()
    }

    /**
     * 清理指定月数以前的崩溃日志
     *
     * @param retainMonthCount 保留最近几个月日志；必须大于或等于 0
     */
    @JvmStatic
    fun clearLogFiles(retainMonthCount: Int) {
        require(retainMonthCount >= 0) {
            "retainMonthCount must be greater than or equal to 0"
        }
        mRegistration?.logStore?.clearLogFiles(retainMonthCount)
    }

    /**
     * 注销崩溃异常处理器，并恢复被本框架替换掉的前一个未捕获异常处理器
     */
    @JvmStatic
    fun unregister() {
        synchronized(mLock) {
            unregisterLocked()
        }
    }

    /**
     * 创建并注册页面与前后台跟踪器
     *
     * @param application Application 实例
     */
    private fun createActivityTracker(application: Application): ActivityTracker {
        val tracker = ActivityTracker()
        // 记录 Started 计数与最近 RESUMED Activity，供崩溃报告上下文使用
        application.registerActivityLifecycleCallbacks(tracker)
        return tracker
    }

    /**
     * 创建崩溃日志存储器
     *
     * @param application Application 实例
     * @param config 初始化配置
     */
    private fun createLogStore(application: Application, config: CrashConfig): CrashLogStore {
        // 未指定目录时统一落到应用私有 filesDir，各 API 行为一致
        val logDirectory = config.logDirectory ?: File(application.filesDir, "crash")

        return CrashLogStore(
            logDirectory = logDirectory,
            maxFileSize = config.maxFileSize,
            retainMonthCount = config.retainMonthCount,
            clearScanIntervalMillis = config.clearScanIntervalMillis
        )
    }

    /**
     * 创建崩溃报告构建器，并写入初始化时的自定义上下文
     *
     * @param application Application 实例
     * @param tracker 页面与前后台跟踪器
     * @param config 初始化配置
     */
    private fun createReportBuilder(
        application: Application,
        tracker: ActivityTracker,
        config: CrashConfig
    ): CrashReportBuilder {
        return CrashReportBuilder(
            application = application,
            activityTracker = tracker
        ).apply {
            seedCustomInfo(config.customInfo)
        }
    }

    /**
     * 在已持有锁的情况下注销当前注册状态
     */
    private fun unregisterLocked() {
        val current = mRegistration ?: return
        // 仅当前仍是我们注册的处理器时才恢复，避免误清第三方后挂上的 handler
        if (Thread.getDefaultUncaughtExceptionHandler() === current.handler) {
            Thread.setDefaultUncaughtExceptionHandler(current.previousHandler)
        }
        current.application.unregisterActivityLifecycleCallbacks(current.tracker)
        // 停止过期日志扫描，并关闭文件流
        current.logStore.close()
        mRegistration = null
    }

    /**
     * 一次成功注册的运行时状态
     *
     * @property application Application 实例
     * @property tracker 页面与前后台跟踪器
     * @property previousHandler 被本框架替换掉的前一个未捕获异常处理器；未设置时为 null
     * @property handler 崩溃后异常处理器
     * @property reportBuilder 本次崩溃报告构建器
     * @property logStore 崩溃日志存储器
     */
    private data class Registration(
        val application: Application,
        val tracker: ActivityTracker,
        val previousHandler: UncaughtExceptionHandler?,
        val handler: CrashHandler,
        val reportBuilder: CrashReportBuilder,
        val logStore: CrashLogStore
    )
}