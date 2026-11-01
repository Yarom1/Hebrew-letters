package io.github.yarom1.hebrewletters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

/**
 * Common behaviour for every screen: right-to-left Hebrew layout, the app font, the back button,
 * speaking, and helping the parent when the phone has no Hebrew voice.
 */
abstract class BaseActivity extends Activity implements Speech.Listener {

    private static boolean muteHintShown;

    /** Set when we sent the parent to install a voice; the engine is restarted on return. */
    private boolean voiceSetupOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Phones stay upright (small hands rotate phones by accident); tablets may rotate.
        if (getResources().getConfiguration().smallestScreenWidthDp < 600) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        // Hebrew reads right-to-left, whatever language the phone is set to.
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    @Override
    public void setContentView(int layoutResId) {
        super.setContentView(layoutResId);
        Fonts.apply(findViewById(android.R.id.content));
        View back = findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    /** Colours the screen background and the status bar above it. */
    protected void setScreenColor(int color) {
        getWindow().setStatusBarColor(color);
        getWindow().getDecorView().setBackgroundColor(color);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Speech speech = Speech.get(this);
        if (voiceSetupOpened) {
            voiceSetupOpened = false;
            speech.restart();
        }
        speech.setListener(this);
    }

    @Override
    protected void onPause() {
        Speech speech = Speech.get(this);
        speech.setListener(null);
        speech.stop();
        super.onPause();
    }

    /** Says {@code text} out loud in Hebrew. */
    protected void say(String text) {
        Speech.get(this).speak(text);
        warnIfMuted();
    }

    /** Says the name of letter {@code index}, e.g. "הָאוֹת אָלֶף". */
    protected void sayLetter(int index) {
        say(getString(R.string.say_letter, Alphabet.get(index).name));
    }

    private void warnIfMuted() {
        if (muteHintShown) {
            return;
        }
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audio != null && audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
            muteHintShown = true;
            Toast.makeText(this, R.string.volume_off, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSpeechProblem(Speech speech) {
        if (isFinishing()) {
            return;
        }
        showVoiceHelp(speech);
    }

    /** Explains to the parent why there is no sound, with buttons that fix it. */
    protected void showVoiceHelp(final Speech speech) {
        int message;
        switch (speech.getState()) {
            case Speech.STATE_MISSING_DATA:
                message = R.string.voice_missing_data;
                break;
            case Speech.STATE_NO_HEBREW:
                message = R.string.voice_no_hebrew;
                break;
            default:
                message = R.string.voice_no_engine;
                break;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.voice_problem_title)
                .setMessage(message)
                .setPositiveButton(R.string.install_voice, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        openVoiceSetup(speech.installVoiceIntent());
                    }
                })
                .setNeutralButton(R.string.voice_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        openVoiceSetup(Speech.voiceSettingsIntent());
                    }
                })
                .setNegativeButton(R.string.close, null)
                .create();
        dialog.show();
        View content = dialog.findViewById(android.R.id.content);
        if (content != null) {
            content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            Fonts.apply(content);
        }
    }

    /** Opens a voice set-up screen, falling back to the Play Store web page or a message. */
    protected void openVoiceSetup(Intent intent) {
        voiceSetupOpened = true;
        if (tryStart(intent)) {
            return;
        }
        if ("market".equals(intent.getScheme()) && tryStart(Speech.playStoreWebIntent(Speech.GOOGLE_TTS))) {
            return;
        }
        if (tryStart(Speech.voiceSettingsIntent())) {
            return;
        }
        voiceSetupOpened = false;
        Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_LONG).show();
    }

    private boolean tryStart(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
