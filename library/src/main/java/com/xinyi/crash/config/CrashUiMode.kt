package com.xinyi.crash.config

/**
 * 崩溃后界面展示模式
 *
 * 仅在 [CrashConfig.showCrashUi] 为 true 时生效。
 *
 * @author 杨耿雷
 * @date 2026/7/16 9:48
 */
enum class CrashUiMode {

    /**
     * 交互页：停留查看崩溃摘要与日志路径，由用户选择退出或重启
     */
    INTERACTIVE,

    /**
     * 过渡页：展示提示后按 [CrashConfig.crashAction] 自动退出或重启
     */
    TRANSITION
}