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

import java.util.Collections;
import java.util.Locale;

public class WordSplitter {

    private static final Infix[] INFIXES = {new Infix("s", "s"), new Infix("s", "s-"), new Infix("", "-")};

    private final Acceptor wordAcceptor;
    private final Acceptor prefixAcceptor;
    private int minimumWordLength = 2;

    public WordSplitter(Acceptor wordAcceptor) {
        this(wordAcceptor, AcceptorBuilder.build(true, Collections.emptyList()));
    }

    public WordSplitter(Acceptor wordAcceptor, Acceptor prefixAcceptor) {
        super();

        this.wordAcceptor = wordAcceptor;
        this.prefixAcceptor = prefixAcceptor;
    }

    @SafeVarargs
    private static Token[] list(Token... value) {
        return value;
    }

    private static InfixSplit removeTrailingInfix(String word) {
        for (Infix eachInfix : INFIXES) {
            if (eachInfix.matches(word)) {
                return eachInfix.createSplit(word);
            }
        }

        return null;
    }

    public void setMinimumWordLength(int minimumWordLength) {
        this.minimumWordLength = minimumWordLength;
    }

    public Word split(String word) {
        Token[] parts = this.splitWord(word);
        if (parts == null) {
            return new Word(word, null, null);
        }

        if (parts.length == 1) {
            return new Word(word, null, parts[0]);
        }

        if (parts.length == 2) {
            return new Word(word, parts[0], parts[1]);
        }

        throw new IllegalArgumentException("Received more than 2 parts");
    }

    private boolean isKnown(String value) {
        return this.isKnownWord(value) || this.isKnownPrefix(value);
    }

    private boolean isKnownPrefix(String word) {
        return this.prefixAcceptor.accepts(word.toLowerCase(Locale.ROOT));
    }

    private boolean isKnownWord(String word) {
        return this.wordAcceptor.accepts(word.toLowerCase(Locale.ROOT));
    }

    private Token[] splitWord(String word) {
        if (this.isKnownWord(word)) {
            return list(Token.create(0, word));
        }

        for (int i = this.minimumWordLength; i < word.length() - this.minimumWordLength; i++) {
            String leftPart = word.substring(0, i);
            String rightPart = word.substring(i);

            if (!this.isKnownWord(rightPart)) {
                continue;
            }

            if (this.isKnown(leftPart)) {
                return list(Token.create(0, leftPart), Token.create(i, rightPart));
            }

            InfixSplit infixSplit = removeTrailingInfix(leftPart);
            if (infixSplit != null) {
                if (this.isKnown(infixSplit.getWord())) {
                    return list(Token.create(0, infixSplit.getWord()), Token.create(i, rightPart));
                }
            }

            Token[] leftSplit = this.splitWord(leftPart);
            if (leftSplit != null) {
                return list(Token.create(0, leftPart), Token.create(i, rightPart));
            }

            if (infixSplit != null) {
                leftSplit = this.splitWord(infixSplit.getWord());
                if (leftSplit != null) {
                    return list(Token.create(0, infixSplit.getWord()), Token.create(i, rightPart));
                }
            }
        }

        return null;
    }

    protected static class Infix {

        private final String infix;
        private final String ending;

        public Infix(String infix, String ending) {
            super();

            this.infix = infix;
            this.ending = ending;
        }

        public InfixSplit createSplit(String word) {
            InfixSplit result = new InfixSplit();

            result.setInfix(this.infix);
            result.setWord(word.substring(0, word.length() - this.ending.length()));

            return result;
        }

        public boolean matches(String word) {
            return word.endsWith(this.ending);
        }
    }

    protected static class InfixSplit {

        private String word;
        private String infix;

        public String getInfix() {
            return this.infix;
        }

        public String getWord() {
            return this.word;
        }

        public void setInfix(String infix) {
            this.infix = infix;
        }

        public void setWord(String word) {
            this.word = word;
        }
    }
}
