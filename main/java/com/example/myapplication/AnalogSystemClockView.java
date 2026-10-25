package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;

/** 自定义模拟表盘：onDraw 绘制刻度与时分秒针，Handler 每秒刷新 */
public class AnalogSystemClockView extends View {

    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint minutePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF faceRect = new RectF();
    private int colorPrimary = 0xFF43A047;
    private int colorDark = 0xFF2E7D32;
    private int colorLight = 0xFF388E3C;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
            handler.postDelayed(this, 1000);
        }
    };

    public AnalogSystemClockView(Context context) {
        super(context);
        init();
    }

    public AnalogSystemClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnalogSystemClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        facePaint.setStyle(Paint.Style.FILL);
        facePaint.setColor(0xFFFFF9F3);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(3));
        ringPaint.setColor(0xFF43A047);

        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);
        tickPaint.setColor(0xFF2E7D32);

        hourPaint.setStyle(Paint.Style.STROKE);
        hourPaint.setStrokeCap(Paint.Cap.ROUND);
        hourPaint.setStrokeWidth(dp(5));
        hourPaint.setColor(0xFF1B5E20);

        minutePaint.setStyle(Paint.Style.STROKE);
        minutePaint.setStrokeCap(Paint.Cap.ROUND);
        minutePaint.setStrokeWidth(dp(3.5f));
        minutePaint.setColor(0xFF388E3C);

        secondPaint.setStyle(Paint.Style.STROKE);
        secondPaint.setStrokeCap(Paint.Cap.ROUND);
        secondPaint.setStrokeWidth(dp(2));
        secondPaint.setColor(0xFFE53935);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(0xFF2E7D32);

        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setColor(0xFF1B5E20);
        numberPaint.setFakeBoldText(true);
        applyThemeColors();
    }

    public void setThemeColors(int primary, int dark, int light) {
        colorPrimary = primary;
        colorDark = dark;
        colorLight = light;
        applyThemeColors();
        invalidate();
    }

    private void applyThemeColors() {
        ringPaint.setColor(colorPrimary);
        tickPaint.setColor(colorDark);
        hourPaint.setColor(colorDark);
        minutePaint.setColor(colorLight);
        centerPaint.setColor(colorDark);
        numberPaint.setColor(colorDark);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.post(tickRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tickRunnable);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - dp(8);

        faceRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawOval(faceRect, facePaint);
        canvas.drawOval(faceRect, ringPaint);

        drawTicks(canvas, cx, cy, radius);
        drawHourNumbers(canvas, cx, cy, radius);

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY) % 12;
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        float hourAngle = (hour + minute / 60f) * 30f - 90f;
        float minuteAngle = minute * 6f - 90f;
        float secondAngle = second * 6f - 90f;

        drawHand(canvas, cx, cy, radius * 0.42f, hourAngle, hourPaint);
        drawHand(canvas, cx, cy, radius * 0.58f, minuteAngle, minutePaint);
        drawHand(canvas, cx, cy, radius * 0.72f, secondAngle, secondPaint);

        canvas.drawCircle(cx, cy, dp(5), centerPaint);
    }

    private void drawTicks(Canvas canvas, float cx, float cy, float radius) {
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            float inner = radius * 0.80f;
            float outer = radius * 0.92f;
            tickPaint.setStrokeWidth(dp(2));
            float x1 = cx + (float) (inner * Math.cos(angle));
            float y1 = cy + (float) (inner * Math.sin(angle));
            float x2 = cx + (float) (outer * Math.cos(angle));
            float y2 = cy + (float) (outer * Math.sin(angle));
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }
    }

    private void drawHourNumbers(Canvas canvas, float cx, float cy, float radius) {
        numberPaint.setTextSize(sp(15));
        Paint.FontMetrics fm = numberPaint.getFontMetrics();
        float textOffset = (fm.ascent + fm.descent) / 2f;
        float numberRadius = radius * 0.66f;

        for (int i = 1; i <= 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            float x = cx + (float) (numberRadius * Math.cos(angle));
            float y = cy + (float) (numberRadius * Math.sin(angle)) - textOffset;
            canvas.drawText(String.valueOf(i), x, y, numberPaint);
        }
    }

    private void drawHand(Canvas canvas, float cx, float cy, float length, float angleDeg, Paint paint) {
        double angle = Math.toRadians(angleDeg);
        float x = cx + (float) (length * Math.cos(angle));
        float y = cy + (float) (length * Math.sin(angle));
        canvas.drawLine(cx, cy, x, y, paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}