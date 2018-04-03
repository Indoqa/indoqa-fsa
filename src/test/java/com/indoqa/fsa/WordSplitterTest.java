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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WordSplitterTest {

    @Test
    public void test() {
        Acceptor prefixAcceptor = AcceptorBuilder.build(false, "an", "ab", "auf", "zu", "aus");
        Acceptor wordAcceptor = AcceptorBuilder.build(false, "Gespräch", "einstellung", "verkäufer", "baum", "weihnacht");
        WordSplitter wordSplitter = new WordSplitter(wordAcceptor, prefixAcceptor);

        Word word = wordSplitter.split("Weihnachtsbaumverkäufer-einstellungsgespräch");
        assertEquals("gespräch", word.getRight().getValue());
        assertEquals("Weihnachtsbaumverkäufer-einstellung", word.getLeft().getValue());

        word = wordSplitter.split(word.getLeft().getValue());
        assertEquals("einstellung", word.getRight().getValue());
        assertEquals("Weihnachtsbaumverkäufer", word.getLeft().getValue());

        word = wordSplitter.split(word.getLeft().getValue());
        assertEquals("verkäufer", word.getRight().getValue());
        assertEquals("Weihnachtsbaum", word.getLeft().getValue());

        word = wordSplitter.split(word.getLeft().getValue());
        assertEquals("baum", word.getRight().getValue());
        assertEquals("Weihnacht", word.getLeft().getValue());
    }
}
