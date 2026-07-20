# Crash Monitor 安卓运行时异常检测框架

An Android runtime diagnostics framework for detecting exceptions, collecting execution context, and generating actionable diagnostic reports.

<div align="center">

  <img src="readme/img/spider_xiaohei.png" width="260" alt="Ember Logo">

</div>

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![License](https://img.shields.io/badge/license-Apache%202.0-green)
![API](https://img.shields.io/badge/API-19%2B-brightgreen)

> 察微于无常，定格于瞬息。
> 
> Perceive the volatile, capture the fleeting.

## 一、简介

`Crash Monitor` 是一个专注于 Android 运行时异常感知与信息采集的开源库。

框架目前的核心能力集中在 Java / Kotlin 未捕获异常（Uncaught Exception）的拦截与上下文信息还原。
当应用发生 Fatal Crash 时，框架会在进程终止前，把崩溃发生那一刻的堆栈、设备参数、Activity 页面栈等数据同步写入本地文件，尽可能保障崩溃上下文不丢失。

> 在后续规划中，除了 Java / Kotlin 层异常，框架还将陆续集成 Native 信号崩溃（SIGSEGV / SIGABRT）监听、ANR 异常检测等底层捕获能力。

目前初始化后，框架会自动接管 `Thread.setDefaultUncaughtExceptionHandler`。发生崩溃时，依次完成崩溃报告组装、同步写入本地文件、业务回调，再按配置退出或重启。

**注意**：框架已内置安全退出与重启防护逻辑，业务侧无需在回调中手动调用 `Process.killProcess` 或编写重启逻辑。

控制台输出和日志本地存储基于 [Ember](https://github.com/starseaway/ember) 框架完成：

- 控制台通过 `Ember.e()` 输出。
- 崩溃文件通过 Ember `FileLogger` 同步写入。

默认日志目录为应用私有路径 `filesDir/crash`，无需申请外部存储权限。

### 能力概览

框架始终会围绕 **“感知 - 捕获 - 记录 - 防护”** 的核心链路，目前已提供以下能力：

- 统一接管 Java / Kotlin 未处理异常
- 记录应用、设备、线程、页面与自定义上下文
- 同步写入本地日志，避免进程死亡日志丢失
- 内置崩溃页，支持竖屏 / 横屏两版布局，亦可切为过渡页或完全自定义
- 支持退出、重启、系统默认处理，与连续崩溃重启的死循环保护

### 演示效果

## 二、SDK 适用范围

| 项目         | 要求                                                             |
|------------|----------------------------------------------------------------|
| Min SDK    | 19（Android 4.4）及以上                                             |
| JVM Target | 1.8                                                            |
| Kotlin     | 1.9+                                                           |
| 日志依赖       | [Ember v1.0.0](https://github.com/starseaway/ember/tree/1.0.0) |

## 三、集成方式

### 1. 添加仓库

```groovy
maven {
    url 'https://jitpack.io'
}
```

### 2. 添加依赖

Groovy：

```groovy
implementation 'com.github.starseaway:crash-monitor:1.0.0'
```

Kotlin DSL：

```kotlin
implementation("com.github.starseaway:crash-monitor:1.0.0")
```

框架会传递引入 `Ember` 依赖库，无需重复声明相同版本。

### 3. 初始化

```kotlin
class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化日志框架
        Ember.init(Ember.builder().build())

        // 初始化崩溃监听器框架
        val crashConfig = CrashMonitor.builder()
            .setCrashAction(CrashAction.RESTART) // EXIT / RESTART / SYSTEM_DEFAULT
            .setShowCrashUi(true) // 是否展示界面
            .setCrashUiMode(CrashUiMode.INTERACTIVE) // 或 TRANSITION
            // .setCrashActivity(CrashActivity::class.java) // 默认可省略；可换成自定义/子类
            // .setCrashUiDisplayMillis(1_500L) // 仅 TRANSITION 生效
            // .setCrashUiLogo(R.drawable.ic_launcher) // 崩溃界面的 Logo 显示
            // .setRestartLoopProtection(maxRestarts = 3, windowMillis = 60_000L) // 设置重启循环保护参数
            // .setLogDirectory(File(...)) // 自定义崩溃日志文件目录
            // .setMaxFileSize(2 * 1024 * 1024) // 单个崩溃日志文件大小上限（超上限会按序号创建新的文件）
            // .setRetainMonthCount(3) // 设置本地日志文件保留的月数
            .putCustomInfo("渠道", BuildConfig.FLAVOR)
            .build()
        CrashMonitor.init(this, crashConfig) { report ->
            // 此时文件已同步写完；不要在这里杀进程或重启
            Ember.i("崩溃报告 ${report.crashId} 已保存至：${CrashMonitor.getLogFilePath()}")
        }
    }
}
```

更多配置项说明可查看源码：[CrashConfig.kt](library/src/main/java/com/xinyi/crash/config/CrashConfig.kt)。

## 四、核心能力概述

### 1. 两维后置

同步写完日志文件，并对外回调完成后，按以下两个维度决定后置行为：

| 维度                     | 选项                                                     |
|------------------------|--------------------------------------------------------|
| **默认动作** `CrashAction` | `EXIT` / `RESTART` / `SYSTEM_DEFAULT`                  |
| **界面模式** `CrashUiMode` | `INTERACTIVE` / `TRANSITION`（仅 `showCrashUi=true` 时生效） |

#### （1）默认动作 CrashAction

| 动作               | 行为                                             |
|------------------|------------------------------------------------|
| `EXIT`           | 写入日志并回调完成后结束进程；有崩溃页时由用户退出，或过渡后自动退出。            |
| `RESTART`        | 写入日志并回调完成后重启应用；有崩溃页时由用户重启，或过渡后自动重启；超限则改为 EXIT。 |
| `SYSTEM_DEFAULT` | 交给原本默认的未捕获异常处理器，不进入框架崩溃页。                      |

#### （2）界面模式 CrashUiMode

仅 `showCrashUi=true` 时展示崩溃页；为 `false` 时跳过界面，直接执行 `CrashAction`。

| 模式            | 说明                                    |
|---------------|---------------------------------------|
| `INTERACTIVE` | 停留查看崩溃摘要（含时间、版本、日志文件路径等）；按钮「退出」「重新启动」 |
| `TRANSITION`  | 展示过渡提示后，按 `CrashAction` 自动退出或重启       |

内置 `CrashActivity` 由库 Manifest 自动合并，提供竖屏 / 横屏布局；也可继承覆写或完全自定义。

#### （3）建议组合

| 应用场景        | 处理方式    | 配置组合                                                       |
|-------------|---------|------------------------------------------------------------|
| 调试 / 内测     | 先看再选    | `setShowCrashUi(true)` + `INTERACTIVE`：保留现场，手动选择退出或重启      |
| 正式发布        | 静默重启    | `RESTART` + `setShowCrashUi(false)`：崩溃后静默重启，用户无感知          |
| 正式发布（需短暂提示） | 过渡后自动执行 | `RESTART` + `setShowCrashUi(true)` + `TRANSITION`：过渡页后自动重启 |
| 放弃恢复、直接退出   | 静默退出    | `EXIT` + `setShowCrashUi(false)`                           |

### 2. 自定义崩溃 Activity

完全自定义崩溃 `Activity` 时，通过 `CrashIntent` 读取 Extra（崩溃摘要、日志路径、模式等）：

```kotlin
class AppCrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashId = intent.getStringExtra(CrashIntent.EXTRA_CRASH_ID)
        val timeMillis = intent.getLongExtra(CrashIntent.EXTRA_TIMESTAMP_MILLIS, 0L)
        val version = intent.getStringExtra(CrashIntent.EXTRA_APP_VERSION_NAME)

        // 两个路径都是绝对路径
        val logDir = intent.getStringExtra(CrashIntent.EXTRA_LOG_DIRECTORY)
        val logFile = intent.getStringExtra(CrashIntent.EXTRA_LOG_FILE_PATH)

        val stack = intent.getStringExtra(CrashIntent.EXTRA_STACK_SUMMARY)
        // ...
    }
}
```

> Intent 只传核心摘要信息，避免 Binder 过大；完整崩溃报告（含设备、应用、运行时信息与完整堆栈）以本地日志文件为准。

### 3. 运行时能力

`CrashConfig` 在 `init` 时一次性生效，不支持整份配置热更新。

如果需要在运行过程中添加一些自定义的动态信息，可以通过运行时能力进行补充，这些信息会写入下一次崩溃报告中的「崩溃上下文」区块。

| API                         | 说明           |
|-----------------------------|--------------|
| `putCustomInfo(key, value)` | 追加或覆盖一条自定义信息 |
| `removeCustomInfo(key)`     | 移除一条自定义信息    |

初始化时也可通过 `CrashMonitor.builder().putCustomInfo(...)` 写入相对固定的信息，与运行时写入的内容会合并进同一份崩溃报告。

此外，框架还提供了一些运行时管理能力，便于获取日志、清理历史数据以及管理框架状态。

| API                               | 说明                            |
|-----------------------------------|-------------------------------|
| `getLogFilePath()`                | 获取当前正在写入的日志文件绝对路径             |
| `clearLogFiles(retainMonthCount)` | 清理指定月数以前的所有日志目录&文件            |
| `unregister()`                    | 注销监听器，恢复先前被本框架替换掉的前一个未捕获异常处理器 |
| `isRegistered()`                  | 框架是否已注册                       |