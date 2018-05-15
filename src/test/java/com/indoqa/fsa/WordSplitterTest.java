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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class WordSplitterTest {

    private static void assertEquals(List<Token> tokens, String... values) {
        assertNotNull("Tokens were null.", tokens);

        String[] tokenValues = tokens
            .stream()
            .map(Token::getValue)
            .map(value -> value.toLowerCase(Locale.ROOT))
            .toArray(String[]::new);
        assertArrayEquals("Expected " + Arrays.toString(values) + " but received " + Arrays.toString(tokenValues), values,
            tokenValues);
    }

    @Test
    public void test() {
        Acceptor prefixAcceptor = MorfologikAcceptorBuilder.build(false, "an", "ab", "auf", "zu", "aus");
        Acceptor wordAcceptor = MorfologikAcceptorBuilder.build(false, "Gespräch", "einstellung", "verkäufer", "baum", "weihnacht");
        WordSplitter wordSplitter = new WordSplitter(wordAcceptor, prefixAcceptor);

        assertEquals(wordSplitter.getTokens("Weihnachtsbaumverkäufer-einstellungsgespräch"), "weihnacht", "baum", "verkäufer",
            "einstellung", "gespräch");
    }

    @Test
    public void test1() {
        Acceptor prefixAcceptor = MorfologikAcceptorBuilder.build(false);
        Acceptor wordAcceptor = MorfologikAcceptorBuilder.build(false, "donau", "dampf", "schiff", "fahrt", "farbe", "farben",
            "gesellschaft", "kapitän", "tür", "kajüte", "kajüten");
        WordSplitter wordSplitter = new WordSplitter(wordAcceptor, prefixAcceptor);

        assertEquals(wordSplitter.getTokens("Donaudampfschifffahrtsgesellschaftskapitänskajütentürfarben"), "donau", "dampf", "schiff",
            "fahrt", "gesellschaft", "kapitän", "kajüten", "tür", "farben");
    }

    @Test
    public void test2() {
        Acceptor prefixAcceptor = MorfologikAcceptorBuilder.build(false, "an");
        Acceptor wordAcceptor = MorfologikAcceptorBuilder.build(false, "ober", "gen", "darm", "gendarm", "anwärter", "wärter");
        WordSplitter wordSplitter = new WordSplitter(wordAcceptor, prefixAcceptor);

        assertEquals(wordSplitter.getTokens("Gendarmanwärter"), "gendarm", "anwärter");
        assertEquals(wordSplitter.getTokens("Obergendarm"), "ober", "gendarm");
        assertEquals(wordSplitter.getTokens("Obergendarmanwärter"), "ober", "gendarm", "anwärter");
    }

    @Test
    public void test3() {
        Acceptor prefixAcceptor = MorfologikAcceptorBuilder.build(true);
        Acceptor wordAcceptor = MorfologikAcceptorBuilder.build(true, "nach", "ober", "high", "speed", "teil", "nachteil", "sonder",
            "zug", "schaffner", "gen", "darm", "gendarm", "armlänge", "länge", "arm");
        Transducer specialTransducer = TransducerBuilder.build('#', true, "nachteilzug#nacht|eil|zug");
        WordSplitter wordSplitter = new WordSplitter(wordAcceptor, prefixAcceptor, specialTransducer);

        assertEquals(wordSplitter.getTokens("nachteilzug"), "nacht", "eil", "zug");
        assertEquals(wordSplitter.getTokens("sondernachteilzug"), "sonder", "nacht", "eil", "zug");
        assertEquals(wordSplitter.getTokens("highspeednachteilzug"), "high", "speed", "nacht", "eil", "zug");
        assertEquals(wordSplitter.getTokens("nachteilzugobergendarmlänge"), "nacht", "eil", "zug", "ober", "gendarm", "länge");
    }
}
