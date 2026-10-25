package com.example.myapplication;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

/** 按 AppTheme 渲染圆角背景、首页时钟区域、返回按钮等 */
public final class ThemeHelper {

    private ThemeHelper() {
    }

    public static GradientDrawable roundedRect(int color, float radiusDp, Context ctx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setCornerRadius(radiusDp * ctx.getResources().getDisplayMetrics().density);
        gd.setColor(color);
        return gd;
    }

    public static GradientDrawable roundedGradient(AppTheme theme, float radiusDp, Context ctx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{theme.btn, theme.primaryLight});
        gd.setCornerRadius(radiusDp * ctx.getResources().getDisplayMetrics().density);
        return gd;
    }

    public static GradientDrawable circle(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        return gd;
    }

    public static void applyToHome(HomeActivity activity, AppTheme theme, UserManager um) {
        ImageView bg = activity.findViewById(R.id.imgBackground);
        TextView tvTotal = activity.findViewById(R.id.tvTotalTime);
        TextView tvPoints = activity.findViewById(R.id.tvPoints);
        TextView btnStart = activity.findViewById(R.id.btnStartFocus);
        TextClock digital = activity.findViewById(R.id.tvDigitalTime);
        AnalogSystemClockView clock = activity.findViewById(R.id.clockView);
        ImageButton profile = activity.findViewById(R.id.btnProfile);
        View cardStats = activity.findViewById(R.id.cardStats);

        bg.setImageResource(theme.bgDrawable);
        tvTotal.setTextColor(theme.primaryDark);
        tvPoints.setBackground(roundedRect(withAlpha(theme.primaryDark, 0xCC), 24f, activity));
        btnStart.setBackground(roundedGradient(theme, 12f, activity));
        digital.setBackground(roundedRect(withAlpha(theme.primaryDark, 0xB3), 12f, activity));

        clock.setThemeColors(theme.primary, theme.primaryDark, theme.primaryLight);

        profile.setBackground(createGlassCircle(activity, theme));
        if (profile.getDrawable() != null) {
            profile.getDrawable().setTint(theme.primaryDark);
        }
    }

    public static void applyToProfile(ProfileActivity activity, AppTheme theme) {
        ImageView bg = activity.findViewById(R.id.imgBackground);
        TextView avatar = activity.findViewById(R.id.tvAvatar);
        TextView btnLogout = activity.findViewById(R.id.btnLogout);
        TextView btnChangePassword = activity.findViewById(R.id.btnChangePassword);
        View statsPanel = activity.findViewById(R.id.statsPanel);

        if (bg != null) bg.setImageResource(theme.bgDrawable);
        if (avatar != null) avatar.setBackground(circle(theme.primary));
        if (btnLogout != null) btnLogout.setBackground(roundedRect(theme.btn, 24f, activity));
        if (btnChangePassword != null) {
            btnChangePassword.setTextColor(theme.primaryDark);
        }
        TextView btnThemeStore = activity.findViewById(R.id.btnThemeStore);
        if (btnThemeStore != null) btnThemeStore.setTextColor(theme.primaryDark);
        if (statsPanel != null) {
            View accent = statsPanel.findViewById(R.id.statsAccent);
            if (accent != null) accent.setBackgroundColor(theme.primary);
        }
    }

    public static void styleThemedBackButton(TextView btnBack, AppTheme theme, Context ctx) {
        if (btnBack == null) return;
        btnBack.setTextColor(theme.primaryDark);
        btnBack.setBackground(createGlassCircle(ctx, theme));
    }

    public static void applyThemeStoreHeader(ThemeStoreActivity activity, AppTheme theme) {
        TextView title = activity.findViewById(R.id.tvPageTitle);
        TextView tvPoints = activity.findViewById(R.id.tvPointsBalance);
        View accent = activity.findViewById(R.id.headerAccent);
        if (title != null) title.setTextColor(theme.primaryDark);
        if (tvPoints != null) tvPoints.setTextColor(theme.primaryDark);
        if (accent != null) accent.setBackgroundColor(theme.primary);
    }

    private static GradientDrawable createGlassCircle(Context ctx, AppTheme theme) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(0xD9FFFFFF);
        gd.setStroke((int) (1.5f * ctx.getResources().getDisplayMetrics().density), withAlpha(theme.primary, 0xAA));
        return gd;
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}