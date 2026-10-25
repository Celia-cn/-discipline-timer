package com.example.myapplication;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * 专注防干扰：startLockTask 固定任务、隐藏系统栏、周期性 moveTaskToFront。
 * 实现「仅能通过放弃按钮结束」的产品需求。
 */
public final class FocusLockHelper {

    private static final long RECLAIM_INTERVAL_MS = 120L;

    private FocusLockHelper() {
    }

    public static void enable(Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applyImmersive(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(false);
        }
        startLockTask(activity);
    }

    public static void disable(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.setRecentsScreenshotEnabled(true);
        }
        stopLockTask(activity);
        clearImmersive(activity);
    }

    public static void reapplyImmersive(Activity activity) {
        applyImmersive(activity);
    }

    public static void bringTaskToFront(Activity activity) {
        ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            try {
                am.moveTaskToFront(activity.getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
            } catch (SecurityException ignored) {
            }
        }
        if (activity instanceof FocusTimerActivity) {
            FocusTimerActivity focusActivity = (FocusTimerActivity) activity;
            Intent intent = new Intent(activity, FocusTimerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(FocusTimerActivity.EXTRA_DURATION_SECONDS, focusActivity.getDurationSeconds());
            intent.putExtra(FocusTimerActivity.EXTRA_REMAINING_SECONDS, focusActivity.getRemainingSeconds());
            activity.startActivity(intent);
        }
    }

    /** 切走时周期性拉回前台，弥补部分机型锁定任务未生效的情况 */
    public static Runnable scheduleReclaim(Activity activity, Handler handler) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                if (!(activity instanceof FocusTimerActivity)) {
                    return;
                }
                if (!((FocusTimerActivity) activity).isEnforcingFocus()) {
                    return;
                }
                bringTaskToFront(activity);
                handler.postDelayed(this, RECLAIM_INTERVAL_MS);
            }
        };
        handler.post(runnable);
        return runnable;
    }

    public static void cancelReclaim(Handler handler, Runnable runnable) {
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    private static void startLockTask(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                activity.startLockTask();
            } catch (SecurityException ignored) {
            }
        }
    }

    private static void stopLockTask(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                activity.stopLockTask();
            } catch (SecurityException ignored) {
            }
        }
    }

    private static void applyImmersive(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private static void clearImmersive(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), true);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                activity.getWindow(), activity.getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
    }
}