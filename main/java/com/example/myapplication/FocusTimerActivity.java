package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 专注计时页：倒计时、完成/放弃上报后端、专注锁定。
 * launchMode=singleTask，配合 FocusSessionState 恢复剩余时间。
 */
public class FocusTimerActivity extends AppCompatActivity {

    /** Intent 传递：计划总秒数 */
    public static final String EXTRA_DURATION_SECONDS = "duration_seconds";
    /** Intent 传递：剩余秒数（从后台拉回时恢复） */
    public static final String EXTRA_REMAINING_SECONDS = "remaining_seconds";

    private UserManager userManager;
    private CountDownTimer countDownTimer;
    private long durationSeconds;
    private long remainingSeconds;
    private boolean completed;
    private boolean gaveUp;
    private boolean focusLockActive;
    private boolean focusTipDismissed;
    private boolean triedToLeave;
    private LinearLayout focusLockOverlay;
    private TextView tvFocusTipMessage;
    private TextView btnFocusTipGotIt;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable reclaimRunnable;
    private boolean showingSystemDialog;
    private boolean sessionEnded;

    /** 供 FocusLockHelper 判断是否需要继续拉回前台 */
    public boolean isEnforcingFocus() {
        return shouldEnforceFocus();
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(this);
        if (!UiHelper.ensureLoggedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_focus_timer);

        if (savedInstanceState != null) {
            durationSeconds = savedInstanceState.getLong("durationSeconds", 0);
            remainingSeconds = savedInstanceState.getLong("remainingSeconds", durationSeconds);
            completed = savedInstanceState.getBoolean("completed", false);
            gaveUp = savedInstanceState.getBoolean("gaveUp", false);
            sessionEnded = savedInstanceState.getBoolean("sessionEnded", false);
        } else {
            durationSeconds = getIntent().getLongExtra(EXTRA_DURATION_SECONDS, 0);
            long restoredRemaining = getIntent().getLongExtra(EXTRA_REMAINING_SECONDS, -1);
            remainingSeconds = restoredRemaining >= 0 ? restoredRemaining : durationSeconds;
        }
        if (durationSeconds <= 0 || remainingSeconds <= 0) {
            finish();
            return;
        }

        ImageView imgBackground = findViewById(R.id.imgBackground);
        TextView tvCountdown = findViewById(R.id.tvCountdown);
        TextView btnGiveUp = findViewById(R.id.btnGiveUp);

        AppTheme theme = userManager.getCurrentTheme();
        imgBackground.setImageResource(theme.bgDrawable);
        btnGiveUp.setBackground(ThemeHelper.roundedRect(ThemeHelper.withAlpha(0xFFFFFFFF, 0xF2), 24f, this));
        btnGiveUp.setTextColor(theme.primaryDark);

        tvCountdown.setText(formatRemaining(remainingSeconds));
        tvCountdown.setBackground(ThemeHelper.roundedRect(ThemeHelper.withAlpha(theme.primaryDark, 0xB3), 12f, this));

        btnGiveUp.setOnClickListener(v -> showGiveUpDialog());

        focusLockOverlay = findViewById(R.id.focusLockOverlay);
        tvFocusTipMessage = findViewById(R.id.tvFocusTipMessage);
        btnFocusTipGotIt = findViewById(R.id.btnFocusTipGotIt);
        btnFocusTipGotIt.setTextColor(theme.primary);
        btnFocusTipGotIt.setOnClickListener(v -> dismissFocusTip());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showGiveUpDialog();
            }
        });

        if (!completed && !gaveUp && !sessionEnded) {
            startCountdown(tvCountdown);
        } else if (completed) {
            tvCountdown.setText(formatRemaining(0));
        }
    }

    /** 启动倒计时：每秒更新 UI 并写入 FocusSessionState */
    private void startCountdown(TextView tvCountdown) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        long millis = remainingSeconds * 1000L;
        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds = (millisUntilFinished + 999) / 1000;
                tvCountdown.setText(formatRemaining(remainingSeconds));
                FocusSessionState.get(FocusTimerActivity.this).updateRemaining(remainingSeconds);
            }

            @Override
            public void onFinish() {
                completed = true;
                onFocusComplete();
            }
        };
        countDownTimer.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("durationSeconds", durationSeconds);
        outState.putLong("remainingSeconds", remainingSeconds);
        outState.putBoolean("completed", completed);
        outState.putBoolean("gaveUp", gaveUp);
        outState.putBoolean("sessionEnded", sessionEnded);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showingSystemDialog = false;
        stopReclaim();
        if (shouldEnforceFocus()) {
            FocusSessionState.get(this).setActive(durationSeconds, remainingSeconds);
            FocusLockHelper.enable(this);
            focusLockActive = true;
            if (triedToLeave) {
                triedToLeave = false;
                showFocusTip(R.string.focus_lock_return_tip);
            } else if (!focusTipDismissed) {
                showFocusTip(R.string.focus_lock_tip_message);
            }
        }
    }

    @Override
    protected void onPause() {
        if (shouldEnforceFocus() && !showingSystemDialog) {
            startReclaim();
            FocusLockHelper.bringTaskToFront(this);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (shouldEnforceFocus() && !showingSystemDialog) {
            FocusLockHelper.bringTaskToFront(this);
        }
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void startReclaim() {
        stopReclaim();
        reclaimRunnable = FocusLockHelper.scheduleReclaim(this, mainHandler);
    }

    private void stopReclaim() {
        FocusLockHelper.cancelReclaim(mainHandler, reclaimRunnable);
        reclaimRunnable = null;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && shouldEnforceFocus()) {
            FocusLockHelper.reapplyImmersive(this);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (shouldEnforceFocus()) {
            triedToLeave = true;
            FocusLockHelper.bringTaskToFront(this);
        }
    }

    private boolean shouldEnforceFocus() {
        return !completed && !gaveUp && durationSeconds > 0;
    }

    private void releaseFocusLock() {
        stopReclaim();
        if (!focusLockActive) return;
        FocusLockHelper.disable(this);
        focusLockActive = false;
        hideFocusTip();
    }

    private void endFocusSession() {
        sessionEnded = true;
        FocusSessionState.get(this).clear();
        releaseFocusLock();
    }

    private void showFocusTip(int messageResId) {
        if (focusLockOverlay == null) return;
        tvFocusTipMessage.setText(messageResId);
        focusLockOverlay.setVisibility(View.VISIBLE);
        focusLockOverlay.setAlpha(0f);
        focusLockOverlay.animate().alpha(1f).setDuration(220).start();
    }

    private void dismissFocusTip() {
        focusTipDismissed = true;
        hideFocusTip();
    }

    private void hideFocusTip() {
        if (focusLockOverlay == null) return;
        focusLockOverlay.animate().cancel();
        focusLockOverlay.setVisibility(View.GONE);
    }

    /** 放弃：弹窗确认后上报 abandon 接口并结束 Activity */
    private void showGiveUpDialog() {
        showingSystemDialog = true;
        stopReclaim();
        new AlertDialog.Builder(this)
                .setTitle(R.string.give_up_title)
                .setMessage(R.string.give_up_message)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    gaveUp = true;
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    userManager.recordFocusAbandon(durationSeconds, remainingSeconds);
                    endFocusSession();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, (d, w) -> showingSystemDialog = false)
                .setOnDismissListener(d -> showingSystemDialog = false)
                .show();
    }

    /** 倒计时结束：上报 complete 接口，弹窗显示获得积分 */
    private void onFocusComplete() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        userManager.recordFocusComplete(durationSeconds);
        int earned = UserManager.pointsForDuration(durationSeconds);
        endFocusSession();

        showingSystemDialog = true;
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.points_earned, earned))
                .setCancelable(false)
                .setPositiveButton(R.string.confirm, (d, w) -> finish())
                .setOnDismissListener(d -> showingSystemDialog = false)
                .show();
    }

    private String formatRemaining(long seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        return UserManager.formatHms(h, m, s);
    }

    @Override
    protected void onDestroy() {
        stopReclaim();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        releaseFocusLock();
        FocusSessionState.get(this).clear();
        if (!sessionEnded && !completed && !gaveUp && durationSeconds > 0) {
            userManager.recordFocusAbandon(durationSeconds, remainingSeconds);
        }
        super.onDestroy();
    }
}