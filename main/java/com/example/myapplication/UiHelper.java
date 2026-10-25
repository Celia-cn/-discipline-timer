package com.example.myapplication;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * UI 工具类：状态栏/返回按钮适配、登录校验、主题化返回键样式。
 */
public final class UiHelper {

    private UiHelper() {
    }

    /** 为顶部标题栏预留状态栏高度 */
    public static void applyStatusBarPadding(AppCompatActivity activity, View header) {
        if (header == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), insets.top + dp(activity, 8),
                    v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    /** 为带左上角返回按钮的页面内容区预留顶部空间，避免与返回按钮重叠 */
    public static void applyContentBelowBackButton(AppCompatActivity activity, View content) {
        if (content == null) return;
        final int backHeight = dp(activity, 48);
        final int gapBelowBack = dp(activity, 16);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            int top = insets.top + dp(activity, 8) + backHeight + gapBelowBack;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    /** 未登录则跳转登录页并结束当前页 */
    public static boolean ensureLoggedIn(AppCompatActivity activity) {
        if (new UserManager(activity).isLoggedIn()) {
            return true;
        }
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return false;
    }

    public static void setupBackButton(AppCompatActivity activity) {
        setupBackButton(activity, v -> activity.finish());
    }

    /** 返回按钮在页面标题栏内时使用，状态栏间距由标题栏统一处理 */
    public static void setupBackButtonInHeader(AppCompatActivity activity) {
        setupBackButton(activity, v -> activity.finish(), false);
    }

    public static void setupBackButton(AppCompatActivity activity, View.OnClickListener listener) {
        setupBackButton(activity, listener, true);
    }

    private static void setupBackButton(AppCompatActivity activity, View.OnClickListener listener,
                                        boolean applyStatusBarMargin) {
        TextView btnBack = activity.findViewById(R.id.btnBack);
        if (btnBack == null) return;
        applyThemeToBackButton(activity, btnBack);
        btnBack.setOnClickListener(listener);
        if (applyStatusBarMargin) {
            applyStatusBarMargin(activity, btnBack);
        }
    }

    public static void refreshBackButtonTheme(AppCompatActivity activity) {
        TextView btnBack = activity.findViewById(R.id.btnBack);
        if (btnBack != null) {
            applyThemeToBackButton(activity, btnBack);
        }
    }

    private static void applyThemeToBackButton(AppCompatActivity activity, TextView btnBack) {
        AppTheme theme = new UserManager(activity).getCurrentTheme();
        ThemeHelper.styleThemedBackButton(btnBack, theme, activity);
    }

    private static void applyStatusBarMargin(AppCompatActivity activity, TextView btnBack) {
        ViewCompat.setOnApplyWindowInsetsListener(btnBack, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.topMargin = insets.top + dp(activity, 8);
            v.setLayoutParams(lp);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(btnBack);
    }

    private static int dp(AppCompatActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}