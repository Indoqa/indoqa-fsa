/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indoqa.fsa.character;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import com.indoqa.fsa.Acceptor;
import com.indoqa.fsa.TestUtils;
import com.indoqa.fsa.Token;

public class CharAcceptorTest {

    private static final int CYCLES = 10;
    private static final int STRING_COUNT = 50_000;

    private static String[] getValues(List<Token> tokens) {
        return tokens.stream().map(Token::getValue).toArray(String[]::new);
    }

    @Test
    public void completionOrderCaseInsensitive() {
        CharAcceptorBuilder builder = new CharAcceptorBuilder(false);
        builder.addAcceptedInput("aca");
        builder.addAcceptedInput("aBa");
        builder.addAcceptedInput("aaa");
        CharAcceptor acceptor = builder.build();

        List<String> completions = acceptor.getCompletions("a", 3);
        assertEquals(3, completions.size());
        assertEquals("aaa", completions.get(0));
        assertEquals("aBa", completions.get(1));
        assertEquals("aca", completions.get(2));
    }

    @Test
    public void completionOrderCaseSensitive() {
        CharAcceptorBuilder builder = new CharAcceptorBuilder(true);
        builder.addAcceptedInput("aca");
        builder.addAcceptedInput("aBa");
        builder.addAcceptedInput("aaa");
        CharAcceptor acceptor = builder.build();

        List<String> completions = acceptor.getCompletions("a", 3);
        assertEquals(3, completions.size());
        assertEquals("aBa", completions.get(0));
        assertEquals("aaa", completions.get(1));
        assertEquals("aca", completions.get(2));
    }

    @Test
    public void getCompletions() {
        CharAcceptorBuilder builder = new CharAcceptorBuilder(false);
        builder.addAcceptedInput("a");
        builder.addAcceptedInput("aa");
        builder.addAcceptedInput("ab");
        builder.addAcceptedInput("abcd");
        builder.addAcceptedInput("b");
        builder.addAcceptedInput("bb");
        builder.addAcceptedInput("c");
        builder.addAcceptedInput("d");
        builder.addAcceptedInput("da");
        builder.addAcceptedInput("dc");
        builder.addAcceptedInput("dcccc");
        builder.addAcceptedInput("dd");
        CharAcceptor charAcceptor = builder.build();

        List<String> completions = charAcceptor.getCompletions("", 10);
        assertEquals("[a, aa, ab, abcd, b, bb, c, d, da, dc]", completions.toString());

        completions = charAcceptor.getCompletions("a", 2);
        assertEquals("[a, aa]", completions.toString());

        completions = charAcceptor.getCompletions("a", 10);
        assertEquals("[a, aa, ab, abcd]", completions.toString());

        completions = charAcceptor.getCompletions("c", 10);
        assertEquals("[c]", completions.toString());

        completions = charAcceptor.getCompletions("dc", 1);
        assertEquals("[dc]", completions.toString());

        completions = charAcceptor.getCompletions("e", 1);
        assertEquals("[]", completions.toString());
    }

    @Test
    public void getMatch() {
        CharAcceptorBuilder builder = new CharAcceptorBuilder(false);
        builder.addAcceptedInput("Wien");
        builder.addAcceptedInput("Salzburg");
        builder.addAcceptedInput("Wels");
        builder.addAcceptedInput("Wien Umgebung");
        builder.addAcceptedInput("Salzburg Land");
        builder.addAcceptedInput("Wels Umgebung");
        CharAcceptor acceptor = builder.build();

        assertEquals(STRING_COUNT, STRING_COUNT);

        String sequence = "Wien";
        assertEquals("Wien", acceptor.getLongestMatch(sequence));
        assertArrayEquals(new String[] {"Wien"}, acceptor.getAllMatches(sequence));

        sequence = "Wien 12345 AB";
        assertEquals("Wien", acceptor.getLongestMatch(sequence));
        assertArrayEquals(new String[] {"Wien"}, acceptor.getAllMatches(sequence));

        sequence = "Wien Umgebung 12345 AB";
        assertEquals("Wien Umgebung", acceptor.getLongestMatch(sequence));
        assertArrayEquals(new String[] {"Wien", "Wien Umgebung"}, acceptor.getAllMatches(sequence));

        sequence = "Salzburg Landtag 12345 AB";
        assertEquals("Salzburg Land", acceptor.getLongestMatch(sequence));
        assertArrayEquals(new String[] {"Salzburg", "Salzburg Land"}, acceptor.getAllMatches(sequence));
    }

