package io.github.yarom1.hebrewletters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** Checks the letter and word data. Plain JUnit, no Android needed. */
public class AlphabetTest {

    private static final String ALEF_BET = "אבגדהוזחטיכלמנסעפצקרשת";

    @Test
    public void hasTheTwentyTwoLettersInOrder() {
        assertEquals(22, Alphabet.count());
        for (int i = 0; i < Alphabet.count(); i++) {
            assertEquals(String.valueOf(ALEF_BET.charAt(i)), Alphabet.get(i).glyph);
        }
    }

    @Test
    public void everyWordStartsWithItsLetter() {
        for (int i = 0; i < Alphabet.count(); i++) {
            Alphabet.Letter letter = Alphabet.get(i);
            for (Alphabet.Word word : letter.words) {
                String plain = Alphabet.withoutNiqqud(word.text);
                assertTrue(word.text + " should start with " + letter.glyph,
                        plain.startsWith(letter.glyph));
            }
        }
    }

    @Test
    public void everyLetterHasSeveralWordsWithPictures() {
        for (int i = 0; i < Alphabet.count(); i++) {
            Alphabet.Letter letter = Alphabet.get(i);
            assertTrue(letter.glyph + " needs at least 3 words", letter.words.length >= 3);
            Set<String> pictures = new HashSet<>();
            for (Alphabet.Word word : letter.words) {
                assertFalse(word.emoji.isEmpty());
                assertTrue("duplicate picture for " + word.text, pictures.add(word.emoji));
            }
        }
    }

    @Test
    public void namesAndWordsCarryNiqqud() {
        for (int i = 0; i < Alphabet.count(); i++) {
            Alphabet.Letter letter = Alphabet.get(i);
            assertNotEquals(letter.name, Alphabet.withoutNiqqud(letter.name));
            assertTrue(Alphabet.withoutNiqqud(letter.name).startsWith(letter.glyph));
            for (Alphabet.Word word : letter.words) {
                assertNotEquals(word.text, Alphabet.withoutNiqqud(word.text));
            }
        }
    }

    @Test
    public void pictureIsNotReusedForAnotherWord() {
        Set<String> pictures = new HashSet<>();
        for (int i = 0; i < Alphabet.count(); i++) {
            for (Alphabet.Word word : Alphabet.get(i).words) {
                assertTrue("picture used twice: " + word.emoji + " " + word.text,
                        pictures.add(word.emoji));
            }
        }
    }

    @Test
    public void indexesWrapAround() {
        assertEquals("ת", Alphabet.get(-1).glyph);
        assertEquals("א", Alphabet.get(22).glyph);
        assertEquals(0, Alphabet.wrap(44));
    }

    @Test
    public void stripsNiqqudButKeepsLetters() {
        assertEquals("אריה", Alphabet.withoutNiqqud("אַרְיֵה"));
        assertEquals("שוקולד", Alphabet.withoutNiqqud("שׁוֹקוֹלָד"));
        assertEquals("אלף־בית", Alphabet.withoutNiqqud("אָלֶף־בֵּית"));
    }
}
