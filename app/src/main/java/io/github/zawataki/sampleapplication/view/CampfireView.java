package io.github.zawataki.sampleapplication.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Random;

/**
 * A simple custom view that renders an animated campfire.
 */
public class CampfireView extends View {

    private final Paint skyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint groundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint logPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flamePaintOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flamePaintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path reusablePath = new Path();
    private final RectF reusableRect = new RectF();

    private final ValueAnimator animator;
    private final Random random = new Random();

    private float flicker = 0.5f;
    private float emberSeed = 0.5f;

    private Shader skyGradient;

    public CampfireView(Context context) {
        this(context, null);
    }

    public CampfireView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CampfireView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(false);
        setClickable(false);
        initPaints();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500L);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(valueAnimator -> {
            flicker = (float) valueAnimator.getAnimatedValue();
            emberSeed = random.nextFloat();
            requestNextFrame();
        });
    }

    private void initPaints() {
        groundPaint.setColor(Color.parseColor("#2d2419"));
        groundPaint.setStyle(Paint.Style.FILL);

        logPaint.setColor(Color.parseColor("#5b3e25"));
        logPaint.setStyle(Paint.Style.FILL);

        flamePaintOuter.setStyle(Paint.Style.FILL);
        flamePaintInner.setStyle(Paint.Style.FILL);

        emberPaint.setStyle(Paint.Style.FILL);
        emberPaint.setColor(Color.argb(120, 255, 209, 150));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!animator.isStarted()) {
            animator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animator.cancel();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        skyGradient = new LinearGradient(
                0f,
                0f,
                0f,
                h,
                new int[]{Color.parseColor("#020111"), Color.parseColor("#3b1d38"), Color.parseColor("#5b311a")},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP
        );
        skyPaint.setShader(skyGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        drawBackground(canvas, width, height);
        drawGround(canvas, width, height);
        drawLogs(canvas, width, height);
        drawFlame(canvas, width, height);
        drawEmbers(canvas, width, height);
    }

    private void drawBackground(Canvas canvas, int width, int height) {
        if (skyGradient == null) {
            canvas.drawColor(Color.BLACK);
        } else {
            canvas.drawRect(0f, 0f, width, height, skyPaint);
        }
    }

    private void drawGround(Canvas canvas, int width, int height) {
        float groundHeight = height * 0.15f;
        reusableRect.set(-width * 0.1f, height - groundHeight * 1.4f, width * 1.1f, height + groundHeight);
        canvas.drawOval(reusableRect, groundPaint);
    }

    private void drawLogs(Canvas canvas, int width, int height) {
        float centerX = width / 2f;
        float baseY = height * 0.68f;
        float logWidth = width * 0.45f;
        float logHeight = height * 0.05f;
        float corner = logHeight / 2f;

        reusableRect.set(centerX - logWidth / 2f, baseY - logHeight / 2f, centerX + logWidth / 2f, baseY + logHeight / 2f);
        canvas.save();
        canvas.rotate(-18f, centerX, baseY);
        canvas.drawRoundRect(reusableRect, corner, corner, logPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(18f, centerX, baseY);
        canvas.drawRoundRect(reusableRect, corner, corner, logPaint);
        canvas.restore();
    }

    private void drawFlame(Canvas canvas, int width, int height) {
        float centerX = width / 2f;
        float baseY = height * 0.63f;
        float outerHeight = height * (0.28f + 0.05f * (flicker - 0.5f));
        outerHeight = Math.max(outerHeight, height * 0.24f);
        float outerWidth = width * 0.20f + width * 0.02f * flicker;

        int outerR = (int) (220 + 25 * flicker);
        int outerG = (int) (120 + 35 * flicker);
        flamePaintOuter.setColor(Color.rgb(outerR, outerG, 40));

        reusablePath.reset();
        reusablePath.moveTo(centerX, baseY - outerHeight);
        reusablePath.cubicTo(centerX + outerWidth, baseY - outerHeight * 0.55f, centerX + outerWidth * 0.8f, baseY - outerHeight * 0.1f, centerX, baseY);
        reusablePath.cubicTo(centerX - outerWidth * 0.8f, baseY - outerHeight * 0.1f, centerX - outerWidth, baseY - outerHeight * 0.55f, centerX, baseY - outerHeight);
        reusablePath.close();
        canvas.drawPath(reusablePath, flamePaintOuter);

        float innerHeight = outerHeight * 0.7f;
        float innerWidth = outerWidth * 0.55f;
        int innerR = 255;
        int innerG = (int) (170 + 20 * flicker);
        flamePaintInner.setColor(Color.rgb(innerR, innerG, 60));

        reusablePath.reset();
        reusablePath.moveTo(centerX, baseY - innerHeight);
        reusablePath.cubicTo(centerX + innerWidth, baseY - innerHeight * 0.55f, centerX + innerWidth * 0.75f, baseY - innerHeight * 0.05f, centerX, baseY - innerHeight * 0.1f);
        reusablePath.cubicTo(centerX - innerWidth * 0.75f, baseY - innerHeight * 0.05f, centerX - innerWidth, baseY - innerHeight * 0.55f, centerX, baseY - innerHeight);
        reusablePath.close();
        canvas.drawPath(reusablePath, flamePaintInner);
    }

    private void drawEmbers(Canvas canvas, int width, int height) {
        float baseY = height * 0.63f;
        int emberCount = 18;
        for (int i = 0; i < emberCount; i++) {
            float normalized = ((i + emberSeed) % emberCount) / emberCount;
            float alpha = (1f - normalized);
            float size = 4f + 6f * alpha;
            float x = (float) (width / 2f + (random.nextFloat() - 0.5f) * width * 0.25f);
            float y = baseY - normalized * height * 0.35f;
            emberPaint.setAlpha((int) (alpha * 160));
            canvas.drawCircle(x, y, size, emberPaint);
        }
    }

    private void requestNextFrame() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            invalidate();
        }
    }
}