    @Test
    public void getOccurrences() {
        CharAcceptorBuilder builder = new CharAcceptorBuilder(false);
        builder.addAcceptedInput("ar ei");
        builder.addAcceptedInput("lang");
        builder.addAcceptedInput("Person");
        builder.addAcceptedInput("langsam");
        builder.addAcceptedInput("langsam fahrender");
        builder.addAcceptedInput("fahrender Personenkraftwagen");
        builder.addAcceptedInput("Personenkraftwagen mit Anhängerkupplung");
        CharAcceptor acceptor = builder.build();
        String sequence = "Da war ein langsam fahrender Personenkraftwagen mit Anhängerkupplung.";

        assertArrayEquals(
            new String[] {
                "ar ei",
                "lang",
                "langsam",
                "langsam fahrender",
                "fahrender Personenkraftwagen",
                "Person",
                "Personenkraftwagen mit Anhängerkupplung"},
            getValues(acceptor.getAllOccurrences(sequence)));

        assertArrayEquals(
            new String[] {"ar ei", "langsam fahrender", "Personenkraftwagen mit Anhängerkupplung"},
            getValues(acceptor.getLongestOccurrences(sequence)));
    }

    @Test
    public void getTokens() {
        CharAcceptorBuilder builder = new CharAcceptorBuilder(false);
        builder.addAcceptedInput("ar ei");
        builder.addAcceptedInput("lang");
        builder.addAcceptedInput("Person");
        builder.addAcceptedInput("langsam");
        builder.addAcceptedInput("langsam fahrender");
        builder.addAcceptedInput("fahrender Personenkraftwagen");
        builder.addAcceptedInput("Personenkraftwagen mit Anhängerkupplung");
        CharAcceptor acceptor = builder.build();
        String sequence = "Da war ein langsam fahrender Personenkraftwagen mit Anhängerkupplung.";

        assertArrayEquals(
            new String[] {"langsam", "langsam fahrender", "fahrender Personenkraftwagen", "Personenkraftwagen mit Anhängerkupplung"},
            getValues(acceptor.getAllTokens(sequence)));

        assertArrayEquals(
            new String[] {"langsam fahrender", "Personenkraftwagen mit Anhängerkupplung"},
            getValues(acceptor.getLongestTokens(sequence)));
    }

    @Test
    public void overlapping() {
        Acceptor acceptor = CharAcceptorBuilder.build(false, "sch", "s");

        assertTrue(acceptor.accepts("s"));
        assertFalse(acceptor.accepts("sc"));
        assertTrue(acceptor.accepts("sch"));
    }

    @Test
    public void overlapping1() {
        Acceptor acceptor = CharAcceptorBuilder.build(false, "be", "bre", "ss");

        assertFalse(acceptor.accepts("sse"));
    }

    @Test
    public void overlapping2() {
        CharAcceptorBuilder builder = new CharAcceptorBuilder(false);

        builder.addAcceptedInput("12");
        builder.addAcceptedInput("22");
        builder.addAcceptedInput("123");

        CharAcceptor acceptor = builder.build();

        assertTrue(acceptor.accepts("12"));
        assertTrue(acceptor.accepts("22"));
        assertTrue(acceptor.accepts("123"));
        assertFalse(acceptor.accepts("223"));
    }

    @Test
    public void randomAccepts() {
        Set<String> inputs = TestUtils.generateRandomStrings(STRING_COUNT);
        CharAcceptor acceptor = CharAcceptorBuilder.build(true, inputs);

        for (int i = 0; i < CYCLES; i++) {
            for (String eachInput : inputs) {
                assertTrue("Original input string must be accepted.", acceptor.accepts(eachInput));
            }
        }

        Set<String> otherInputs = TestUtils.generateRandomStrings(STRING_COUNT);

        for (int i = 0; i < CYCLES; i++) {
            for (String eachOtherInput : otherInputs) {
                assertEquals(
                    "Random input must only be accepted if it was part of the original input.",
                    inputs.contains(eachOtherInput),
                    acceptor.accepts(eachOtherInput));
            }
        }
    }

    @Test
    public void randomCompletions() {
        NavigableSet<String> inputs = new TreeSet<>(TestUtils.generateRandomStrings(STRING_COUNT));
        CharAcceptor acceptor = CharAcceptorBuilder.build(true, inputs);

        int length = 5;
        int maxCount = 3;

        for (String eachInput : inputs) {
            String substring = eachInput.substring(0, Math.min(eachInput.length(), length));

            List<String> expected = new ArrayList<>();
            for (String eachValue : inputs.tailSet(substring, true)) {
                if (expected.size() == maxCount || !eachValue.startsWith(substring)) {
                    break;
                }

                expected.add(eachValue);
            }

            List<String> actual = acceptor.getCompletions(substring, maxCount);

            assertEquals("Number of completions is wrong", expected.size(), actual.size());
            assertTrue("Unexpected completions", expected.containsAll(actual));
        }
    }

    @Test
    public void repeating() {
        Acceptor acceptor = CharAcceptorBuilder.build(false, ".", ",");

        assertArrayEquals(
            new String[] {".", ".", ".", ".", ".", ","},
            acceptor.getAllOccurrences(".....,").stream().map(Token::getOriginal).toArray(String[]::new));

        assertArrayEquals(
            new String[] {".", ".", ".", ".", ".", ","},
            acceptor.getLongestOccurrences(".....,").stream().map(Token::getOriginal).toArray(String[]::new));
    }
}
