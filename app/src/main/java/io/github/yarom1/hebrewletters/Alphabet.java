package io.github.yarom1.hebrewletters;

/**
 * The 22 letters of the Hebrew alphabet, their names and example words.
 *
 * <p>Names and words are written with niqqud (vowel points). Children who are starting to read
 * learn with niqqud, and it also tells the phone's text-to-speech engine exactly how to pronounce
 * each word (for example אָלֶף "alef" and not אֶלֶף "elef").
 *
 * <p>Words use full spelling (כתיב מלא) plus niqqud, so that even an engine that ignores the
 * vowel points still sees the everyday spelling. Every word starts with its letter; a unit test
 * checks this.
 */
final class Alphabet {

    /** Variation selector that asks for the colourful emoji form of a symbol, e.g. ☀ → ☀️. */
    private static final String VS = "️";

    static final class Word {
        /** The word with niqqud, as shown and as spoken. */
        final String text;
        /** A picture of the word. */
        final String emoji;

        Word(String text, String emoji) {
            this.text = text;
            this.emoji = emoji;
        }
    }

    static final class Letter {
        /** The letter itself, e.g. "א". */
        final String glyph;
        /** The letter's name with niqqud, e.g. "אָלֶף". */
        final String name;
        /** Words that start with this letter, the most familiar first. */
        final Word[] words;

        Letter(String glyph, String name, Word... words) {
            this.glyph = glyph;
            this.name = name;
            this.words = words;
        }
    }

    private static Word w(String text, String emoji) {
        return new Word(text, emoji);
    }

    private static final Letter[] LETTERS = {
        new Letter("א", "אָלֶף",
                w("אַרְיֵה", "🦁"), w("אֲבַטִּיחַ", "🍉"), w("אַרְנָב", "🐰"),
                w("אֲנָנָס", "🍍"), w("אוֹטוֹבּוּס", "🚌")),
        new Letter("ב", "בֵּית",
                w("בָּנָנָה", "🍌"), w("בַּיִת", "🏠"), w("בַּלּוֹן", "🎈"),
                w("בֵּיצָה", "🥚"), w("בַּקְבּוּק", "🍼")),
        new Letter("ג", "גִּימֶל",
                w("גָּמָל", "🐫"), w("גְּלִידָה", "🍦"), w("גֶּזֶר", "🥕"),
                w("גִּיטָרָה", "🎸"), w("גֶּשֶׁם", "🌧" + VS)),
        new Letter("ד", "דָּלֶת",
                w("דָּג", "🐟"), w("דּוֹב", "🐻"), w("דְּבוֹרָה", "🐝"),
                w("דּוֹלְפִין", "🐬"), w("דּוּבְדְּבָן", "🍒")),
        new Letter("ה", "הֵא",
                w("הַר", "⛰" + VS), w("הֶלִיקוֹפְּטֶר", "🚁"), w("הַמְבּוּרְגֶּר", "🍔"),
                w("הִיפּוֹפּוֹטָם", "🦛")),
        new Letter("ו", "וָו",
                w("וֶרֶד", "🌹"), w("וָפֶל", "🧇"), w("וָו", "🪝")),
        new Letter("ז", "זַיִן",
                w("זֶבְּרָה", "🦓"), w("זְאֵב", "🐺"), w("זַחַל", "🐛"),
                w("זִיקּוּקִים", "🎆"), w("זַיִת", "🫒")),
        new Letter("ח", "חֵית",
                w("חָתוּל", "🐱"), w("חִילָּזוֹן", "🐌"), w("חוּלְצָה", "👕"),
                w("חַמָּנִיָּה", "🌻"), w("חָלָב", "🥛")),
        new Letter("ט", "טֵית",
                w("טֶלֶפוֹן", "☎" + VS), w("טְרַקְטוֹר", "🚜"), w("טִירָה", "🏰"),
                w("טִיפָּה", "💧"), w("טַבַּעַת", "💍")),
        new Letter("י", "יוֹד",
                w("יַנְשׁוּף", "🦉"), w("יָרֵחַ", "🌙"), w("יָד", "✋"),
                w("יוֹנָה", "🕊" + VS), w("יָם", "🌊")),
        new Letter("כ", "כַּף",
                w("כֶּלֶב", "🐶"), w("כַּדּוּר", "⚽"), w("כּוֹכָב", "⭐"),
                w("כֶּתֶר", "👑"), w("כִּבְשָׂה", "🐑")),
        new Letter("ל", "לָמֶד",
                w("לִימוֹן", "🍋"), w("לֵב", "❤" + VS), w("לֶחֶם", "🍞"),
                w("לְטָאָה", "🦎"), w("לֵיצָן", "🤡")),
        new Letter("מ", "מֵם",
                w("מְכוֹנִית", "🚗"), w("מִטְרִיָּה", "☔"), w("מְלָפְפוֹן", "🥒"),
                w("מַפְתֵּחַ", "🔑"), w("מַתָּנָה", "🎁")),
        new Letter("נ", "נוּן",
                w("נָחָשׁ", "🐍"), w("נֵר", "🕯" + VS), w("נְמָלָה", "🐜"),
                w("נָמֵר", "🐯"), w("נְסִיכָה", "👸")),
        new Letter("ס", "סָמֶךְ",
                w("סוּס", "🐴"), w("סֵפֶר", "📖"), w("סַבְתָּא", "👵"),
                w("סִירָה", "⛵"), w("סוּכָּרִיָּה", "🍭")),
        new Letter("ע", "עַיִן",
                w("עֵץ", "🌳"), w("עוּגָה", "🎂"), w("עַכְבָּר", "🐭"),
                w("עֲנָבִים", "🍇"), w("עַגְבָנִיָּה", "🍅"), w("עִיפָּרוֹן", "✏" + VS)),
        new Letter("פ", "פֵּא",
                w("פִּיל", "🐘"), w("פֶּרַח", "🌸"), w("פִּיצָה", "🍕"),
                w("פַּרְפַּר", "🦋"), w("פָּרָה", "🐄"), w("פַּטִּישׁ", "🔨")),
        new Letter("צ", "צָדִי",
                w("צָב", "🐢"), w("צִיפּוֹר", "🐦"), w("צְפַרְדֵּעַ", "🐸"),
                w("צֶבַע", "🎨"), w("צַלַּחַת", "🍽" + VS)),
        new Letter("ק", "קוֹף",
                w("קוֹף", "🐒"), w("קִיפּוֹד", "🦔"), w("קֶשֶׁת", "🌈"),
                w("קַרְנַף", "🦏"), w("קַקְטוּס", "🌵")),
        new Letter("ר", "רֵישׁ",
                w("רַכֶּבֶת", "🚂"), w("רוֹבּוֹט", "🤖"), w("רָקֶטָה", "🚀"),
                w("רַמְזוֹר", "🚦"), w("רַדְיוֹ", "📻")),
        new Letter("ש", "שִׁין",
                w("שֶׁמֶשׁ", "☀" + VS), w("שׁוֹקוֹלָד", "🍫"), w("שָׁעוֹן", "⏰"),
                w("שֶׁלֶג", "❄" + VS), w("שֵׁן", "🦷"), w("שׁוּעָל", "🦊")),
        new Letter("ת", "תָּו",
                w("תַּפּוּחַ", "🍎"), w("תּוּת", "🍓"), w("תַּרְנְגוֹל", "🐓"),
                w("תִּינוֹק", "👶"), w("תַּנִּין", "🐊"), w("תְּמָנוּן", "🐙")),
    };

