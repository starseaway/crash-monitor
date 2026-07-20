@file:JvmName("StringExecutor")

package com.xinyi.crash.extensions

/**
 * 空值 / 空白文本占位扩展
 *
 * @author 杨耿雷
 * @date 2026/7/15 15:25
 */

/** 未知占位文案 */
internal const val PLACEHOLDER_UNKNOWN = "未知"

/** 无内容占位文案 */
internal const val PLACEHOLDER_NONE = "无"

/**
 * 空或空白时返回占位文案，否则返回原字符串
 *
 * @param placeholder 占位文案，默认 [PLACEHOLDER_UNKNOWN]
 */
internal fun String?.orPlaceholder(placeholder: String = PLACEHOLDER_UNKNOWN): String {
    return if (isNullOrBlank()) placeholder else this
}

/**
 * 空或空白时返回「未知」
 */
internal fun String?.orUnknown(): String = orPlaceholder(PLACEHOLDER_UNKNOWN)

/**
 * 空或空白时返回「无」
 */
internal fun String?.orNone(): String = orPlaceholder(PLACEHOLDER_NONE)

/**
 * 将任意可空对象转为展示文本；对象为 null 或其 [Any.toString] 为空/空白时返回占位文案
 *
 * @param placeholder 占位文案，默认 [PLACEHOLDER_UNKNOWN]
 */
internal fun Any?.toDisplayString(placeholder: String = PLACEHOLDER_UNKNOWN): String {
    return this?.toString().orPlaceholder(placeholder)
}