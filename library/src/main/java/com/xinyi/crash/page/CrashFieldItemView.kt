package com.xinyi.crash.page

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.xinyi.crash.R

/**
 * 崩溃详情 KV 条目
 *
 * @author 新一
 * @date 2026/7/20 17:32
 */
class CrashFieldItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    /**
     * 标签文本视图
     */
    private val labelView: TextView

    /**
     * 值文本视图
     */
    private val valueView: TextView

    /**
     * 标签
     */
    var label: String = ""
        private set

    /**
     * 值
     */
    var value: String = ""
        private set

    init {
        orientation = HORIZONTAL
        gravity = Gravity.TOP
        LayoutInflater.from(context).inflate(R.layout.item_crash_field, this, true)
        labelView = findViewById(R.id.tv_crash_field_label)
        valueView = findViewById(R.id.tv_crash_field_value)
    }

    /**
     * 设置标签与值
     *
     * @param label 字段名
     * @param value 字段值
     * @param selectable 值是否允许长按复制，默认 true
     */
    fun setField(label: String, value: String, selectable: Boolean = true) {
        this.label = label
        this.value = value
        labelView.text = label.plus("：")
        valueView.text = value
        valueView.setTextIsSelectable(selectable)
    }
}