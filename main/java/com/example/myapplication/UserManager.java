package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 业务门面（Model 层）：对外提供注册、登录、专注记录、主题等业务。
 * 内部通过 ApiClient 访问后端；内存缓存 UserProfile；仅持久化当前用户名。
 */
public class UserManager {

    /** 测试账号（需 MySQL 已执行 timer.sql）：demo / 123456 */
    public static final String DEFAULT_USERNAME = "demo";
    public static final String DEFAULT_PASSWORD = "123456";

    public static final String THEME_GREEN = "green";
    public static final String THEME_BLUE = "blue";
    public static final String THEME_YELLOW = "yellow";
    public static final String THEME_ORANGE = "orange";
    public static final String THEME_RED = "red";

    public static final String BG_GREEN = THEME_GREEN;
    public static final String BG_BLUE = THEME_BLUE;
    public static final String BG_YELLOW = THEME_YELLOW;
    public static final String BG_ORANGE = THEME_ORANGE;
    public static final String BG_RED = THEME_RED;
    public static final int UNLOCK_COST = 100;

    private static final String PREFS = "discipline_timer_prefs";
    private static final String KEY_CURRENT = "current_user";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final SharedPreferences prefs;
    private final ApiClient api;
    private UserProfile cache;

    public UserManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        api = new ApiClient();
        restoreCacheIfLoggedIn();
    }

    private void restoreCacheIfLoggedIn() {
        String username = getCurrentUsername();
        if (username == null) {
            return;
        }
        UserProfile profile = runIo(() -> api.fetchProfile(username), null);
        if (profile != null) {
            cache = profile;
        }
    }

    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty()) return false;
        if (password == null || password.isEmpty()) return false;
        final String u = username.trim();
        return Boolean.TRUE.equals(runIo(() -> api.register(u, password), false));
    }

    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        final String u = username.trim();
        UserProfile profile = runIo(() -> api.login(u, password), null); // 网络在子线程
        if (profile == null) {
            return false;
        }
        prefs.edit().putString(KEY_CURRENT, u).apply();
        cache = profile;
        return true;
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        if (oldPassword == null || newPassword == null || newPassword.isEmpty()) return false;
        String username = getCurrentUsername();
        if (username == null) return false;
        Boolean ok = runIo(() -> api.changePassword(username, oldPassword, newPassword), false);
        return Boolean.TRUE.equals(ok);
    }

    public boolean resetPassword(String username, String newPassword) {
        if (username == null || newPassword == null || newPassword.isEmpty()) return false;
        final String u = username.trim();
        return Boolean.TRUE.equals(runIo(() -> api.resetPassword(u, newPassword), false));
    }

    public boolean userExists(String username) {
        if (username == null) return false;
        return Boolean.TRUE.equals(runIo(() -> api.userExists(username.trim()), false));
    }

    public void logout() {
        prefs.edit().remove(KEY_CURRENT).apply();
        cache = null;
    }

    public String getCurrentUsername() {
        return prefs.getString(KEY_CURRENT, null);
    }

    public boolean isLoggedIn() {
        return getCurrentUsername() != null && cache != null;
    }

    public boolean isServerReachable() {
        return Boolean.TRUE.equals(runIo(() -> api.isServerReachable(), false)); // 必须在子线程访问网络
    }

    public int getPoints() {
        return cache != null ? cache.points : 0;
    }

    public long getTotalSeconds() {
        return cache != null ? cache.totalSeconds : 0L;
    }

    public int getFocusCount() {
        return cache != null ? cache.focusCount : 0;
    }

    public int getAbandonCount() {
        return cache != null ? cache.abandonCount : 0;
    }

    public long getTotalAbandonSeconds() {
        return cache != null ? cache.totalAbandonSeconds : 0L;
    }

    public int getFishCount() {
        return cache != null ? cache.fishCount : 0;
    }

    public void recordFocusComplete(long seconds) {
        String username = getCurrentUsername();
        if (username == null) return;
        UserProfile updated = runIo(() -> api.recordFocusComplete(username, seconds), null);
        if (updated != null) {
            cache = updated;
        }
    }

    public void recordFocusAbandon(long sessionDurationSeconds, long remainingSeconds) {
        String username = getCurrentUsername();
        if (username == null) return;
        UserProfile updated = runIo(() -> api.recordFocusAbandon(username, sessionDurationSeconds, remainingSeconds), null);
        if (updated != null) {
            cache = updated;
        }
    }

    public int getFocusSessionCount() {
        return getFocusCount() + getAbandonCount();
    }

    public String getTotalFocusDurationHms() {
        return formatDurationHms(getTotalSeconds());
    }

    public String getAverageFocusDuration() {
        int count = getFocusSessionCount();
        if (count == 0) return formatDurationHms(0);
        return formatDurationHms(getTotalSeconds() / count);
    }

    public String getAverageAbandonDuration() {
        int count = getAbandonCount();
        if (count == 0) return formatDurationHms(0);
        return formatDurationHms(getTotalAbandonSeconds() / count);
    }

    public AppTheme getCurrentTheme() {
        return AppTheme.byId(getSelectedTheme());
    }

    public String getSelectedTheme() {
        return getSelectedBackground();
    }

    public void setSelectedTheme(String themeId) {
        setSelectedBackground(themeId);
    }

    public boolean isThemeUnlocked(String themeId) {
        return isBackgroundUnlocked(themeId);
    }

    public boolean unlockTheme(String themeId) {
        return unlockBackground(themeId, AppTheme.byId(themeId).unlockCost);
    }

    public String getSelectedBackground() {
        if (cache == null) return BG_GREEN;
        return cache.selectedBg != null ? cache.selectedBg : BG_GREEN;
    }

    public void setSelectedBackground(String bgId) {
        String username = getCurrentUsername();
        if (username == null) return;
        UserProfile updated = runIo(() -> api.selectTheme(username, bgId), null);
        if (updated != null) {
            cache = updated;
        }
    }

    public boolean isBackgroundUnlocked(String bgId) {
        if (BG_GREEN.equals(bgId)) return true;
        if (cache == null || cache.unlockedBgs == null) return false;
        return cache.unlockedBgs.contains(bgId);
    }

    public boolean unlockBackground(String bgId) {
        return unlockBackground(bgId, UNLOCK_COST);
    }

    private boolean unlockBackground(String bgId, int cost) {
        if (BG_GREEN.equals(bgId)) return true;
        if (isBackgroundUnlocked(bgId)) return true;
        if (cost > 0 && getPoints() < cost) return false;
        String username = getCurrentUsername();
        if (username == null) return false;
        UserProfile updated = runIo(() -> api.unlockTheme(username, bgId), null);
        if (updated != null) {
            cache = updated;
            return true;
        }
        return false;
    }

    public int getBackgroundDrawableId(String bgId) {
        switch (bgId) {
            case BG_BLUE: return R.drawable.blue;
            case BG_YELLOW: return R.drawable.yellow;
            case BG_ORANGE: return R.drawable.orange;
            case BG_RED: return R.drawable.red;
            default: return R.drawable.green;
        }
    }

    public void refreshProfile() {
        String username = getCurrentUsername();
        if (username == null) return;
        UserProfile profile = runIo(() -> api.fetchProfile(username), null);
        if (profile != null) {
            cache = profile;
        }
    }

    public static int pointsForDuration(long seconds) {
        return (int) (seconds / 600);
    }

    public static String formatDuration(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) {
            return h + "小时" + m + "分钟" + s + "秒";
        }
        if (m > 0) {
            return m + "分钟" + s + "秒";
        }
        return s + "秒";
    }

    public static String formatHms(int hours, int minutes, int seconds) {
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String formatDurationHms(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        if (h > 0 && shown < 2) {
            sb.append(h).append('h');
            shown++;
        }
        if (m > 0 && shown < 2) {
            sb.append(m).append('m');
            shown++;
        }
        if (s > 0 && shown < 2) {
            sb.append(s).append('s');
            shown++;
        }
        return sb.length() == 0 ? "0s" : sb.toString();
    }

    /** 在后台线程执行网络请求，避免阻塞主线程（Android 网络规范） */
    private <T> T runIo(Callable<T> task, T fallback) {
        Future<T> future = IO.submit(task);
        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            return fallback;
        }
    }
}