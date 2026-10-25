package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** 个人中心：专注统计、改密、主题商店、退出登录 */
public class ProfileActivity extends AppCompatActivity {

    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(this);
        if (!UiHelper.ensureLoggedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_profile);
        setTitle(R.string.profile);
        UiHelper.setupBackButton(this);

        TextView tvAvatar = findViewById(R.id.tvAvatar);
        TextView tvAccount = findViewById(R.id.tvAccount);
        TextView btnLogout = findViewById(R.id.btnLogout);
        TextView btnChangePassword = findViewById(R.id.btnChangePassword);
        TextView btnThemeStore = findViewById(R.id.btnThemeStore);

        String username = userManager.getCurrentUsername();
        tvAccount.setText(username);
        if (username != null && !username.isEmpty()) {
            tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
        }

        btnThemeStore.setOnClickListener(v ->
                startActivity(new Intent(this, ThemeStoreActivity.class)));

        btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));

        btnLogout.setOnClickListener(v -> {
            userManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindStats();
        AppTheme theme = userManager.getCurrentTheme();
        UiHelper.refreshBackButtonTheme(this);
        ThemeHelper.applyToProfile(this, theme);
    }

    private void bindStats() {
        userManager.refreshProfile();
        bindStat(R.id.statFocusCount, String.valueOf(userManager.getFocusSessionCount()), R.string.stat_focus_count);
        bindStat(R.id.statAvgFocus, userManager.getAverageFocusDuration(), R.string.stat_avg_focus);
        bindStat(R.id.statFocusRate, userManager.getTotalFocusDurationHms(), R.string.stat_focus_duration);
        bindStat(R.id.statAbandonCount, String.valueOf(userManager.getAbandonCount()), R.string.stat_abandon_count);
        bindStat(R.id.statAvgAbandon, userManager.getAverageAbandonDuration(), R.string.stat_avg_abandon);
        bindStat(R.id.statPoints, String.valueOf(userManager.getPoints()), R.string.stat_points);
    }

    private void bindStat(int includeId, String value, int labelRes) {
        View root = findViewById(includeId);
        if (root == null) return;
        TextView tvVal = root.findViewById(R.id.tvStatValue);
        TextView tvLabel = root.findViewById(R.id.tvStatLabel);
        tvVal.setText(value);
        tvLabel.setText(labelRes);
    }
}