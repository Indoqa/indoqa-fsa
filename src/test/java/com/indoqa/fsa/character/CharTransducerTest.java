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

import static com.indoqa.fsa.TestUtils.generateRandomStrings;
import static org.junit.Assert.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import com.indoqa.fsa.TestUtils;
import com.indoqa.fsa.Token;
import com.indoqa.fsa.Transducer;
import com.indoqa.fsa.TransducerBuilder;

public class CharTransducerTest {

    private static final int STRING_COUNT = 10000;

    @Test
    public void caseInsensitive() {
        Transducer transducer = CharTransducerBuilder.build(false, "#", "Nachteilzug#Nacht|eil|zug");

        assertEquals("Nacht|eil|zug", transducer.transduce("Nachteilzug"));
    }

    @Test
    public void completionsWithCustomSeparator() {
        NavigableSet<String> inputs = new TreeSet<>(Arrays.asList("a", "aa", "aaa", "aaaa", "ab", "abc"));

        Function<String, String> makeCompletion = input -> "A_" + input + "_B";

        CharTransducerBuilder transducerBuilder = new CharTransducerBuilder(true, (char) 0);
        for (String eachInput : inputs) {
            transducerBuilder.add(eachInput, makeCompletion.apply(eachInput));
        }
        CharTransducer transducer = transducerBuilder.build();

        int length = 3;
        int maxCount = 3;

        for (String eachInput : inputs) {
            String substring = eachInput.substring(0, Math.min(eachInput.length(), length));

            List<String> expected = new ArrayList<>();
            for (String eachValue : inputs.tailSet(substring, true)) {
                if (expected.size() == maxCount || !eachValue.startsWith(substring)) {
                    break;
                }

                expected.add(makeCompletion.apply(eachValue));
            }

            List<String> actual = transducer.getCompletions(substring, maxCount)
                .stream()
                .map(Token::getValue)
                .collect(Collectors.toList());

            assertEquals(expected.size(), actual.size());
            assertTrue(expected.containsAll(actual));
        }
    }

    @Test
    public void completionsWithDefaultSeparator() {
        NavigableSet<String> inputs = new TreeSet<>(Arrays.asList("a", "aa", "aaa", "aaaa", "ab", "abc"));

        Function<String, String> makeCompletion = input -> "A_" + input + "_B";

        CharTransducerBuilder transducerBuilder = new CharTransducerBuilder(true);
        for (String eachInput : inputs) {
            transducerBuilder.add(eachInput, makeCompletion.apply(eachInput));
        }
        CharTransducer transducer = transducerBuilder.build();

        int length = 3;
        int maxCount = 3;

        for (String eachInput : inputs) {
            String substring = eachInput.substring(0, Math.min(eachInput.length(), length));

            List<String> expected = new ArrayList<>();
            for (String eachValue : inputs.tailSet(substring, true)) {
                if (expected.size() == maxCount || !eachValue.startsWith(substring)) {
                    break;
                }

                expected.add(makeCompletion.apply(eachValue));
            }

            List<String> actual = transducer.getCompletions(substring, maxCount)
                .stream()
                .map(Token::getValue)
                .collect(Collectors.toList());

            assertEquals(expected.size(), actual.size());
            assertTrue(expected.containsAll(actual));
        }
    }

    @Test
    public void getAll1() {
        TransducerBuilder builder = new CharTransducerBuilder(true);
        builder.add("A", "A1");
        builder.add("AA", "A2");
        builder.add("AAA", "A3");
        builder.add("AAAA", "A4");
        Transducer transducer = builder.build();

        String sequence = "AAAA";
        assertEquals(4, transducer.getAllMatches(sequence).size());
        assertEquals(1, transducer.getAllTokens(sequence).size());
        assertEquals(4 + 3 + 2 + 1, transducer.getAllOccurrences(sequence).size());
    }

    @Test
    public void getAll2() {
        TransducerBuilder builder = new CharTransducerBuilder(false);
        builder.add("Auto", "A");
        Transducer transducer = builder.build();

        String sequence = "Rennauto Auto Automatik";
        assertEquals(0, transducer.getAllMatches(sequence).size());
        assertEquals(1, transducer.getAllTokens(sequence).size());
        assertEquals(3, transducer.getAllOccurrences(sequence).size());
    }

    @Test
    public void getAllMatches() {
        TransducerBuilder builder = new CharTransducerBuilder(true);
        builder.add("29.02.", "2016-02-09");
        builder.add("29.02.2016", "2016-02-09");
        builder.add("29.02. 2016", "2016-02-09");
        Transducer transducer = builder.build();

        assertEquals(1, transducer.getAllTokens("am 29.02. beginnt").size());
    }

