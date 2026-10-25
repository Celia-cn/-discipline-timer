package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 专注会话本地状态（SharedPreferences）：记录是否进行中及剩余秒数，
 * 进程被杀后仍可从 Intent 恢复 FocusTimerActivity。
 */
public final class FocusSessionState {

    private static final String PREFS = "focus_session_state";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_REMAINING = "remaining";

    private final SharedPreferences prefs;

    private FocusSessionState(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static FocusSessionState get(Context context) {
        return new FocusSessionState(context);
    }

    public void setActive(long durationSeconds, long remainingSeconds) {
        prefs.edit()
                .putBoolean(KEY_ACTIVE, true)
                .putLong(KEY_DURATION, durationSeconds)
                .putLong(KEY_REMAINING, remainingSeconds)
                .apply();
    }

    public void updateRemaining(long remainingSeconds) {
        if (!isActive()) return;
        prefs.edit().putLong(KEY_REMAINING, remainingSeconds).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public boolean isActive() {
        return prefs.getBoolean(KEY_ACTIVE, false);
    }

    public long getDurationSeconds() {
        return prefs.getLong(KEY_DURATION, 0);
    }

    public long getRemainingSeconds() {
        return prefs.getLong(KEY_REMAINING, 0);
    }
}