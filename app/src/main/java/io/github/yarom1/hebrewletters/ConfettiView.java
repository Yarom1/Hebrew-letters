package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import java.util.Random;

/** A short burst of falling confetti, shown when a letter has been written. */
public class ConfettiView extends View {

    private static final int[] COLORS = {
        0xFFF03E3E, 0xFFF76707, 0xFFFAB005, 0xFF2F9E44, 0xFF1C7ED6, 0xFF7048E8, 0xFFD6336C
    };
    private static final int PIECES = 110;
    private static final long DURATION_MS = 2600;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final float density;

    private final float[] x = new float[PIECES];
    private final float[] y = new float[PIECES];
    private final float[] vx = new float[PIECES];
    private final float[] vy = new float[PIECES];
    private final float[] angle = new float[PIECES];
    private final float[] spin = new float[PIECES];
    private final float[] size = new float[PIECES];
    private final int[] color = new int[PIECES];

    private long startTime = -1;
    private long lastFrame;

    public ConfettiView(Context context) {
        this(context, null);
    }

    public ConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void burst() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        for (int i = 0; i < PIECES; i++) {
            x[i] = w * (0.15f + 0.7f * random.nextFloat());
            y[i] = h * (0.35f + 0.2f * random.nextFloat());
            double direction = Math.toRadians(-160 + 140 * random.nextDouble()); // mostly upwards
            float speed = (450 + 900 * random.nextFloat()) * density;
            vx[i] = (float) Math.cos(direction) * speed;
            vy[i] = (float) Math.sin(direction) * speed;
            angle[i] = 360 * random.nextFloat();
            spin[i] = (random.nextFloat() - 0.5f) * 1080;
            size[i] = (7 + 7 * random.nextFloat()) * density;
            color[i] = COLORS[random.nextInt(COLORS.length)];
        }
        startTime = AnimationUtils.currentAnimationTimeMillis();
        lastFrame = startTime;
        postInvalidateOnAnimation();
    }

    public boolean isRunning() {
        return startTime >= 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (startTime < 0) {
            return;
        }
        long now = AnimationUtils.currentAnimationTimeMillis();
        float dt = Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float progress = (now - startTime) / (float) DURATION_MS;
        if (progress >= 1f) {
            startTime = -1;
            return;
        }
        int alpha = progress < 0.7f ? 255 : (int) (255 * (1f - (progress - 0.7f) / 0.3f));
        float gravity = 1400 * density;
        for (int i = 0; i < PIECES; i++) {
            vy[i] += gravity * dt;
            vx[i] *= 0.985f;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            angle[i] += spin[i] * dt;
            paint.setColor(color[i]);
            paint.setAlpha(alpha);
            canvas.save();
            canvas.rotate(angle[i], x[i], y[i]);
            float half = size[i] / 2f;
            canvas.drawRect(x[i] - half, y[i] - half / 2f, x[i] + half, y[i] + half / 2f, paint);
            canvas.restore();
        }
        postInvalidateOnAnimation();
    }
}
