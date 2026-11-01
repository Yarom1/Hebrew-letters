package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Shows a picture and a word that starts with the chosen letter, and says the word. */
public class WordsActivity extends BaseActivity {

    static final String EXTRA_LETTER = "letter";
    private static final String STATE_WORD = "word";

    private int index;
    private int wordIndex;
    private Alphabet.Word[] words;
    private TextView chip;
    private TextView emoji;
    private TextView wordText;
    private LinearLayout dots;
    private View another;

    static Intent intent(Context context, int letter) {
        return new Intent(context, WordsActivity.class).putExtra(EXTRA_LETTER, letter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int startWord = 0;
        if (savedInstanceState != null) {
            index = savedInstanceState.getInt(EXTRA_LETTER, 0);
            startWord = savedInstanceState.getInt(STATE_WORD, 0);
        } else {
            index = getIntent().getIntExtra(EXTRA_LETTER, 0);
        }
        setContentView(R.layout.activity_words);
        setScreenColor(getColor(R.color.mode_words_bg));

        chip = (TextView) findViewById(R.id.letter_chip);
        emoji = (TextView) findViewById(R.id.word_emoji);
        wordText = (TextView) findViewById(R.id.word_text);
        dots = (LinearLayout) findViewById(R.id.word_dots);
        another = findViewById(R.id.btn_another);

        View.OnClickListener sayName = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.bounce(chip);
                sayLetter(index);
            }
        };
        chip.setOnClickListener(sayName);
        findViewById(R.id.btn_speak).setOnClickListener(sayName);

        findViewById(R.id.btn_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLetter(index - 1, 0, true);
            }
        });
        findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLetter(index + 1, 0, true);
            }
        });

        View.OnClickListener sayWord = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.bounce(emoji);
                say(words[wordIndex].text);
            }
        };
        findViewById(R.id.word_card).setOnClickListener(sayWord);
        View sayButton = findViewById(R.id.btn_say_word);
        sayButton.setBackground(Ui.touchable(
                Ui.rounded(getColor(R.color.mode_words), Ui.dp(this, 32))));
        sayButton.setOnClickListener(sayWord);

        another.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.bounce(v);
                wordIndex = (wordIndex + 1) % words.length;
                showWord(true);
            }
        });

        showLetter(index, startWord, savedInstanceState == null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_LETTER, index);
        outState.putInt(STATE_WORD, wordIndex);
    }

    private void showLetter(int newIndex, int startWord, boolean speak) {
        index = Alphabet.wrap(newIndex);
        Alphabet.Letter letter = Alphabet.get(index);
        words = wordsWithPictures(letter);
        wordIndex = Math.max(0, Math.min(startWord, words.length - 1));
        chip.setText(letter.glyph);
        chip.setBackground(Ui.touchable(Ui.circle(Ui.letterColor(index))));
        chip.setContentDescription(letter.name);
        Ui.bounce(chip);
        another.setVisibility(words.length > 1 ? View.VISIBLE : View.GONE);
        buildDots();
        showWord(speak);
    }

    private void showWord(boolean speak) {
        Alphabet.Word word = words[wordIndex];
        emoji.setText(word.emoji);
        wordText.setText(highlightFirstLetter(word.text, Ui.letterColor(index)));
        wordText.setContentDescription(word.text);
        for (int i = 0; i < dots.getChildCount(); i++) {
            dots.getChildAt(i).setBackground(Ui.circle(
                    i == wordIndex ? Ui.letterColor(index) : 0xFFD9D9E3));
        }
        Ui.popIn(emoji, 0);
        if (speak) {
            say(word.text);
        }
    }

    private void buildDots() {
        dots.removeAllViews();
        if (words.length < 2) {
            return;
        }
        int size = Ui.dp(this, 12);
        int margin = Ui.dp(this, 5);
        for (int i = 0; i < words.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, 0, margin, 0);
            dots.addView(dot, lp);
        }
    }

    /**
     * Words whose picture this phone can draw. Newer emoji (like 🪝) are missing on older phones;
     * those words are skipped there. If none can be checked, all words are used.
     */
    static Alphabet.Word[] wordsWithPictures(Alphabet.Letter letter) {
        Paint paint = new Paint();
        List<Alphabet.Word> supported = new ArrayList<>();
        for (Alphabet.Word word : letter.words) {
            if (paint.hasGlyph(word.emoji)) {
                supported.add(word);
            }
        }
        if (supported.isEmpty()) {
            return letter.words;
        }
        return supported.toArray(new Alphabet.Word[0]);
    }

    /** Colours the first letter of the word (together with its vowel points). */
    static CharSequence highlightFirstLetter(String text, int color) {
        int end = 1;
        while (end < text.length() && Alphabet.isHebrewMark(text.charAt(end))) {
            end++;
        }
        SpannableString styled = new SpannableString(text);
        styled.setSpan(new ForegroundColorSpan(color), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
    }

    String currentWord() {
        return words[wordIndex].text;
    }
}
