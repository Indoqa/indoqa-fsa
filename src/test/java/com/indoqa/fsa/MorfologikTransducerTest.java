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

import static com.indoqa.fsa.TestUtils.generateRandomStrings;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MorfologikTransducerTest {

    private static final int STRING_COUNT = 10000;

    @Test
    public void caseInsensitive() {
        Transducer transducer = MorfologikTransducerBuilder.build('#', false, "Nachteilzug#Nacht|eil|zug");

        assertEquals("Nacht|eil|zug", transducer.transduce("Nachteilzug"));
    }

    @Test
    public void test() {
        List<String> inputs = new ArrayList<>(generateRandomStrings(STRING_COUNT));
        List<String> outputs = new ArrayList<>(generateRandomStrings(STRING_COUNT));

        MorfologikTransducerBuilder transducerBuilder = new MorfologikTransducerBuilder('|');
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

            assertEquals("Random input should only be translated if it was part of the original input.", expected,
                transducer.transduce(eachOtherInput, eachOtherInput));
        }
    }

    @Test
    public void test2() {
        MorfologikTransducerBuilder builder = new MorfologikTransducerBuilder('|');
        builder.add("29.02.", "2016-02-09");
        builder.add("29.02.2016", "2016-02-09");
        builder.add("29.02. 2016", "2016-02-09");
        Transducer transducer = builder.build();

        List<Token> tokens = transducer.getTransducedTokens("am 29.02. beginnt");
        assertEquals(1, tokens.size());
    }

    @Test
    public void test3() {
        MorfologikTransducerBuilder builder = new MorfologikTransducerBuilder('|');
        builder.add("ABC", "CBA");
        Transducer transducer = builder.build();

        List<Token> tokens = transducer.getTransducedTokens("ABC");
        assertEquals(1, tokens.size());

        tokens = transducer.getTransducedTokens("ABC ");
        assertEquals(1, tokens.size());

        tokens = transducer.getTransducedTokens(" ABC ");
        assertEquals(1, tokens.size());
    }

    @Test
    public void transduce() {
        Transducer transducer = CharTransducerBuilder.build('#', false, "Auto#PKW");

        assertEquals("Autobahn", transducer.transduce("Autobahn"));
        assertEquals("PKW", transducer.transduce("Auto"));
    }
}
