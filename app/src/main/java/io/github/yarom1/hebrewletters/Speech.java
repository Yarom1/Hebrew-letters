package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import java.util.List;
import java.util.Locale;

/**
 * Makes the phone speak Hebrew using its built-in text-to-speech engine.
 *
 * <p>One instance lives as long as the app, so the engine is ready the moment a child taps a
 * letter. If the phone's default engine has no Hebrew (common on Samsung phones) and Google's
 * engine is installed, it switches to Google's engine automatically. If there is still no Hebrew
 * voice, the screen that is currently open is told once, so it can explain to the parent how to
 * install one.
 */
final class Speech {

    /** Implemented by screens that can show the "no Hebrew voice" help dialog. */
    interface Listener {
        void onSpeechProblem(Speech speech);
    }

    static final int STATE_STARTING = 0;
    static final int STATE_READY = 1;
    /** The engine works but does not support Hebrew. */
    static final int STATE_NO_HEBREW = 2;
    /** The engine supports Hebrew but the voice has not been downloaded yet. */
    static final int STATE_MISSING_DATA = 3;
    /** There is no working text-to-speech engine. */
    static final int STATE_NO_ENGINE = 4;

    static final String GOOGLE_TTS = "com.google.android.tts";

    private static final int NO_STATUS = Integer.MIN_VALUE;

    private static Speech instance;

    private final Context app;
    private TextToSpeech tts;
    /** Package name of the engine in use, once known. */
    private String engine;
    private int state = STATE_STARTING;
    /** Incremented whenever an engine is replaced, so late callbacks from old engines are ignored. */
    private int generation;
    private int earlyStatus = NO_STATUS;
    private boolean triedGoogle;
    private String pendingText;
    private Listener listener;
    private boolean problemPending;
    private boolean problemShown;
    private int utteranceCounter;

    static synchronized Speech get(Context context) {
        if (instance == null) {
            instance = new Speech(context.getApplicationContext());
        }
        return instance;
    }

    private Speech(Context app) {
        this.app = app;
        start(null);
    }

    /** Drops the shared instance so the next {@link #get} starts afresh. Used by tests. */
    static synchronized void resetForTests() {
        if (instance != null) {
            instance.shutdownEngine();
            instance = null;
        }
    }

    int getState() {
        return state;
    }

