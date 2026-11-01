package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/** Small drawing and animation helpers shared by the screens. */
final class Ui {

    /** Bright colours for the letters; letter i always gets the same colour on every screen. */
    private static final int[] LETTER_COLORS = {
        0xFFF03E3E, // red
        0xFFF76707, // orange
        0xFFE8A200, // amber
        0xFF2F9E44, // green
        0xFF1098AD, // teal
        0xFF1C7ED6, // blue
        0xFF7048E8, // violet
        0xFFD6336C, // pink
    };

    private Ui() {}

    static int letterColor(int index) {
        return LETTER_COLORS[Alphabet.wrap(index) % LETTER_COLORS.length];
    }

    static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    static GradientDrawable gradient(int startColor, int endColor, float radiusPx) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[] {startColor, endColor});
        d.setCornerRadius(radiusPx);
        return d;
    }

    static GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    /** Wraps a background so it shows the standard touch ripple, clipped to its shape. */
    static Drawable touchable(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), content, null);
    }

    /** A playful "boing": the view shrinks a little and springs back. */
    static void bounce(View view) {
        view.animate().cancel();
        view.setScaleX(0.82f);
        view.setScaleY(0.82f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(380)
                .setInterpolator(new OvershootInterpolator(3f))
                .start();
    }

    /** The view grows in from nothing. */
    static void popIn(View view, long delayMs) {
        view.animate().cancel();
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delayMs)
                .setDuration(420)
                .setInterpolator(new OvershootInterpolator(2.2f))
                .start();
    }
}
