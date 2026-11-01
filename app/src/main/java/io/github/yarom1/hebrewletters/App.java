package io.github.yarom1.hebrewletters;

import android.app.Application;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fonts.init(this);
        // Start the text-to-speech engine early so the first letter is spoken without delay.
        Speech.get(this);
    }
}
