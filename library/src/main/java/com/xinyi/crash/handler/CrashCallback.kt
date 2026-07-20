package com.xinyi.crash.handler

import com.xinyi.crash.report.CrashReport

/**
 * 崩溃报告回调
 *
 * 此接口在专用崩溃日志同步写入本地文件之后、执行退出/重启等后置动作之前调用。
 *
 * > 回调应尽快返回；不要在这里自行杀进程或重启应用；这些由框架按配置完成。
 *
 * @author 杨耿雷
 * @date 2026/7/14 16:45
 */
fun interface CrashCallback {

    /**
     * 当崩溃报告处理完成时调用
     *
     * @param report 结构化崩溃报告
     */
    fun onCrash(report: CrashReport)
}