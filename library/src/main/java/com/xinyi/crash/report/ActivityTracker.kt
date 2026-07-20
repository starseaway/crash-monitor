package com.xinyi.crash.report

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/**
 * 前台页面与进程前后台跟踪器
 *
 * 为 [CrashReportBuilder] 提供当前页面与应用是否处于前台的上下文。
 *
 * > 仅持有 Activity 弱引用，避免延长页面生命周期。
 *
 * @author 杨耿雷
 * @date 2026/7/14 15:18
 */
internal class ActivityTracker : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var currentActivityReference: WeakReference<Activity>? = null

    /**
     * 已进入 Started 状态的 Activity 数量；大于 0 视为应用位于前台
     */
    private val startedActivityCount = AtomicInteger(0)

    /**
     * 当前前台 Activity 类名；无前台页面时返回 null
     */
    val currentActivityName: String?
        get() = currentActivityReference?.get()?.javaClass?.name

    /**
     * 应用是否处于前台
     *
     * 以 Activity 的 Started 计数为准：至少一个 Activity 处于 Started 即视为前台。
     */
    val isAppInForeground: Boolean
        get() = startedActivityCount.get() > 0

    override fun onActivityResumed(activity: Activity) {
        currentActivityReference = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        val currentActivity = currentActivityReference?.get()
        if (currentActivity === activity) {
            currentActivityReference = null
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        val currentActivity = currentActivityReference?.get()
        if (currentActivity === activity) {
            currentActivityReference = null
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount.incrementAndGet()
    }

    override fun onActivityStopped(activity: Activity) {
        while (true) {
            val current = startedActivityCount.get()
            val next = (current - 1).coerceAtLeast(0)
            if (startedActivityCount.compareAndSet(current, next)) {
                return
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}