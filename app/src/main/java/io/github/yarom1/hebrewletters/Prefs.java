package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.content.SharedPreferences;

/** Small persistent settings: which letters earned a star, and how fast the phone speaks. */
final class Prefs {

    static final float RATE_SLOW = 0.85f;
    static final float RATE_NORMAL = 1.0f;

    private static final String FILE = "hebrew_letters";
    private static final String KEY_TRACED = "traced_";
    private static final String KEY_RATE = "speech_rate";

    private Prefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean isTraced(Context context, int letter) {
        return prefs(context).getBoolean(KEY_TRACED + letter, false);
    }

    static void setTraced(Context context, int letter) {
        prefs(context).edit().putBoolean(KEY_TRACED + letter, true).apply();
    }

    static int tracedCount(Context context) {
        int count = 0;
        for (int i = 0; i < Alphabet.count(); i++) {
            if (isTraced(context, i)) {
                count++;
            }
        }
        return count;
    }

    static void resetStars(Context context) {
        SharedPreferences.Editor editor = prefs(context).edit();
        for (int i = 0; i < Alphabet.count(); i++) {
            editor.remove(KEY_TRACED + i);
        }
        editor.apply();
    }

    /** Children follow slower speech more easily, so "slow" is the default. */
    static float speechRate(Context context) {
        return prefs(context).getFloat(KEY_RATE, RATE_SLOW);
    }

    static void setSpeechRate(Context context, float rate) {
        prefs(context).edit().putFloat(KEY_RATE, rate).apply();
    }
}
