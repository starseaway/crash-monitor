package com.xinyi.app.crash

import android.app.Application
import com.xinyi.crash.CrashMonitor
import com.xinyi.crash.config.CrashAction
import com.xinyi.crash.config.CrashUiMode
import com.xinyi.device.DeviceContext
import com.xinyi.ember.Ember

class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化设备工具包库
        DeviceContext.init(this)

        // 初始化日志框架
        Ember.init(Ember.builder().build())

        // 初始化崩溃监听器框架
        val crashConfig = CrashMonitor.builder()
            .setCrashAction(CrashAction.EXIT)
            .setShowCrashUi(true)
            .setCrashUiMode(CrashUiMode.TRANSITION)
            .build()
        CrashMonitor.init(this, crashConfig) { report ->
            // 此时文件已同步写完；不要在这里杀进程或重启
            Ember.i("崩溃报告 ${report.crashId} 已保存至：${CrashMonitor.getLogFilePath()}")
        }
    }
}