    /** Says {@code text} in Hebrew, interrupting anything that is being said. */
    void speak(String text) {
        if (text == null || text.length() == 0) {
            return;
        }
        if (state == STATE_READY && tts != null) {
            try {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "letters-" + (++utteranceCounter));
            } catch (RuntimeException e) {
                // The engine's service died. Start it again and say this when it is back.
                restart();
                pendingText = text;
            }
        } else if (state == STATE_STARTING) {
            pendingText = text; // spoken as soon as the engine is ready
        } else {
            problemPending = true;
            notifyProblem();
        }
    }

    void stop() {
        if (tts != null && state == STATE_READY) {
            try {
                tts.stop();
            } catch (RuntimeException ignored) {
                // nothing to stop
            }
        }
    }

    void setListener(Listener listener) {
        this.listener = listener;
        notifyProblem();
    }

    void setRate(float rate) {
        if (tts != null && state == STATE_READY) {
            tts.setSpeechRate(rate);
        }
    }

    /** Starts the engine again from scratch, e.g. after the parent installed a Hebrew voice. */
    void restart() {
        shutdownEngine();
        triedGoogle = false;
        problemShown = false;
        problemPending = false;
        start(null);
    }

    /** A user-friendly name of the engine in use, e.g. "Speech Services by Google". */
    String engineLabel() {
        if (tts == null || engine == null) {
            return "";
        }
        try {
            for (TextToSpeech.EngineInfo info : tts.getEngines()) {
                if (engine.equals(info.name)) {
                    return info.label;
                }
            }
        } catch (RuntimeException ignored) {
            // fall through
        }
        return engine;
    }

    /**
     * The best way to get a Hebrew voice on this phone: the engine's own "install voice data"
     * screen when it supports Hebrew, otherwise Google's engine (or its Play Store page).
     */
    Intent installVoiceIntent() {
        if (state == STATE_MISSING_DATA && engine != null) {
            return new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(engine);
        }
        if (isInstalled(GOOGLE_TTS)) {
            return new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(GOOGLE_TTS);
        }
        return playStoreIntent(GOOGLE_TTS);
    }

    static Intent playStoreIntent(String packageName) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
    }

    static Intent playStoreWebIntent(String packageName) {
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
    }

    /** The phone's text-to-speech settings screen. */
    static Intent voiceSettingsIntent() {
        return new Intent("com.android.settings.TTS_SETTINGS");
    }

    private void start(String enginePackage) {
        final int gen = ++generation;
        state = STATE_STARTING;
        engine = enginePackage;
        earlyStatus = NO_STATUS;
        tts = null;
        TextToSpeech.OnInitListener onInit = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                onEngineInit(gen, status);
            }
        };
        TextToSpeech created;
        try {
            created = enginePackage == null
                    ? new TextToSpeech(app, onInit)
                    : new TextToSpeech(app, onInit, enginePackage);
        } catch (RuntimeException e) {
            fail(STATE_NO_ENGINE);
            return;
        }
        tts = created;
        if (earlyStatus != NO_STATUS) {
            // The engine answered from inside its constructor (this happens when it fails at once).
            int status = earlyStatus;
            earlyStatus = NO_STATUS;
            onEngineInit(gen, status);
        }
    }

    private void onEngineInit(int gen, int status) {
        if (gen != generation) {
            return; // a callback from an engine we already replaced
        }
        if (tts == null) {
            earlyStatus = status; // still inside the TextToSpeech constructor
            return;
        }
        if (status != TextToSpeech.SUCCESS) {
            if (!switchToGoogle()) {
                fail(STATE_NO_ENGINE);
            }
            return;
        }
        if (engine == null) {
            try {
                engine = tts.getDefaultEngine();
            } catch (RuntimeException ignored) {
                // unknown engine
            }
        }
        int best = TextToSpeech.LANG_NOT_SUPPORTED;
        for (Locale locale : hebrewLocales()) {
            int result = isLanguageAvailable(locale);
            if (result >= TextToSpeech.LANG_AVAILABLE) {
                result = setLanguage(locale);
                if (result >= TextToSpeech.LANG_AVAILABLE) {
                    tts.setSpeechRate(Prefs.speechRate(app));
                    state = STATE_READY;
                    String text = pendingText;
                    pendingText = null;
                    speak(text);
                    return;
                }
            }
            best = Math.max(best, result);
        }
        if (switchToGoogle()) {
            return;
        }
        fail(best == TextToSpeech.LANG_MISSING_DATA ? STATE_MISSING_DATA : STATE_NO_HEBREW);
    }

    private static Locale[] hebrewLocales() {
        // Java historically calls Hebrew "iw"; engines accept one or the other.
        return new Locale[] {
            new Locale("he", "IL"), new Locale("iw", "IL"), new Locale("he"), new Locale("iw")
        };
    }

    private int isLanguageAvailable(Locale locale) {
        try {
            return tts.isLanguageAvailable(locale);
        } catch (RuntimeException e) {
            return TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    private int setLanguage(Locale locale) {
        try {
            return tts.setLanguage(locale);
        } catch (RuntimeException e) {
            return TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    /** Tries Google's engine once if it is installed and is not the one that just failed. */
    private boolean switchToGoogle() {
        if (triedGoogle) {
            return false;
        }
        triedGoogle = true;
        if (GOOGLE_TTS.equals(engine) || !isInstalled(GOOGLE_TTS)) {
            return false;
        }
        String text = pendingText;
        shutdownEngine();
        start(GOOGLE_TTS);
        if (pendingText == null) {
            pendingText = text;
        }
        return true;
    }

    private boolean isInstalled(String enginePackage) {
        if (tts == null) {
            return false;
        }
        try {
            List<TextToSpeech.EngineInfo> engines = tts.getEngines();
            if (engines != null) {
                for (TextToSpeech.EngineInfo info : engines) {
                    if (enginePackage.equals(info.name)) {
                        return true;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // treat as not installed
        }
        return false;
    }

    private void fail(int newState) {
        state = newState;
        problemPending = true;
        notifyProblem();
    }

    private void notifyProblem() {
        if (!problemPending || listener == null) {
            return;
        }
        if (state == STATE_READY || state == STATE_STARTING) {
            problemPending = false;
            return;
        }
        problemPending = false;
        if (problemShown) {
            return; // tell the parent once; the settings screen always shows the status
        }
        problemShown = true;
        listener.onSpeechProblem(this);
    }

    private void shutdownEngine() {
        TextToSpeech old = tts;
        tts = null;
        generation++;
        if (old != null) {
            try {
                old.shutdown();
            } catch (RuntimeException ignored) {
                // already gone
            }
        }
    }
}
