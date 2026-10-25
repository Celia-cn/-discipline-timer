package com.example.myapplication;

import android.graphics.Outline;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** 主题商店：展示各主题，积分解锁或切换当前主题 */
public class ThemeStoreActivity extends AppCompatActivity {

    private UserManager userManager;
    private LinearLayout layoutThemes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userManager = new UserManager(this);
        if (!UiHelper.ensureLoggedIn(this)) {
            return;
        }
        setContentView(R.layout.activity_theme_store);
        setTitle(R.string.theme_store);
        UiHelper.setupBackButton(this);

        layoutThemes = findViewById(R.id.layoutThemes);
        refreshPage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPage();
    }

    private void refreshPage() {
        userManager.refreshProfile();
        AppTheme theme = userManager.getCurrentTheme();
        ImageView bg = findViewById(R.id.imgBackground);
        bg.setImageResource(theme.bgDrawable);
        TextView tvPoints = findViewById(R.id.tvPointsBalance);
        tvPoints.setText(getString(R.string.current_points, userManager.getPoints()));
        UiHelper.refreshBackButtonTheme(this);
        ThemeHelper.applyThemeStoreHeader(this, theme);
        buildThemeList();
    }

    private void buildThemeList() {
        layoutThemes.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        String selected = userManager.getSelectedTheme();

        for (AppTheme theme : AppTheme.ALL) {
            View item = inflater.inflate(R.layout.item_theme, layoutThemes, false);
            ImageView preview = item.findViewById(R.id.imgPreview);
            TextView name = item.findViewById(R.id.tvThemeName);
            TextView status = item.findViewById(R.id.tvThemeStatus);
            TextView action = item.findViewById(R.id.btnThemeAction);

            preview.setImageResource(theme.bgDrawable);
            preview.setClipToOutline(true);
            preview.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                            12 * getResources().getDisplayMetrics().density);
                }
            });

            name.setText(theme.name);
            boolean unlocked = userManager.isThemeUnlocked(theme.id);
            boolean isSelected = theme.id.equals(selected);

            if (isSelected) {
                status.setText(R.string.theme_in_use);
                action.setText(R.string.theme_using);
                action.setBackground(ThemeHelper.roundedRect(theme.btn, 8f, this));
            } else if (unlocked) {
                status.setText(R.string.theme_unlocked);
                action.setText(R.string.theme_apply);
                action.setBackground(ThemeHelper.roundedRect(theme.btn, 8f, this));
            } else if (theme.unlockCost == 0) {
                status.setText(R.string.theme_free);
                action.setText(R.string.theme_apply);
                action.setBackground(ThemeHelper.roundedRect(theme.btn, 8f, this));
            } else {
                status.setText(getString(R.string.theme_cost, theme.unlockCost));
                action.setText(R.string.theme_buy);
                action.setBackground(ThemeHelper.roundedRect(0xFF9E9E9E, 8f, this));
            }

            action.setOnClickListener(v -> onThemeAction(theme, unlocked, isSelected));
            layoutThemes.addView(item);
        }
    }

    private void onThemeAction(AppTheme theme, boolean unlocked, boolean isSelected) {
        if (isSelected) return;

        if (unlocked || theme.unlockCost == 0) {
            userManager.setSelectedTheme(theme.id);
            Toast.makeText(this, R.string.theme_applied, Toast.LENGTH_SHORT).show();
            refreshPage();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.theme_buy_title)
                .setMessage(getString(R.string.theme_buy_message, theme.name, theme.unlockCost))
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    if (userManager.unlockTheme(theme.id)) {
                        userManager.setSelectedTheme(theme.id);
                        Toast.makeText(this, R.string.theme_applied, Toast.LENGTH_SHORT).show();
                        refreshPage();
                    } else {
                        Toast.makeText(this, R.string.unlock_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}