package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

/**
 * A grid with all the letters. What happens when a letter is tapped depends on the mode:
 * writing opens the tracing screen, listening says the letter's name, words opens the words screen.
 */
public class LetterGridActivity extends BaseActivity {

    static final String EXTRA_MODE = "mode";
    static final int MODE_WRITE = 0;
    static final int MODE_LISTEN = 1;
    static final int MODE_WORDS = 2;

    private static final String STATE_SPOTLIGHT = "spotlight";

    private int mode;
    private int tileSize;
    private int spotlightLetter;
    private LetterAdapter adapter;
    private TextView spotLetter;
    private TextView spotName;
    private View spotlight;

    static Intent intent(Context context, int mode) {
        return new Intent(context, LetterGridActivity.class).putExtra(EXTRA_MODE, mode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_LISTEN);
        setContentView(R.layout.activity_letter_grid);

        int titleRes;
        int bgColor;
        switch (mode) {
            case MODE_WRITE:
                titleRes = R.string.mode_write;
                bgColor = getColor(R.color.mode_write_bg);
                break;
            case MODE_WORDS:
                titleRes = R.string.mode_words;
                bgColor = getColor(R.color.mode_words_bg);
                break;
            default:
                titleRes = R.string.mode_listen;
                bgColor = getColor(R.color.mode_listen_bg);
                break;
        }
        setScreenColor(bgColor);
        ((TextView) findViewById(R.id.header_title)).setText(titleRes);

        spotlight = findViewById(R.id.spotlight);
        spotLetter = (TextView) findViewById(R.id.spot_letter);
        spotName = (TextView) findViewById(R.id.spot_name);
        if (mode == MODE_LISTEN) {
            spotlightLetter = savedInstanceState == null ? 0
                    : savedInstanceState.getInt(STATE_SPOTLIGHT, 0);
            spotlight.setVisibility(View.VISIBLE);
            showSpotlight(spotlightLetter);
            spotlight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Ui.bounce(spotLetter);
                    sayLetter(spotlightLetter);
                }
            });
        }

        GridView grid = (GridView) findViewById(R.id.grid);
        configureGrid(grid);
        adapter = new LetterAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                onLetterTapped(position, view);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged(); // stars may have changed on the tracing screen
        updateBadge();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SPOTLIGHT, spotlightLetter);
    }

    /** Picks a number of columns that gives big, easy-to-hit tiles on any screen width. */
    private void configureGrid(GridView grid) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int spacing = Ui.dp(this, 10);
        // The window width (correct in split-screen too), minus the grid's side padding.
        int windowWidth = Math.round(getResources().getConfiguration().screenWidthDp * metrics.density);
        int available = windowWidth - grid.getPaddingLeft() - grid.getPaddingRight();
        // About 80dp per tile: four big tiles across a phone, more on tablets.
        int columns = (int) (available / (80 * metrics.density));
        columns = Math.max(3, Math.min(columns, 8));
        tileSize = (available - spacing * (columns - 1)) / columns;
        grid.setNumColumns(columns);
        grid.setColumnWidth(tileSize);
        grid.setHorizontalSpacing(spacing);
        grid.setVerticalSpacing(spacing);
    }

    private void updateBadge() {
        TextView badge = (TextView) findViewById(R.id.header_badge);
        if (mode == MODE_WRITE) {
            badge.setText(getString(R.string.stars_count, Prefs.tracedCount(this), Alphabet.count()));
        } else {
            badge.setText("");
        }
    }

    private void onLetterTapped(int index, View tile) {
        switch (mode) {
            case MODE_WRITE:
                startActivity(TraceActivity.intent(this, index));
                break;
            case MODE_WORDS:
                startActivity(WordsActivity.intent(this, index));
                break;
            default:
                Ui.bounce(tile);
                showSpotlight(index);
                Ui.bounce(spotLetter);
                sayLetter(index);
                break;
        }
    }

    private void showSpotlight(int index) {
        spotlightLetter = index;
        Alphabet.Letter letter = Alphabet.get(index);
        spotLetter.setText(letter.glyph);
        spotLetter.setTextColor(Ui.letterColor(index));
        spotName.setText(letter.name);
    }

    private final class LetterAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return Alphabet.count();
        }

        @Override
        public Object getItem(int position) {
            return Alphabet.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View tile = convertView;
            if (tile == null) {
                tile = getLayoutInflater().inflate(R.layout.item_letter, parent, false);
                tile.setLayoutParams(new AbsListView.LayoutParams(tileSize, tileSize));
                Fonts.apply(tile);
            }
            Alphabet.Letter letter = Alphabet.get(position);
            tile.setBackground(Ui.touchable(
                    Ui.rounded(Ui.letterColor(position), tileSize * 0.24f)));
            tile.setElevation(Ui.dp(LetterGridActivity.this, 3));
            tile.setContentDescription(letter.name);

            TextView glyph = (TextView) tile.findViewById(R.id.tile_letter);
            glyph.setText(letter.glyph);
            TextView emoji = (TextView) tile.findViewById(R.id.tile_emoji);
            TextView star = (TextView) tile.findViewById(R.id.tile_star);

            if (mode == MODE_WORDS) {
                // Letter at the top, a little picture of its first word underneath.
                glyph.setTextSize(TypedValue.COMPLEX_UNIT_PX, tileSize * 0.42f);
                glyph.setPadding(0, 0, 0, (int) (tileSize * 0.26f));
                emoji.setVisibility(View.VISIBLE);
                emoji.setText(letter.words[0].emoji);
                emoji.setTextSize(TypedValue.COMPLEX_UNIT_PX, tileSize * 0.24f);
            } else {
                glyph.setTextSize(TypedValue.COMPLEX_UNIT_PX, tileSize * 0.56f);
                glyph.setPadding(0, 0, 0, 0);
                emoji.setVisibility(View.GONE);
            }
            star.setVisibility(mode == MODE_WRITE && Prefs.isTraced(LetterGridActivity.this, position)
                    ? View.VISIBLE : View.GONE);
            return tile;
        }
    }
}
