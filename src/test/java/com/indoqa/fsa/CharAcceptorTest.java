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
package com.indoqa.fsa;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class CharAcceptorTest {

    private static final int STRING_COUNT = 10000;

    private static String[] getValues(List<Token> tokens) {
        return tokens.stream().map(Token::getValue).toArray(String[]::new);
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

        assertArrayEquals(new String[] {"ar ei", "lang", "langsam", "langsam fahrender", "fahrender Personenkraftwagen", "Person",
            "Personenkraftwagen mit Anhängerkupplung"}, getValues(acceptor.getAllOccurrences(sequence)));

        assertArrayEquals(new String[] {"ar ei", "langsam fahrender", "Personenkraftwagen mit Anhängerkupplung"},
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

        assertArrayEquals(new String[] {"langsam fahrender", "Personenkraftwagen mit Anhängerkupplung"},
            getValues(acceptor.getLongestTokens(sequence)));
    }

    @Test
    public void random() {
        Set<String> inputs = TestUtils.generateRandomStrings(STRING_COUNT);
        CharAcceptor acceptor = CharAcceptorBuilder.build(true, inputs);

        for (int i = 0; i < 1_000; i++) {
            for (String eachInput : inputs) {
                assertTrue("Original input string should be accepted.", acceptor.accepts(eachInput));
            }
        }

        Set<String> otherInputs = TestUtils.generateRandomStrings(STRING_COUNT);
        for (String eachOtherInput : otherInputs) {
            assertEquals("Random input should only be accepted if it was part of the original input.", inputs.contains(eachOtherInput),
                acceptor.accepts(eachOtherInput));
        }
    }
}
