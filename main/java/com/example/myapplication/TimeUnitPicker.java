package com.example.myapplication;

import android.view.View;
import android.widget.TextView;

/** 时/分/秒滚轮式加减控件，用于自定义专注时长 */
public class TimeUnitPicker {

    private final TextView tvValue;
    private final TextView btnUp;
    private final TextView btnDown;
    private int value;
    private final int min;
    private final int max;

    public TimeUnitPicker(View root, int initial, int min, int max) {
        this.min = min;
        this.max = max;
        this.value = clamp(initial);
        tvValue = root.findViewById(R.id.tvValue);
        btnUp = root.findViewById(R.id.btnUp);
        btnDown = root.findViewById(R.id.btnDown);
        updateDisplay();
        btnUp.setOnClickListener(v -> increment());
        btnDown.setOnClickListener(v -> decrement());
    }

    public int getValue() {
        return value;
    }

    public void setValue(int v) {
        value = clamp(v);
        updateDisplay();
    }

    private void increment() {
        value = value >= max ? min : value + 1;
        updateDisplay();
    }

    private void decrement() {
        value = value <= min ? max : value - 1;
        updateDisplay();
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }

    private void updateDisplay() {
        tvValue.setText(String.format("%02d", value));
    }
}