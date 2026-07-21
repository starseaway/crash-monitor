package com.xinyi.crash.page

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.xinyi.crash.R
import com.xinyi.crash.config.CrashAction
import com.xinyi.crash.config.CrashConfig
import com.xinyi.crash.config.CrashUiMode
import com.xinyi.crash.extensions.orUnknown
import com.xinyi.crash.handler.CrashProcessHelper
import com.xinyi.crash.utils.CrashTimeFormatter
import com.xinyi.crash.utils.StackTraceStyleFormatter

/**
 * 内置崩溃页
 *
 * - [CrashUiMode.INTERACTIVE]：展示崩溃摘要与日志路径，由用户选择退出或重启
 * - [CrashUiMode.TRANSITION]：展示提示后按默认动作自动退出或重启
 *
 * 子类可继承并覆写 [initLayoutId] / [onCrashReady]；完全自定义时请读取 [CrashIntent] 相关参数。
 *
 * @author 杨耿雷
 * @date 2026/7/16 10:10
 */
open class CrashActivity : Activity() {

    /**
     * 主线程处理器
     */
    private val mMainHandler = Handler(Looper.getMainLooper())

    /**
     * 进度动画
     */
    private var mProgressAnim: ObjectAnimator? = null

    /**
     * 是否已完成动作
     */
    private var isFinishedAction = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(initLayoutId())
        onCrashReady()
    }

    override fun onDestroy() {
        stopProgressAnimation()
        mMainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 崩溃页禁止返回
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 布局资源
     */
    protected open fun initLayoutId(): Int = R.layout.activity_crash

    /**
     * 布局加载完成后的回调
     */
    protected open fun onCrashReady() {
        bindCommonContent()
        when (resolveUiMode()) {
            CrashUiMode.INTERACTIVE -> setupInteractiveMode()
            CrashUiMode.TRANSITION -> setupTransitionMode()
        }
    }

    /**
     * 绑定 Logo、通用文案与崩溃摘要
     */
    protected open fun bindCommonContent() {
        val logoView = findViewById<ImageView?>(R.id.iv_crash_logo)
        val logoResId = intent.getIntExtra(CrashIntent.EXTRA_LOGO_RES_ID, 0)
        if (logoView != null && logoResId != 0) {
            logoView.setImageResource(logoResId)
            logoView.background = null
        }

        val customMessage = intent.getStringExtra(CrashIntent.EXTRA_MESSAGE)

        findViewById<CrashFieldItemView?>(R.id.field_crash_id)?.setField(
            label = getString(R.string.crash_label_id),
            value = intent.getStringExtra(CrashIntent.EXTRA_CRASH_ID).orUnknown()
        )

        val timestampMillis = intent.getLongExtra(CrashIntent.EXTRA_TIMESTAMP_MILLIS, 0L)
        findViewById<CrashFieldItemView?>(R.id.field_crash_time)?.setField(
            label = getString(R.string.crash_label_time),
            value = CrashTimeFormatter.format(timestampMillis).orUnknown()
        )

        findViewById<CrashFieldItemView?>(R.id.field_crash_version)?.setField(
            label = getString(R.string.crash_label_version),
            value = intent.getStringExtra(CrashIntent.EXTRA_APP_VERSION_NAME).orUnknown()
        )

        val exceptionType = intent.getStringExtra(CrashIntent.EXTRA_EXCEPTION_TYPE).orUnknown()
        val exceptionMessage = intent.getStringExtra(CrashIntent.EXTRA_EXCEPTION_MESSAGE)
        val exceptionText = if (exceptionMessage.isNullOrBlank()) exceptionType
                            else "$exceptionType: $exceptionMessage"
        findViewById<CrashFieldItemView?>(R.id.field_crash_exception)?.setField(
            label = getString(R.string.crash_label_exception),
            value = exceptionText
        )

        findViewById<CrashFieldItemView?>(R.id.field_crash_thread)?.setField(
            label = getString(R.string.crash_label_thread),
            value = intent.getStringExtra(CrashIntent.EXTRA_THREAD_NAME).orUnknown(),
            selectable = false
        )

        findViewById<CrashFieldItemView?>(R.id.field_crash_log_file)?.setField(
            label = getString(R.string.crash_label_log_file),
            value = intent.getStringExtra(CrashIntent.EXTRA_LOG_FILE_PATH).orUnknown()
        )

        bindStackSummary(
            intent.getStringExtra(CrashIntent.EXTRA_STACK_SUMMARY).orUnknown()
        )

        if (!customMessage.isNullOrBlank()) {
            findViewById<TextView?>(R.id.tv_crash_message)?.text = customMessage
        }
    }

    /**
     * 绑定堆栈摘要
     */
    protected open fun bindStackSummary(stackSummary: String) {
        val stackView = findViewById<TextView?>(R.id.tv_crash_stack) ?: return
        stackView.text = StackTraceStyleFormatter.format(
            stackTrace = stackSummary,
            stackColor = resolveColor(R.color.crash_stack),
            linkColor = resolveColor(R.color.crash_stack_link)
        )
    }

    /**
     * 解析颜色资源，兼容 API 19
     */
    private fun resolveColor(colorResId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColor(colorResId)
        } else {
            @Suppress("DEPRECATION")
            resources.getColor(colorResId)
        }
    }

    /**
     * 交互模式：展示详情与操作按钮
     */
    protected open fun setupInteractiveMode() {
        findViewById<TextView?>(R.id.tv_crash_title)?.setText(R.string.crash_title_interactive)
        if (intent.getStringExtra(CrashIntent.EXTRA_MESSAGE).isNullOrBlank()) {
            findViewById<TextView?>(R.id.tv_crash_message)
                ?.setText(R.string.crash_message_interactive)
        }
        findViewById<View?>(R.id.fl_crash_progress)?.visibility = View.GONE
        findViewById<View?>(R.id.sv_crash_detail)?.visibility = View.VISIBLE
        findViewById<View?>(R.id.ll_crash_actions)?.visibility = View.VISIBLE
        applyTransitionCentered(false)

        findViewById<TextView?>(R.id.btn_crash_exit)?.setOnClickListener {
            performExit()
        }
        findViewById<TextView?>(R.id.btn_crash_restart)?.setOnClickListener {
            performRestart()
        }
    }

    /**
     * 过渡模式：隐藏按钮，展示进度后自动执行默认动作
     */
    protected open fun setupTransitionMode() {
        val isRestart = resolveCrashAction() == CrashAction.RESTART
        findViewById<TextView?>(R.id.tv_crash_title)?.setText(
            if (isRestart) {
                R.string.crash_title_transition_restart
            } else {
                R.string.crash_title_transition_exit
            }
        )
        if (intent.getStringExtra(CrashIntent.EXTRA_MESSAGE).isNullOrBlank()) {
            findViewById<TextView?>(R.id.tv_crash_message)?.setText(
                if (isRestart) {
                    R.string.crash_message_transition_restart
                } else {
                    R.string.crash_message_transition_exit
                }
            )
        }
        findViewById<View?>(R.id.sv_crash_detail)?.visibility = View.GONE
        findViewById<View?>(R.id.ll_crash_actions)?.visibility = View.GONE
        findViewById<View?>(R.id.fl_crash_progress)?.visibility = View.VISIBLE
        applyTransitionCentered(true)
        startProgressAnimation()

        val displayMillis = intent.getLongExtra(
            CrashIntent.EXTRA_DISPLAY_MILLIS,
            CrashConfig.DEFAULT_CRASH_UI_DISPLAY_MILLIS
        ).coerceAtLeast(0L)
        mMainHandler.postDelayed({ performDefaultAction() }, displayMillis)
    }

    /**
     * 过渡页将 Logo / 标题 / 提示 / 进度垂直居中；交互页恢复顶对齐
     */
    private fun applyTransitionCentered(centered: Boolean) {
        val root = findViewById<LinearLayout?>(R.id.ll_crash_root) ?: return
        if (root.orientation == LinearLayout.VERTICAL) {
            root.gravity = if (centered) Gravity.CENTER else Gravity.TOP
            return
        }
        // 横屏：隐藏分隔线，侧栏内容居中
        findViewById<View?>(R.id.v_crash_divider)?.visibility =
            if (centered) View.GONE else View.VISIBLE
        val header = findViewById<View?>(R.id.ll_crash_header) ?: return
        val lp = header.layoutParams as? FrameLayout.LayoutParams ?: return
        lp.gravity = if (centered) {
            Gravity.CENTER
        } else {
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        header.layoutParams = lp
    }

    /**
     * 过渡模式结束后的默认动作
     */
    protected open fun performDefaultAction() {
        when (resolveCrashAction()) {
            CrashAction.RESTART -> performRestart()
            CrashAction.EXIT, CrashAction.SYSTEM_DEFAULT -> performExit()
        }
    }

    /**
     * 退出应用
     */
    protected open fun performExit() {
        if (isFinishedAction || isFinishing) {
            return
        }
        isFinishedAction = true
        CrashProcessHelper.exitProcessNow()
    }

    /**
     * 重启进入应用默认启动页
     */
    protected open fun performRestart() {
        if (isFinishedAction || isFinishing) {
            return
        }
        isFinishedAction = true

        val maxRestarts = intent.getIntExtra(
            CrashIntent.EXTRA_MAX_RESTARTS,
            CrashConfig.DEFAULT_MAX_RESTARTS_IN_WINDOW
        )
        val windowMillis = intent.getLongExtra(
            CrashIntent.EXTRA_RESTART_WINDOW_MILLIS,
            CrashConfig.DEFAULT_RESTART_WINDOW_MILLIS
        )
        if (!CrashProcessHelper.canRestartSafely(this, maxRestarts, windowMillis)) {
            CrashProcessHelper.exitProcessNow()
            return
        }
        CrashProcessHelper.markRestartAttempt(this, windowMillis)
        if (!CrashProcessHelper.launchAppMain(this)) {
            CrashProcessHelper.exitProcessNow()
            return
        }
        finish()
    }

    /**
     * 解析 UI 模式
     */
    protected open fun resolveUiMode(): CrashUiMode {
        val raw = intent.getStringExtra(CrashIntent.EXTRA_UI_MODE)
        return runCatching { CrashUiMode.valueOf(raw.orEmpty()) }
            .getOrDefault(CrashUiMode.INTERACTIVE)
    }
    
    /**
     * 解析崩溃动作
     */
    protected open fun resolveCrashAction(): CrashAction {
        val raw = intent.getStringExtra(CrashIntent.EXTRA_CRASH_ACTION)
        return runCatching { CrashAction.valueOf(raw.orEmpty()) }
            .getOrDefault(CrashAction.EXIT)
    }

    /**
     * 启动进度动画
     */
    protected open fun startProgressAnimation() {
        val progressView = findViewById<View?>(R.id.iv_crash_progress) ?: return
        stopProgressAnimation()
        mProgressAnim = ObjectAnimator.ofFloat(progressView, View.ROTATION, 0f, 360f).apply {
            duration = 1_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    /**
     * 停止进度动画
     */
    protected open fun stopProgressAnimation() {
        mProgressAnim?.cancel()
        mProgressAnim = null
        findViewById<View?>(R.id.iv_crash_progress)?.rotation = 0f
    }
}