package com.xinyi.crash.utils

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan

/**
 * 堆栈摘要样式格式化
 *
 * 仿 Android Studio Logcat：主体暗红，`(文件:行号)` 为蓝色链接样式。
 *
 * @author 杨耿雷
 * @date 2026/7/16 10:50
 */
object StackTraceStyleFormatter {

    /**
     * 匹配堆栈中的源码位置，例如 `(MainActivity.java:24)`、`(Foo.kt:10)`
     */
    private val SOURCE_LOCATION = Regex("""\(([A-Za-z0-9_./$]+\.(?:java|kt):\d+)\)""")

    /**
     * 将纯文本堆栈格式化为带颜色的可展示文本
     *
     * @param stackTrace 堆栈原文；空或空白时原样返回
     * @param stackColor 主体文字颜色（ARGB）
     * @param linkColor 源码位置颜色（ARGB）
     * @param underlineLink 是否为源码位置加下划线
     */
    @JvmStatic
    @JvmOverloads
    fun format(
        stackTrace: CharSequence,
        stackColor: Int,
        linkColor: Int,
        underlineLink: Boolean = true
    ): CharSequence {
        if (stackTrace.isBlank()) {
            return stackTrace
        }

        val text = stackTrace.toString()
        val spannable = SpannableString(text)
        spannable.setSpan(
            ForegroundColorSpan(stackColor),
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        SOURCE_LOCATION.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            spannable.setSpan(
                ForegroundColorSpan(linkColor),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (underlineLink) {
                spannable.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return spannable
    }
}