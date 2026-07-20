@file:JvmName("StringBuilderExecutor")

package com.xinyi.crash.extensions

/**
 * [StringBuilder] 文本拼装扩展
 *
 * @author 杨耿雷
 * @date 2026/7/15 15:10
 */

/** 分区标题默认分隔线 */
private const val DEFAULT_SECTION_DIVIDER = "========================================"

/**
 * 追加一行文本（兼容 API 19，不依赖 [StringBuilder.appendLine]）
 *
 * @param value 行内容
 */
internal fun StringBuilder.appendLineCompat(value: String): StringBuilder {
    return append(value).append('\n')
}

/**
 * 追加「名称: 值」字段行
 *
 * @param name 字段名
 * @param value 字段值
 */
internal fun StringBuilder.appendField(name: String, value: String): StringBuilder {
    return appendLineCompat("$name: $value")
}

/**
 * 追加分隔线与分区标题
 *
 * @param title 分区标题
 * @param divider 分隔线；默认等号线
 */
internal fun StringBuilder.appendSection(
    title: String,
    divider: String = DEFAULT_SECTION_DIVIDER
): StringBuilder {
    appendLineCompat(divider)
    return appendLineCompat("$title:")
}

/**
 * 追加分区标题及其下的键值映射
 *
 * @param title 分区标题
 * @param values 键值映射
 * @param divider 分隔线；默认等号线
 */
internal fun StringBuilder.appendMapSection(
    title: String,
    values: Map<String, String>,
    divider: String = DEFAULT_SECTION_DIVIDER
): StringBuilder {
    appendSection(title, divider)
    values.forEach { (key, value) ->
        appendField(key, value)
    }
    return this
}