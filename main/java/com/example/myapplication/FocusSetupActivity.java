package com.example.myapplication;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/**
 * 专注前设置：GridLayout 选择时长（10/30/60/120 分钟或自定义），
 * 跳转 FocusTimerActivity 并传入 EXTRA_DURATION_SECONDS。
 */
public class FocusSetupActivity extends AppCompatActivity {

    private static final long SEC_10_MIN = 10 * 60L;
    private static final long SEC_30_MIN = 30 * 60L;
    private static final long SEC_1_HOUR = 60 * 60L;
    private static final long SEC_2_HOUR = 2 * 60 * 60L;

    private UserManager userManager;
    private TimeUnitPicker pickerHours;
    private TimeUnitPicker pickerMinutes;
    private TimeUnitPicker pickerSeconds;

    private long selectedSeconds = SEC_30_MIN;
    private boolean customMode;

    private TextView btnPreset10;
    private TextView btnPreset30;
    private TextView btnPreset1h;
    private TextView btnPreset2h;
    private TextView btnPresetCustom;
    private LinearLayout panelCustom;
    private TextView btnStart;
    private TextView selectedPresetBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(this);
        if (!UiHelper.ensureLoggedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_focus_setup);
        UiHelper.setupBackButton(this);
        UiHelper.applyContentBelowBackButton(this, findViewById(R.id.focusSetupContent));

        ImageView imgBackground = findViewById(R.id.imgBackground);
        btnPreset10 = findViewById(R.id.btnPreset10);
        btnPreset30 = findViewById(R.id.btnPreset30);
        btnPreset1h = findViewById(R.id.btnPreset1h);
        btnPreset2h = findViewById(R.id.btnPreset2h);
        btnPresetCustom = findViewById(R.id.btnPresetCustom);
        panelCustom = findViewById(R.id.panelCustom);
        btnStart = findViewById(R.id.btnStart);

        View hoursRoot = findViewById(R.id.pickerHours);
        View minutesRoot = findViewById(R.id.pickerMinutes);
        View secondsRoot = findViewById(R.id.pickerSeconds);
        pickerHours = new TimeUnitPicker(hoursRoot, 0, 0, 23);
        pickerMinutes = new TimeUnitPicker(minutesRoot, 30, 0, 59);
        pickerSeconds = new TimeUnitPicker(secondsRoot, 0, 0, 59);

        btnPreset10.setOnClickListener(v -> selectPreset(SEC_10_MIN, btnPreset10));
        btnPreset30.setOnClickListener(v -> selectPreset(SEC_30_MIN, btnPreset30));
        btnPreset1h.setOnClickListener(v -> selectPreset(SEC_1_HOUR, btnPreset1h));
        btnPreset2h.setOnClickListener(v -> selectPreset(SEC_2_HOUR, btnPreset2h));
        btnPresetCustom.setOnClickListener(v -> selectCustomMode());

        btnStart.setOnClickListener(v -> startFocus());

        selectPreset(SEC_30_MIN, btnPreset30);
        applyTheme();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiHelper.refreshBackButtonTheme(this);
        applyTheme();
    }

    private void selectPreset(long seconds, TextView selectedBtn) {
        customMode = false;
        selectedSeconds = seconds;
        panelCustom.setVisibility(View.GONE);
        selectedPresetBtn = selectedBtn;
        highlightPreset(selectedBtn);
    }

    private void selectCustomMode() {
        customMode = true;
        panelCustom.setVisibility(View.VISIBLE);
        selectedPresetBtn = btnPresetCustom;
        highlightPreset(btnPresetCustom);
        selectedSeconds = getCustomSeconds();
    }

    private long getCustomSeconds() {
        return pickerHours.getValue() * 3600L
                + pickerMinutes.getValue() * 60L
                + pickerSeconds.getValue();
    }

    private void highlightPreset(TextView selected) {
        AppTheme theme = userManager.getCurrentTheme();
        TextView[] all = {btnPreset10, btnPreset30, btnPreset1h, btnPreset2h, btnPresetCustom};
        for (TextView btn : all) {
            if (btn == selected) {
                btn.setBackground(selectedChipBackground(theme));
                btn.setTextColor(theme.primaryDark);
            } else {
                btn.setBackgroundResource(R.drawable.bg_preset_chip);
                btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            }
        }
    }

    private GradientDrawable selectedChipBackground(AppTheme theme) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(12f * getResources().getDisplayMetrics().density);
        gd.setColor(ThemeHelper.withAlpha(theme.primary, 0x33));
        gd.setStroke((int) (2 * getResources().getDisplayMetrics().density), theme.primary);
        return gd;
    }

    private void startFocus() {
        long seconds = customMode ? getCustomSeconds() : selectedSeconds;
        if (seconds <= 0) {
            Toast.makeText(this, R.string.duration_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, FocusTimerActivity.class);
        intent.putExtra(FocusTimerActivity.EXTRA_DURATION_SECONDS, seconds);
        startActivity(intent);
    }

    private void applyTheme() {
        AppTheme theme = userManager.getCurrentTheme();
        ImageView bg = findViewById(R.id.imgBackground);
        bg.setImageResource(theme.bgDrawable);
        btnStart.setBackground(ThemeHelper.roundedGradient(theme, 24f, this));
        if (selectedPresetBtn != null) {
            highlightPreset(selectedPresetBtn);
        }
    }
}