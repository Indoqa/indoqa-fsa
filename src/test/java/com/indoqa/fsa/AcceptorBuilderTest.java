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
import java.util.stream.Collectors;

import org.junit.Test;

public class AcceptorBuilderTest {

    private static final int STRING_COUNT = 10000;

    private static Acceptor createAcceptor() {
        AcceptorBuilder builder = new AcceptorBuilder(true);
        builder.addAcceptedInput("Wien ");
        builder.addAcceptedInput("Salzburg ");
        builder.addAcceptedInput("Wels ");
        builder.addAcceptedInput("Wien Umgebung ");
        builder.addAcceptedInput("Salzburg Land ");
        builder.addAcceptedInput("Wels Umgebung ");
        return builder.build();
    }

    @Test
    public void getLongestMatch() {
        Acceptor acceptor = createAcceptor();
        assertEquals("Wien ", acceptor.getLongestMatch("Wien 12345 AB"));
        assertEquals("Wien Umgebung ", acceptor.getLongestMatch("Wien Umgebung 12345 AB"));
        assertEquals("Salzburg ", acceptor.getLongestMatch("Salzburg Landtag 12345 AB"));
    }

    @Test
    public void getAllMatches() {
        Acceptor acceptor = createAcceptor();
        String[] allMatches = acceptor.getAllMatches("Wien Salzburg Linz Graz");
        assertEquals(2, allMatches.length);
    }

    @Test
    public void getTokens() {
        AcceptorBuilder builder = new AcceptorBuilder(true);
        builder.addAcceptedInput("Auto");
        builder.addAcceptedInput("Autobahn");
        builder.addAcceptedInput("links");
        builder.addAcceptedInput("schneller");
        builder.addAcceptedInput("schnell");
        builder.addAcceptedInput("fährt");
        builder.addAcceptedInput("fahr");
        builder.addAcceptedInput("gefahren");
        Acceptor acceptor = builder.build();

        List<Token> result = acceptor.getTokens("Auf der Autobahn fährt man ganz links meist schneller.");
        List<String> values = result.stream().map(Token::getValue).collect(Collectors.toList());
        assertTrue(values.contains("Autobahn"));
        assertTrue(values.contains("fährt"));
        assertTrue(values.contains("links"));
        assertTrue(values.contains("schneller"));
        assertEquals(4, result.size());
    }

    @Test
    public void random() {
        Set<String> inputs = TestUtils.generateRandomStrings(STRING_COUNT);
        Acceptor acceptor = AcceptorBuilder.build(true, inputs);

        for (int i = 0; i < 1000; i++) {
            for (String eachInput : inputs) {
                assertTrue("Original input string should be accepted.", acceptor.accepts(eachInput));
            }
        }

        Set<String> otherInputs = TestUtils.generateRandomStrings(STRING_COUNT);
        for (String eachOtherInput : otherInputs) {
            assertEquals(
                "Random input should only be accepted if it was part of the original input.",
                inputs.contains(eachOtherInput),
                acceptor.accepts(eachOtherInput)
            );
        }
    }
}
