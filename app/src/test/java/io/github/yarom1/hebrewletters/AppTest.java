package io.github.yarom1.hebrewletters;

import static io.github.yarom1.hebrewletters.TestUtil.hebrewVoiceReady;
import static io.github.yarom1.hebrewletters.TestUtil.idle;
import static io.github.yarom1.hebrewletters.TestUtil.lastSpoken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

/** Opens every screen and checks the three learning modes behave as expected. */
@RunWith(RobolectricTestRunner.class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
public class AppTest {

    @After
    public void tearDown() {
        Speech.resetForTests();
    }

    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void homeCardsOpenTheThreeModes() {
        MainActivity home = Robolectric.buildActivity(MainActivity.class).setup().get();
        int[] cards = {R.id.card_write, R.id.card_listen, R.id.card_words};
        int[] modes = {
            LetterGridActivity.MODE_WRITE, LetterGridActivity.MODE_LISTEN, LetterGridActivity.MODE_WORDS
        };
        for (int i = 0; i < cards.length; i++) {
            home.findViewById(cards[i]).performClick();
            Intent started = shadowOf(home).getNextStartedActivity();
            assertEquals(LetterGridActivity.class.getName(), started.getComponent().getClassName());
            assertEquals(modes[i], started.getIntExtra(LetterGridActivity.EXTRA_MODE, -1));
        }
    }

    @Test
    public void homeLettersSayTheirNames() {
        hebrewVoiceReady();
        MainActivity home = Robolectric.buildActivity(MainActivity.class).setup().get();
        LinearLayout strip = home.findViewById(R.id.letter_strip);
        assertEquals(5, strip.getChildCount());
        strip.getChildAt(0).performClick();
        assertEquals("הָאוֹת אָלֶף", lastSpoken());
    }

    @Test
    public void listeningModeSaysTheTappedLetter() {
        hebrewVoiceReady();
        LetterGridActivity grid = Robolectric.buildActivity(LetterGridActivity.class,
                LetterGridActivity.intent(context(), LetterGridActivity.MODE_LISTEN)).setup().get();
        GridView view = grid.findViewById(R.id.grid);
        assertEquals(22, view.getAdapter().getCount());
        clickItem(view, 1);
        assertEquals("הָאוֹת בֵּית", lastSpoken());
        assertEquals("ב", ((TextView) grid.findViewById(R.id.spot_letter)).getText().toString());
        assertEquals("בֵּית", ((TextView) grid.findViewById(R.id.spot_name)).getText().toString());

        grid.findViewById(R.id.spotlight).performClick();
        assertEquals("הָאוֹת בֵּית", lastSpoken());
    }

    @Test
    public void writingModeOpensTheTracingScreen() {
        LetterGridActivity grid = Robolectric.buildActivity(LetterGridActivity.class,
                LetterGridActivity.intent(context(), LetterGridActivity.MODE_WRITE)).setup().get();
        assertEquals(View.GONE, grid.findViewById(R.id.spotlight).getVisibility());
        clickItem((GridView) grid.findViewById(R.id.grid), 3);
        Intent started = shadowOf(grid).getNextStartedActivity();
        assertEquals(TraceActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(3, started.getIntExtra(TraceActivity.EXTRA_LETTER, -1));
    }

    @Test
    public void wordsModeOpensTheWordsScreen() {
        LetterGridActivity grid = Robolectric.buildActivity(LetterGridActivity.class,
                LetterGridActivity.intent(context(), LetterGridActivity.MODE_WORDS)).setup().get();
        clickItem((GridView) grid.findViewById(R.id.grid), 21);
        Intent started = shadowOf(grid).getNextStartedActivity();
        assertEquals(WordsActivity.class.getName(), started.getComponent().getClassName());
        assertEquals(21, started.getIntExtra(WordsActivity.EXTRA_LETTER, -1));
    }

    @Test
    public void wordsScreenShowsAndSaysWordsThatStartWithTheLetter() {
        hebrewVoiceReady();
        WordsActivity words = Robolectric.buildActivity(WordsActivity.class,
                WordsActivity.intent(context(), 0)).setup().get();
        assertEquals("א", ((TextView) words.findViewById(R.id.letter_chip)).getText().toString());
        String first = words.currentWord();
        assertTrue(Alphabet.withoutNiqqud(first).startsWith("א"));
        assertEquals(first, lastSpoken());
        assertEquals(first, ((TextView) words.findViewById(R.id.word_text)).getText().toString());

        words.findViewById(R.id.btn_another).performClick();
        String second = words.currentWord();
        assertNotEquals(first, second);
        assertTrue(Alphabet.withoutNiqqud(second).startsWith("א"));
        assertEquals(second, lastSpoken());

        words.findViewById(R.id.btn_say_word).performClick();
        assertEquals(second, lastSpoken());

        words.findViewById(R.id.btn_speak).performClick();
        assertEquals("הָאוֹת אָלֶף", lastSpoken());

        // Going back from א wraps around to ת.
        words.findViewById(R.id.btn_prev).performClick();
        assertEquals("ת", ((TextView) words.findViewById(R.id.letter_chip)).getText().toString());
        assertTrue(Alphabet.withoutNiqqud(words.currentWord()).startsWith("ת"));
        words.findViewById(R.id.btn_next).performClick();
        assertEquals("א", ((TextView) words.findViewById(R.id.letter_chip)).getText().toString());
    }

    @Test
    public void tracingScreenSaysTheLetterOnceTheVoiceIsReady() {
        // The engine is still starting when the screen opens: the name must be said when ready.
        TraceActivity trace = Robolectric.buildActivity(TraceActivity.class,
                TraceActivity.intent(context(), 2)).setup().get();
        assertNull(lastSpoken());
        hebrewVoiceReady();
        assertEquals("הָאוֹת גִּימֶל", lastSpoken());
        assertEquals("ג", ((TextView) trace.findViewById(R.id.letter_chip)).getText().toString());

        trace.findViewById(R.id.btn_next).performClick();
        assertEquals("הָאוֹת דָּלֶת", lastSpoken());
        assertEquals(3, trace.getLetterIndex());
    }

    @Test
    public void explainsWhatToDoWhenThePhoneHasNoHebrewVoice() {
        Robolectric.buildActivity(MainActivity.class).setup();
        TestUtil.engineStarted(); // engine works, but no Hebrew
        idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("a help dialog should explain how to get a Hebrew voice", dialog);
        assertTrue(dialog.isShowing());
        assertEquals(Speech.STATE_NO_HEBREW, Speech.get(context()).getState());
    }

    @Test
    public void settingsPanelOpens() {
        hebrewVoiceReady();
        MainActivity home = Robolectric.buildActivity(MainActivity.class).setup().get();
        home.findViewById(R.id.btn_settings).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        TextView status = dialog.findViewById(R.id.voice_status);
        assertTrue(status.getText().toString().startsWith("✅"));
        dialog.findViewById(R.id.btn_test_voice).performClick();
        assertEquals(home.getString(R.string.test_sentence), lastSpoken());
    }

    private static void clickItem(GridView grid, int position) {
        grid.setSelection(position);
        idle();
        View child = grid.getChildAt(position - grid.getFirstVisiblePosition());
        assertNotNull("tile " + position + " should be on screen", child);
        grid.performItemClick(child, position, position);
    }
}
