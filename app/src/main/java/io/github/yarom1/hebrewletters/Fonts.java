package io.github.yarom1.hebrewletters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * The app's typeface: Rubik, a friendly, rounded font with clear Hebrew block letters and full
 * niqqud support. It is bundled so the letters look the same on every phone.
 */
final class Fonts {

    /** Views with this tag keep the system font (used for emoji pictures). */
    static final String TAG_KEEP_FONT = "emoji";

    private static Typeface bold;
    private static Typeface medium;

    private Fonts() {}

    static synchronized void init(Context context) {
        if (bold != null) {
            return;
        }
        bold = load(context, "fonts/Rubik-Bold.ttf", Typeface.DEFAULT_BOLD);
        medium = load(context, "fonts/Rubik-Medium.ttf", Typeface.DEFAULT);
    }

    private static Typeface load(Context context, String asset, Typeface fallback) {
        try {
            return Typeface.createFromAsset(context.getAssets(), asset);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    static Typeface bold(Context context) {
        init(context);
        return bold;
    }

    static Typeface medium(Context context) {
        init(context);
        return medium;
    }

    /**
     * Applies the app font to every TextView (and Button) under {@code root}: bold text gets
     * Rubik Bold, everything else Rubik Medium.
     */
    static void apply(View root) {
        if (root == null) {
            return;
        }
        if (root instanceof TextView) {
            TextView text = (TextView) root;
            if (!TAG_KEEP_FONT.equals(text.getTag())) {
                Typeface current = text.getTypeface();
                boolean isBold = current != null && current.isBold();
                text.setTypeface(isBold ? bold(root.getContext()) : medium(root.getContext()));
                CharSequence content = text.getText();
                if (content instanceof String) {
                    String shown = Alphabet.forDisplay((String) content);
                    if (!shown.equals(content)) {
                        text.setText(shown);
                    }
                }
            }
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                apply(group.getChildAt(i));
            }
        }
    }
}
