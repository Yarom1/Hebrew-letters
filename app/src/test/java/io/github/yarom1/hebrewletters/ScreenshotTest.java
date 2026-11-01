package io.github.yarom1.hebrewletters;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.widget.GridView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;

/**
 * Saves a picture of every screen, for a visual check. Runs only when the system property
 * {@code screenshots.dir} is set, e.g. {@code -Dscreenshots.dir=build/screenshots}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
public class ScreenshotTest {

    private File dir;

    @Before
    public void setUp() {
        String path = System.getProperty("screenshots.dir");
        Assume.assumeTrue("screenshots disabled", path != null && !path.isEmpty());
        dir = new File(path);
        dir.mkdirs();
        TestUtil.hebrewVoiceReady();
    }

    @After
    public void tearDown() {
        Speech.resetForTests();
    }

    @Test
    public void home() throws IOException {
        Activity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        TestUtil.idleFor(2000);
        save(a.getWindow().getDecorView(), "1-home");
    }

    @Test
    public void listening() throws IOException {
        LetterGridActivity a = Robolectric.buildActivity(LetterGridActivity.class,
                LetterGridActivity.intent(RuntimeEnvironment.getApplication(),
                        LetterGridActivity.MODE_LISTEN)).setup().get();
        GridView grid = a.findViewById(R.id.grid);
        TestUtil.idle();
        grid.performItemClick(grid.getChildAt(2), 2, 2);
        TestUtil.idleFor(1000);
        save(a.getWindow().getDecorView(), "2-listen");
    }

    @Test
    public void writingGrid() throws IOException {
        Prefs.setTraced(RuntimeEnvironment.getApplication(), 0);
        Prefs.setTraced(RuntimeEnvironment.getApplication(), 1);
        Activity a = Robolectric.buildActivity(LetterGridActivity.class,
                LetterGridActivity.intent(RuntimeEnvironment.getApplication(),
                        LetterGridActivity.MODE_WRITE)).setup().get();
        TestUtil.idleFor(500);
        save(a.getWindow().getDecorView(), "3-write-grid");
    }

    @Test
    public void tracing() throws IOException {
        TraceActivity a = Robolectric.buildActivity(TraceActivity.class,
                TraceActivity.intent(RuntimeEnvironment.getApplication(), 0)).setup().get();
        TestUtil.idleFor(800);
        save(a.getWindow().getDecorView(), "4-trace-start");

        TraceView trace = a.findViewById(R.id.trace);
        TestUtil.line(trace, trace.getWidth() * 0.3f, trace.getHeight() * 0.3f,
                trace.getWidth() * 0.6f, trace.getHeight() * 0.65f);
        TestUtil.idleFor(300);
        save(a.getWindow().getDecorView(), "5-trace-drawing");

        TraceTest.fillInside(trace, trace.getBrushWidth() * 0.5f);
        TestUtil.idleFor(700);
        save(a.getWindow().getDecorView(), "6-trace-done");
    }

    @Test
    public void wordsGrid() throws IOException {
        Activity a = Robolectric.buildActivity(LetterGridActivity.class,
                LetterGridActivity.intent(RuntimeEnvironment.getApplication(),
                        LetterGridActivity.MODE_WORDS)).setup().get();
        TestUtil.idleFor(500);
        save(a.getWindow().getDecorView(), "7-words-grid");
    }

    @Test
    public void words() throws IOException {
        WordsActivity a = Robolectric.buildActivity(WordsActivity.class,
                WordsActivity.intent(RuntimeEnvironment.getApplication(), 17)).setup().get();
        TestUtil.idleFor(800);
        save(a.getWindow().getDecorView(), "8-words");
    }

    @Test
    public void settings() throws IOException {
        Activity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        a.findViewById(R.id.btn_settings).performClick();
        TestUtil.idleFor(500);
        Dialog dialog = ShadowAlertDialog.getLatestDialog();
        save(dialog.getWindow().getDecorView(), "9-settings");
    }

    private void save(View view, String name) throws IOException {
        int w = view.getWidth();
        int h = view.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        view.draw(new Canvas(bitmap));
        try (OutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