    @Test
    public void getLongestMatch() {
        CharTransducer transducer = CharTransducerBuilder.build(false, "\\|", "a|b", "aa|bb", "aaa|bbb");

        Token longestMatch = transducer.getLongestMatch("aaaa");
        assertEquals("aaa", longestMatch.getOriginal());
        assertEquals("bbb", longestMatch.getValue());
    }

    @Test
    public void iterate() {
        String[] originals = {"a", "aa", "ab", "abcd", "b", "bb", "c", "d", "da", "dc", "dcccc", "dd"};
        String[] values = {"1", "11", "12", "1234", "2", "22", "3", "4", "41", "43", "43333", "44"};

        CharTransducerBuilder builder = new CharTransducerBuilder(true);
        for (int i = 0; i < originals.length; i++) {
            builder.add(originals[i], values[i]);
        }
        CharTransducer transducer = builder.build();

        Iterator<Token> iterator = transducer.iterator();
        for (int i = 0; i < originals.length; i++) {
            assertTrue(iterator.hasNext());
            Token token = iterator.next();
            assertEquals(originals[i], token.getOriginal());
            assertEquals(values[i], token.getValue());
        }

        assertFalse(iterator.hasNext());
    }

    @Test
    public void random() {
        List<String> inputs = new ArrayList<>(generateRandomStrings(STRING_COUNT));
        List<String> outputs = new ArrayList<>(generateRandomStrings(STRING_COUNT));

        TransducerBuilder transducerBuilder = new CharTransducerBuilder(true);
        for (int i = 0; i < inputs.size(); i++) {
            transducerBuilder.add(inputs.get(i), outputs.get(i));
        }
        Transducer transducer = transducerBuilder.build();

        for (int i = 0; i < inputs.size(); i++) {
            assertEquals("Original input was not translated to expected output.", outputs.get(i), transducer.transduce(inputs.get(i)));
        }

        List<String> otherInputs = new ArrayList<>(generateRandomStrings(STRING_COUNT));
        for (String eachOtherInput : otherInputs) {
            int index = inputs.indexOf(eachOtherInput);
            String expected = index == -1 ? eachOtherInput : outputs.get(index);

            assertEquals(
                "Random input should only be translated if it was part of the original input.",
                expected,
                transducer.transduce(eachOtherInput, eachOtherInput));
        }
    }

    @Test
    public void randomCompletions() {
        NavigableSet<String> inputs = new TreeSet<>(TestUtils.generateRandomStrings(STRING_COUNT));

        Function<String, String> makeCompletion = input -> "A_" + input + "_B";

        CharTransducerBuilder transducerBuilder = new CharTransducerBuilder(true);
        for (String eachInput : inputs) {
            transducerBuilder.add(eachInput, makeCompletion.apply(eachInput));
        }
        CharTransducer transducer = transducerBuilder.build();

        int length = 3;
        int maxCount = 3;

        for (String eachInput : inputs) {
            String substring = eachInput.substring(0, length);

            List<String> expected = new ArrayList<>();
            for (String eachValue : inputs.tailSet(substring, true)) {
                if (expected.size() == maxCount || !eachValue.startsWith(substring)) {
                    break;
                }

                expected.add(makeCompletion.apply(eachValue));
            }

            List<String> actual = transducer.getCompletions(substring, maxCount)
                .stream()
                .map(Token::getValue)
                .collect(Collectors.toList());

            assertEquals(expected.size(), actual.size());
            assertTrue(expected.containsAll(actual));
        }
    }

    @Test
    public void test3() {
        TransducerBuilder builder = new CharTransducerBuilder(true);
        builder.add("ABC", "CBA");
        Transducer transducer = builder.build();

        List<Token> tokens = transducer.getAllTokens("ABC");
        assertEquals(1, tokens.size());

        tokens = transducer.getAllTokens("ABC ");
        assertEquals(1, tokens.size());

        tokens = transducer.getAllTokens(" ABC ");
        assertEquals(1, tokens.size());
    }

    @Test
    public void test4() {
        CharTransducerBuilder builder = new CharTransducerBuilder(true);
        builder.add(Arrays.asList("A", "B", "C", "D", "F", "G"), "A");
        Transducer transducer = builder.build();

        List<Token> tokens = transducer.getAllOccurrences("ABCDEFG");
        assertEquals(6, tokens.size());
        assertTrue(tokens.stream().map(Token::getValue).allMatch(value -> value.equals("A")));
    }

    @Test
    public void transduce() {
        Transducer transducer = CharTransducerBuilder.build(false, "#", "Auto#PKW");

        assertEquals("Autobahn", transducer.transduce("Autobahn"));
        assertEquals("PKW", transducer.transduce("Auto"));
    }
}
