package com.xinyi.crash.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃时间格式化
 *
 * 线程安全：使用 [ThreadLocal] 为每个线程维护独立 [SimpleDateFormat] 实例。
 *
 * @author 杨耿雷
 * @date 2026/7/16 11:32
 */
object CrashTimeFormatter {

    /**
     * 时间格式化器
     */
    private val mCrashTimeFormat = object : ThreadLocal<SimpleDateFormat>() {
        
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.CHINA)
        }
    }

    /**
     * 格式化崩溃发生时间（含时区）
     *
     * @param timestampMillis 毫秒时间戳；小于等于 0 时返回空串
     */
    @JvmStatic
    fun format(timestampMillis: Long): String {
        if (timestampMillis <= 0L) {
            return ""
        }
        return mCrashTimeFormat.get()!!.format(Date(timestampMillis))
    }
}