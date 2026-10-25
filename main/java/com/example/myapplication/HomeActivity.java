package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 首页：展示累计自律时长、积分、当前主题背景；
 * 入口：开始专注、个人中心。
 */
public class HomeActivity extends AppCompatActivity {

    private UserManager userManager;
    private ImageView imgBackground;
    private TextView tvTotalTime;
    private TextView tvPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(this);
        if (!userManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_home);

        imgBackground = findViewById(R.id.imgBackground);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvPoints = findViewById(R.id.tvPoints);
        ImageButton btnProfile = findViewById(R.id.btnProfile);
        TextView btnStartFocus = findViewById(R.id.btnStartFocus);
        applyStatusBarMargin(btnProfile);

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        btnStartFocus.setOnClickListener(v ->
                startActivity(new Intent(this, FocusSetupActivity.class)));
    }

    /** 从其他页面返回时拉取最新用户数据（积分/时长可能已变） */
    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void applyStatusBarMargin(ImageButton btnProfile) {
        ViewCompat.setOnApplyWindowInsetsListener(btnProfile, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.topMargin = insets.top + dp(8);
            v.setLayoutParams(lp);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(btnProfile);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void refreshUi() {
        userManager.refreshProfile();
        AppTheme theme = userManager.getCurrentTheme();
        tvTotalTime.setText(UserManager.formatDuration(userManager.getTotalSeconds()));
        tvPoints.setText(getString(R.string.points_balance, userManager.getPoints()));
        ThemeHelper.applyToHome(this, theme, userManager);
    }
}