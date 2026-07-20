package com.xinyi.app.crash;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.xinyi.device.app.AppManager;

/**
 * 测试界面
 *
 * @author 新一
 * @date 2026/7/16 14:05
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 显示版本名
        TextView appVersion = findViewById(R.id.tv_app_version);
        appVersion.setText(AppManager.getAppVersionName());

        findViewById(R.id.tv_trigger_crash).setOnClickListener(view -> {
            throw new IllegalStateException("Crash Monitor 演示崩溃");
        });
    }
}