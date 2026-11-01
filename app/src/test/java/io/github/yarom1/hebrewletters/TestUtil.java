package io.github.yarom1.hebrewletters;

import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.robolectric.shadows.ShadowTextToSpeech;

final class TestUtil {

    private TestUtil() {}

    /** Pretends the phone's text-to-speech engine finished starting and speaks Hebrew. */
    static void hebrewVoiceReady() {
        ShadowTextToSpeech.addLanguageAvailability(new Locale("he", "IL"));
        engineStarted();
    }

    /** Pretends the engine finished starting (without adding any language). */
    static void engineStarted() {
        TextToSpeech tts = ShadowTextToSpeech.getLastTextToSpeechInstance();
        assertNotNull("the app should have created a TextToSpeech engine", tts);
        shadowOf(tts).getOnInitListener().onInit(TextToSpeech.SUCCESS);
        idle();
    }

    static String lastSpoken() {
        TextToSpeech tts = ShadowTextToSpeech.getLastTextToSpeechInstance();
        return tts == null ? null : shadowOf(tts).getLastSpokenText();
    }

    static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** Lets animations finish. */
    static void idleFor(long millis) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis));
    }

    /** Drags one finger through {@code points} (view coordinates). */
    static void drag(View view, List<PointF> points) {
        if (points.isEmpty()) {
            return;
        }
        long down = SystemClock.uptimeMillis();
        long t = down;
        PointF first = points.get(0);
        send(view, down, t, MotionEvent.ACTION_DOWN, first.x, first.y);
        for (int i = 1; i < points.size(); i++) {
            t += 8;
            send(view, down, t, MotionEvent.ACTION_MOVE, points.get(i).x, points.get(i).y);
        }
        PointF last = points.get(points.size() - 1);
        send(view, down, t + 8, MotionEvent.ACTION_UP, last.x, last.y);
    }

    static void line(View view, float x1, float y1, float x2, float y2) {
        List<PointF> points = new ArrayList<>();
        int steps = Math.max(2, (int) (Math.hypot(x2 - x1, y2 - y1) / 6));
        for (int i = 0; i <= steps; i++) {
            float f = i / (float) steps;
            points.add(new PointF(x1 + (x2 - x1) * f, y1 + (y2 - y1) * f));
        }
        drag(view, points);
    }

    /** Traces every contour of the letter's outline, like a careful child following the edge. */
    static void traceOutline(TraceView trace) {
        PathMeasure measure = new PathMeasure(trace.getGlyphPath(), false);
        float[] pos = new float[2];
        do {
            float length = measure.getLength();
            if (length < 1) {
                continue;
            }
            List<PointF> points = new ArrayList<>();
            for (float d = 0; d <= length; d += 5) {
                measure.getPosTan(d, pos, null);
                points.add(new PointF(pos[0], pos[1]));
            }
            drag(trace, points);
        } while (measure.nextContour());
    }

    private static void send(View view, long down, long time, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(down, time, action, x, y, 0);
        view.dispatchTouchEvent(event);
        event.recycle();
    }
}