    private Alphabet() {}

    static int count() {
        return LETTERS.length;
    }

    /** Returns letter {@code index}, wrapping around at both ends (after ת comes א again). */
    static Letter get(int index) {
        return LETTERS[wrap(index)];
    }

    static int wrap(int index) {
        int n = LETTERS.length;
        return ((index % n) + n) % n;
    }

    /** Vav followed by holam (holam male), as in קוֹף. */
    private static final String VAV_HOLAM = "\u05D5\u05B9";
    /** The same as one character, "HEBREW LETTER VAV WITH HOLAM". */
    private static final String VAV_HOLAM_PRECOMPOSED = "\uFB4B";

    /**
     * Prepares text with niqqud for the screen. A holam on a vav is drawn by many fonts (including
     * ours) beside the top of the vav, where children and parents easily mistake it for a shuruk
     * (וּ). The precomposed character puts the dot clearly above the vav on every phone. Speech
     * keeps using the ordinary spelling.
     */
    static String forDisplay(String text) {
        return text.indexOf(VAV_HOLAM) < 0 ? text : text.replace(VAV_HOLAM, VAV_HOLAM_PRECOMPOSED);
    }

    /** Removes niqqud and cantillation marks, e.g. "אַרְיֵה" → "אריה". */
    static String withoutNiqqud(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\uFB4B') {
                out.append('ו');
            } else if (!isHebrewMark(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** True for Hebrew points (U+0591–U+05C7) that attach to a letter, but not for maqaf etc. */
    static boolean isHebrewMark(char c) {
        return (c >= '֑' && c <= 'ֽ') || c == 'ֿ' || c == 'ׁ' || c == 'ׂ'
                || c == 'ׄ' || c == 'ׅ' || c == 'ׇ';
    }
}
