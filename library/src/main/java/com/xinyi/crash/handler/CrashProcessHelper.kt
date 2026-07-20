package com.xinyi.crash.handler

import android.content.Context
import android.content.Intent
import android.os.Process
import kotlin.system.exitProcess

/**
 * 崩溃后进程终止与重启辅助
 *
 * @author 杨耿雷
 * @date 2026/7/16 10:05
 */
internal object CrashProcessHelper {

    /**
     * 重启尝试时间戳记录的 SharedPreferences 名称
     */
    private const val PREFS_NAME = "crash_monitor_restart"

    /**
     * 重启尝试时间戳记录的键
     */
    private const val KEY_RESTART_TIMESTAMPS = "restart_timestamps"

    /**
     * 进程退出代码
     */
    private const val PROCESS_EXIT_CODE = 10

    /**
     * 判断当前是否允许继续自动重启
     * 
     * @param context 上下文
     * @param maxRestartsInWindow 在窗口期内允许的最大重启次数
     * @param restartWindowMillis 窗口期时间长度（毫秒）
     */
    fun canRestartSafely(
        context: Context,
        maxRestartsInWindow: Int,
        restartWindowMillis: Long
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val windowStart = now - restartWindowMillis
        val recentAttempts = prefs.getString(KEY_RESTART_TIMESTAMPS, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.toLongOrNull() }
            .filter { it >= windowStart }
        return recentAttempts.size < maxRestartsInWindow
    }

    /**
     * 记录一次重启尝试
     * 
     * @param context 上下文
     * @param restartWindowMillis 窗口期时间长度（毫秒）
     */
    fun markRestartAttempt(context: Context, restartWindowMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val windowStart = now - restartWindowMillis
        val recentAttempts = prefs.getString(KEY_RESTART_TIMESTAMPS, "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.toLongOrNull() }
            .filter { it >= windowStart }
            .toMutableList()
        recentAttempts.add(now)
        prefs.edit()
            .putString(KEY_RESTART_TIMESTAMPS, recentAttempts.joinToString(","))
            .apply()
    }

    /**
     * 拉起应用默认启动页
     *
     * @return true 表示已成功发起启动
     */
    fun launchAppMain(context: Context): Boolean {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: return false

        // 设置启动标志，确保启动的是新的任务栈，并且清除当前任务栈
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    /**
     * 立即结束当前进程
     */
    fun exitProcessNow() {
        Process.killProcess(Process.myPid())
        exitProcess(PROCESS_EXIT_CODE)
    }
}