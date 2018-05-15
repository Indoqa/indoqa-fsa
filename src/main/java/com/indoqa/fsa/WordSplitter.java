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

import java.util.ArrayList;
import java.util.List;

public class WordSplitter {

    private static final Interfix[] INTERFIXES = {new Interfix("s"), new Interfix("s-"), new Interfix("-"), new Interfix("es"),
        new Interfix("es-"), new Interfix("i"), new Interfix("i-"), new Interfix("o"), new Interfix("o-")};

    private final Acceptor wordsAcceptor;
    private final Acceptor prefixesAcceptor;
    private final Transducer specialsTransducer;
    private int minimumWordLength = 2;

    public WordSplitter(Acceptor wordAcceptor) {
        this(wordAcceptor, null);
    }

    public WordSplitter(Acceptor wordAcceptor, Acceptor prefixAcceptor) {
        this(wordAcceptor, prefixAcceptor, null);
    }

    public WordSplitter(Acceptor wordsAcceptor, Acceptor prefixesAcceptor, Transducer specialsTransducer) {
        super();

        this.wordsAcceptor = wordsAcceptor;

        if (prefixesAcceptor == null) {
            this.prefixesAcceptor = MorfologikAcceptorBuilder.build(true);
        } else {
            this.prefixesAcceptor = prefixesAcceptor;
        }

        if (specialsTransducer == null) {
            this.specialsTransducer = TransducerBuilder.build('|', true);
        } else {
            this.specialsTransducer = specialsTransducer;
        }
    }

    private static <T> List<T> list(T value1) {
        List<T> result = new ArrayList<>();
        result.add(value1);
        return result;
    }

    private static String removeTrailingInterfix(String word) {
        for (Interfix eachInfix : INTERFIXES) {
            if (eachInfix.matches(word)) {
                return eachInfix.subtractFrom(word);
            }
        }

        return null;
    }

    public List<Token> getTokens(String word) {
        return this.splitWord(word, Token::create);
    }

    public List<Integer> getTokenStarts(String word) {
        return this.splitWord(word, (position, part) -> position);
    }

    public void setMinimumWordLength(int minimumWordLength) {
        this.minimumWordLength = minimumWordLength;
    }

    private <T> List<T> splitKnownOrSpecial(String word, int offset, Creator<T> creator) {
        if (this.wordsAcceptor.accepts(word) || this.prefixesAcceptor.accepts(word)) {
            return list(creator.create(offset, word));
        }

        return this.splitSpecial(word, creator);
    }

    private <T> List<T> splitSpecial(String word, Creator<T> creator) {
        if (this.specialsTransducer == null) {
            return null;
        }

        String transduce = this.specialsTransducer.transduce(word);
        if (transduce == null) {
            return null;
        }

        List<T> result = new ArrayList<>();

        int start = 0;
        for (int i = 0; i < transduce.length(); i++) {
            if (transduce.charAt(i) == '|') {
                result.add(creator.create(start - result.size(), transduce.substring(start, i)));
                start = i + 1;
            }
        }

        result.add(creator.create(start - result.size(), transduce.substring(start)));

        return result;
    }

    private <T> List<T> splitWord(String word, Creator<T> creator) {
        List<T> result = this.splitKnownOrSpecial(word, 0, creator);
        if (result != null) {
            return result;
        }

        for (int i = this.minimumWordLength; i < word.length() - this.minimumWordLength; i++) {
            String rightPart = word.substring(i);

            result = this.splitKnownOrSpecial(rightPart, i, creator);
            if (result == null) {
                continue;
            }

            String leftPart = word.substring(0, i);

            List<T> leftResult = this.splitWord(leftPart, creator);
            if (leftResult == null) {
                leftPart = removeTrailingInterfix(leftPart);
                if (leftPart == null) {
                    continue;
                }

                leftResult = this.splitWord(leftPart, creator);
                if (leftResult == null) {
                    continue;
                }
            }

            leftResult.addAll(result);
            return leftResult;
        }

        return null;
    }

    @FunctionalInterface
    private static interface Creator<T> {

        T create(int position, String part);
    }

    private static class Interfix {

        private final String value;

        public Interfix(String value) {
            super();

            this.value = value;
        }

        public boolean matches(String word) {
            return word.endsWith(this.value);
        }

        public String subtractFrom(String word) {
            return word.substring(0, word.length() - this.value.length());
        }
    }
}
