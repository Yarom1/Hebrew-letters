package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Random;

/** Writing practice: the child traces one big letter with a finger. */
public class TraceActivity extends BaseActivity implements TraceView.Listener {

    static final String EXTRA_LETTER = "letter";
    private static final String STATE_CRAYON = "crayon";

    /** Crayon colours the child can pick from. */
    static final int[] CRAYONS = {0xFF1C7ED6, 0xFFF03E3E, 0xFF2F9E44, 0xFFF76707, 0xFF7048E8};

    private final Random random = new Random();
    private int index;
    private int crayon;
    private TraceView trace;
    private ConfettiView confetti;
    private View success;
    private TextView successText;
    private View hint;
    private TextView chip;
    private TextView name;
    private LinearLayout palette;

    static Intent intent(Context context, int letter) {
        return new Intent(context, TraceActivity.class).putExtra(EXTRA_LETTER, letter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            index = savedInstanceState.getInt(EXTRA_LETTER, 0);
            crayon = savedInstanceState.getInt(STATE_CRAYON, 0);
        } else {
            index = getIntent().getIntExtra(EXTRA_LETTER, 0);
        }
        setContentView(R.layout.activity_trace);
        setScreenColor(getColor(R.color.mode_write_bg));

        trace = (TraceView) findViewById(R.id.trace);
        trace.setListener(this);
        trace.setClipToOutline(true); // keep the paint inside the rounded card
        confetti = (ConfettiView) findViewById(R.id.confetti);
        success = findViewById(R.id.success);
        successText = (TextView) findViewById(R.id.success_text);
        hint = findViewById(R.id.trace_hint);
        chip = (TextView) findViewById(R.id.letter_chip);
        name = (TextView) findViewById(R.id.letter_name);

        View.OnClickListener sayName = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.bounce(chip);
                sayLetter(index);
            }
        };
        chip.setOnClickListener(sayName);
        name.setOnClickListener(sayName);
        findViewById(R.id.btn_speak).setOnClickListener(sayName);

        findViewById(R.id.btn_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLetter(index - 1, true);
            }
        });
        View.OnClickListener next = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLetter(index + 1, true);
            }
        };
        findViewById(R.id.btn_next).setOnClickListener(next);
        View nextLetter = findViewById(R.id.btn_next_letter);
        nextLetter.setBackground(Ui.touchable(
                Ui.rounded(getColor(R.color.success), Ui.dp(this, 32))));
        nextLetter.setOnClickListener(next);

        View.OnClickListener startOver = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.bounce(v);
                trace.clear();
                success.setVisibility(View.GONE);
                hint.setVisibility(View.VISIBLE);
            }
        };
        findViewById(R.id.btn_clear).setOnClickListener(startOver);
        findViewById(R.id.btn_again).setOnClickListener(startOver);

        buildPalette();
        showLetter(index, savedInstanceState == null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_LETTER, index);
        outState.putInt(STATE_CRAYON, crayon);
    }

    private void showLetter(int newIndex, boolean speak) {
        index = Alphabet.wrap(newIndex);
        Alphabet.Letter letter = Alphabet.get(index);
        int color = Ui.letterColor(index);
        chip.setText(letter.glyph);
        chip.setBackground(Ui.touchable(Ui.circle(color)));
        chip.setContentDescription(letter.name);
        name.setText(Alphabet.forDisplay(letter.name));
        name.setTextColor(color);
        trace.setGuideColor(color);
        trace.setLetter(letter.glyph);
        trace.setContentDescription(getString(R.string.trace_description, letter.name));
        success.setVisibility(View.GONE);
        hint.setVisibility(View.VISIBLE);
        Ui.bounce(chip);
        if (speak) {
            sayLetter(index);
        }
    }

    private void buildPalette() {
        palette = (LinearLayout) findViewById(R.id.palette);
        int size = Ui.dp(this, 42);
        int margin = Ui.dp(this, 6);
        for (int i = 0; i < CRAYONS.length; i++) {
            final int crayonIndex = i;
            View swatch = new View(this);
            swatch.setContentDescription(getString(R.string.color_n, i + 1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, 0, margin, 0);
            palette.addView(swatch, lp);
            swatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectCrayon(crayonIndex);
                }
            });
        }
        selectCrayon(crayon);
    }

    private void selectCrayon(int newCrayon) {
        crayon = newCrayon;
        trace.setInkColor(CRAYONS[crayon]);
        for (int i = 0; i < palette.getChildCount(); i++) {
            View swatch = palette.getChildAt(i);
            boolean selected = i == crayon;
            GradientDrawable dot = Ui.circle(CRAYONS[i]);
            if (selected) {
                dot.setStroke(Ui.dp(this, 4), getColor(R.color.ink_dark));
            }
            swatch.setBackground(dot);
            swatch.setElevation(Ui.dp(this, selected ? 6 : 2));
            swatch.animate().scaleX(selected ? 1.22f : 1f).scaleY(selected ? 1.22f : 1f)
                    .setDuration(150).start();
        }
    }

    @Override
    public void onTraceStarted() {
        hint.setVisibility(View.GONE);
    }

    @Override
    public void onTraceCompleted() {
        Prefs.setTraced(this, index);
        String[] praise = getResources().getStringArray(R.array.praise);
        String text = praise[random.nextInt(praise.length)];
        successText.setText(Alphabet.forDisplay(text));
        success.setVisibility(View.VISIBLE);
        Ui.popIn(success, 200);
        confetti.burst();
        say(text);
    }

    int getLetterIndex() {
        return index;
    }
}
