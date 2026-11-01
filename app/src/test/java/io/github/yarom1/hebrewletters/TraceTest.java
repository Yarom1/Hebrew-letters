package io.github.yarom1.hebrewletters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Region;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Tracing needs real pixels, so these tests use Robolectric's native graphics. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
public class TraceTest {

    @After
    public void tearDown() {
        Speech.resetForTests();
    }

    private static TraceActivity open(int letter) {
        return Robolectric.buildActivity(TraceActivity.class,
                TraceActivity.intent(RuntimeEnvironment.getApplication(), letter)).setup().get();
    }

    @Test
    public void tracingAnyLetterAlongItsEdgeEarnsAStar() {
        TraceActivity activity = open(0);
        TraceView trace = activity.findViewById(R.id.trace);
        View success = activity.findViewById(R.id.success);
        for (int i = 0; i < Alphabet.count(); i++) {
            assertEquals(i, activity.getLetterIndex());
            assertFalse(trace.isCompleted());
            assertEquals(View.GONE, success.getVisibility());

            TestUtil.traceOutline(trace);

            String detail = Alphabet.get(i).glyph + ": coverage=" + trace.getCoverage()
                    + " excess=" + trace.getExcess();
            System.out.println("outline " + detail);
            assertTrue(detail, trace.isCompleted());
            assertTrue(Prefs.isTraced(activity, i));
            assertEquals(View.VISIBLE, success.getVisibility());
            activity.findViewById(R.id.btn_next_letter).performClick();
        }
        assertEquals(22, Prefs.tracedCount(activity));
        assertEquals(0, activity.getLetterIndex()); // after ת comes א again
    }

    @Test
    public void colouringInsideTheLetterEarnsAStar() {
        TraceActivity activity = open(0);
        TraceView trace = activity.findViewById(R.id.trace);
        for (int i : new int[] {0, 4, 9, 11, 18, 20}) { // א ה י ל ק ש
            activity.findViewById(R.id.btn_clear).performClick();
            trace.setLetter(Alphabet.get(i).glyph);
            fillInside(trace, trace.getBrushWidth() * 0.5f);
            String detail = Alphabet.get(i).glyph + ": coverage=" + trace.getCoverage()
                    + " excess=" + trace.getExcess();
            System.out.println("fill " + detail);
            assertTrue(detail, trace.isCompleted());
        }
    }

    @Test
    public void aWobblyButHonestTraceEarnsAStar() {
        TraceActivity activity = open(0);
        TraceView trace = activity.findViewById(R.id.trace);
        Random random = new Random(7);
        for (int i = 0; i < Alphabet.count(); i++) {
            trace.setLetter(Alphabet.get(i).glyph);
            // Follow the edge with a shaky finger: up to a third of the brush off, plus gaps.
            float wobble = trace.getBrushWidth() * 0.33f;
            PathMeasure measure = new PathMeasure(trace.getGlyphPath(), false);
            float[] pos = new float[2];
            float[] tan = new float[2];
            do {
                List<PointF> points = new ArrayList<>();
                float length = measure.getLength();
                for (float d = 0; d <= length; d += 5) {
                    measure.getPosTan(d, pos, tan);
                    float off = (random.nextFloat() * 2 - 1) * wobble;
                    points.add(new PointF(pos[0] - tan[1] * off, pos[1] + tan[0] * off));
                    if (random.nextFloat() < 0.01f) { // lift the finger now and then
                        TestUtil.drag(trace, points);
                        points = new ArrayList<>();
                        d += 12;
                    }
                }
                TestUtil.drag(trace, points);
            } while (measure.nextContour());
            String detail = Alphabet.get(i).glyph + ": coverage=" + trace.getCoverage()
                    + " excess=" + trace.getExcess();
            System.out.println("wobbly " + detail);
            assertTrue(detail, trace.isCompleted());
        }
    }

    @Test
    public void theLetterPlusASmallDoodleStillCounts() {
        TraceActivity activity = open(4); // ה
        TraceView trace = activity.findViewById(R.id.trace);
        // A little zigzag in the corner first...
        TestUtil.line(trace, 20, 20, 80, 60);
        TestUtil.line(trace, 80, 60, 30, 100);
        assertFalse(trace.isCompleted());
        // ...then the letter.
        TestUtil.traceOutline(trace);
        System.out.println("doodle: coverage=" + trace.getCoverage() + " excess=" + trace.getExcess());
        assertTrue(trace.isCompleted());
    }

    @Test
    public void scribblingOverTheWholeCardDoesNotCount() {
        TraceActivity activity = open(0);
        TraceView trace = activity.findViewById(R.id.trace);
        for (int i : new int[] {0, 5, 12, 20}) { // א ו (thin) מ ש (wide)
            trace.setLetter(Alphabet.get(i).glyph);
            float step = trace.getBrushWidth() * 0.5f;
            // Top to bottom, then bottom to top: the star must never appear on the way.
            for (float y = step / 2; y < trace.getHeight(); y += step) {
                TestUtil.line(trace, 2, y, trace.getWidth() - 2, y);
                assertFalse("scribble on " + Alphabet.get(i).glyph + " counted at y=" + y
                        + " excess=" + trace.getExcess(), trace.isCompleted());
            }
            System.out.println("scribble " + Alphabet.get(i).glyph + ": coverage="
                    + trace.getCoverage() + " excess=" + trace.getExcess());
            assertTrue(trace.getCoverage() > 0.95f);
        }
        trace.setLetter(Alphabet.get(12).glyph);
        float step = trace.getBrushWidth() * 0.5f;
        for (float y = trace.getHeight() - step / 2; y > 0; y -= step) {
            TestUtil.line(trace, trace.getWidth() - 2, y, 2, y);
            assertFalse(trace.isCompleted());
        }
        assertEquals(View.GONE, activity.findViewById(R.id.success).getVisibility());
    }

    @Test
    public void aFewStrokesAreNotEnough() {
        TraceActivity activity = open(11); // ל
        TraceView trace = activity.findViewById(R.id.trace);
        float cx = trace.getWidth() / 2f;
        float cy = trace.getHeight() / 2f;
        TestUtil.line(trace, cx, cy, cx + 1, cy + 1);
        TestUtil.line(trace, cx - 40, cy, cx + 40, cy);
        assertFalse(trace.isCompleted());
        assertFalse(Prefs.isTraced(activity, 11));
    }

    @Test
    public void clearStartsOver() {
        TraceActivity activity = open(1);
        TraceView trace = activity.findViewById(R.id.trace);
        TestUtil.traceOutline(trace);
        assertTrue(trace.isCompleted());
        activity.findViewById(R.id.btn_again).performClick();
        assertFalse(trace.isCompleted());
        assertEquals(0f, trace.getCoverage(), 0f);
        assertEquals(View.GONE, activity.findViewById(R.id.success).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.trace_hint).getVisibility());
    }

    /** Colours the letter in with horizontal strokes, staying inside its outline. */
    static void fillInside(TraceView trace, float step) {
        Region letter = new Region();
        letter.setPath(trace.getGlyphPath(), new Region(0, 0, trace.getWidth(), trace.getHeight()));
        for (float y = step / 2; y < trace.getHeight(); y += step) {
            List<PointF> run = new ArrayList<>();
            for (int x = 0; x < trace.getWidth(); x += 3) {
                if (letter.contains(x, (int) y)) {
                    run.add(new PointF(x, y));
                } else if (!run.isEmpty()) {
                    TestUtil.drag(trace, run);
                    run = new ArrayList<>();
                }
            }
            TestUtil.drag(trace, run);
        }
    }
}
