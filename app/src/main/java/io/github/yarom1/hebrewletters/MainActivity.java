package io.github.yarom1.hebrewletters;

import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

/** Home screen: three big cards, one for each way of learning the letters. */
public class MainActivity extends BaseActivity {

    /** The colourful letters shown under the title. */
    private static final int STRIP_LETTERS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(getColor(R.color.bg_home_top));

        setUpCard(R.id.card_write, R.color.mode_write_light, R.color.mode_write,
                LetterGridActivity.MODE_WRITE);
        setUpCard(R.id.card_listen, R.color.mode_listen_light, R.color.mode_listen,
                LetterGridActivity.MODE_LISTEN);
        setUpCard(R.id.card_words, R.color.mode_words_light, R.color.mode_words,
                LetterGridActivity.MODE_WORDS);
        buildLetterStrip();

        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });
    }

    private void setUpCard(int id, int lightColor, int color, final int mode) {
        View card = findViewById(id);
        card.setBackground(Ui.touchable(
                Ui.gradient(getColor(lightColor), getColor(color), Ui.dp(this, 28))));
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.bounce(v);
                startActivity(LetterGridActivity.intent(MainActivity.this, mode));
            }
        });
    }

    private void buildLetterStrip() {
        LinearLayout strip = (LinearLayout) findViewById(R.id.letter_strip);
        int size = Ui.dp(this, 50);
        int margin = Ui.dp(this, 4);
        for (int i = 0; i < STRIP_LETTERS; i++) {
            final int index = i;
            TextView tile = new TextView(this);
            tile.setText(Alphabet.get(i).glyph);
            tile.setTextColor(getColor(R.color.white));
            tile.setTextSize(30);
            tile.setTypeface(Fonts.bold(this));
            tile.setGravity(Gravity.CENTER);
            tile.setIncludeFontPadding(false);
            tile.setBackground(Ui.touchable(Ui.rounded(Ui.letterColor(i), Ui.dp(this, 14))));
            tile.setElevation(Ui.dp(this, 3));
            tile.setContentDescription(Alphabet.get(i).name);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, margin, margin, margin);
            strip.addView(tile, lp);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Ui.bounce(v);
                    sayLetter(index);
                }
            });
            Ui.popIn(tile, 150 + 90L * i);
        }
    }

    /** A small settings panel for parents: voice check, speech speed, resetting stars. */
    private void showSettings() {
        final View panel = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Fonts.apply(panel);
        final Speech speech = Speech.get(this);

        TextView status = (TextView) panel.findViewById(R.id.voice_status);
        switch (speech.getState()) {
            case Speech.STATE_READY:
                status.setText(getString(R.string.voice_status_ready, speech.engineLabel()));
                break;
            case Speech.STATE_STARTING:
                status.setText(R.string.voice_status_starting);
                break;
            default:
                status.setText(R.string.voice_status_problem);
                break;
        }

        panel.findViewById(R.id.btn_test_voice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (speech.getState() == Speech.STATE_READY
                        || speech.getState() == Speech.STATE_STARTING) {
                    say(getString(R.string.test_sentence));
                } else {
                    showVoiceHelp(speech);
                }
            }
        });

        RadioGroup rates = (RadioGroup) panel.findViewById(R.id.rate_group);
        boolean slow = Prefs.speechRate(this) < (Prefs.RATE_SLOW + Prefs.RATE_NORMAL) / 2;
        ((RadioButton) panel.findViewById(slow ? R.id.rate_slow : R.id.rate_normal)).setChecked(true);
        rates.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                float rate = checkedId == R.id.rate_slow ? Prefs.RATE_SLOW : Prefs.RATE_NORMAL;
                Prefs.setSpeechRate(MainActivity.this, rate);
                speech.setRate(rate);
                say(getString(R.string.test_sentence));
            }
        });

        panel.findViewById(R.id.btn_install_voice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openVoiceSetup(speech.installVoiceIntent());
            }
        });
        panel.findViewById(R.id.btn_phone_voice_settings).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openVoiceSetup(Speech.voiceSettingsIntent());
                    }
                });
        Button reset = (Button) panel.findViewById(R.id.btn_reset_stars);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.resetStars(MainActivity.this);
                Toast.makeText(MainActivity.this, R.string.stars_reset_done, Toast.LENGTH_SHORT)
                        .show();
            }
        });

        ((TextView) panel.findViewById(R.id.about)).setText(getString(R.string.about, versionName()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.settings_title)
                .setView(panel)
                .setPositiveButton(R.string.close, null)
                .create();
        dialog.show();
        View content = dialog.findViewById(android.R.id.content);
        if (content != null) {
            content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            Fonts.apply(content);
        }
    }

    private String versionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
