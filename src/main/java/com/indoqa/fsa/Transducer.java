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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.indoqa.fsa.traversal.Result;
import com.indoqa.fsa.traversal.Result.Match;
import com.indoqa.fsa.traversal.TransducerTraversal;
import com.indoqa.fsa.utils.EncodingUtils;

import morfologik.fsa.ByteSequenceIterator;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.WordData;

public class Transducer {

    private final boolean caseSensitive;
    private final TransducerTraversal traversal;
    private final Dictionary dictionary;
    private final DictionaryLookup dictionaryLookup;
    private final ByteSequenceIterator iterator;

    protected Transducer(Dictionary dictionary, boolean caseSensitive) {
        this.dictionary = dictionary;
        this.dictionaryLookup = new DictionaryLookup(dictionary);
        this.traversal = new TransducerTraversal(this.dictionary.fsa, this.dictionary.metadata.getSeparator());
        this.iterator = new ByteSequenceIterator(this.dictionary.fsa, this.dictionary.fsa.getRootNode());
        this.caseSensitive = caseSensitive;
    }

    public String getLongestTransducedMatch(String input) {
        byte[] bytes = this.getBytes(input);
        Result match = new Result();

        this.traversal.match(match, bytes, 0, bytes.length);
        if (match.getMatch() != Match.NON_TERMINAL_MATCH) {
            return null;
        }

        this.iterator.restartFrom(match.getNode());
        ByteBuffer byteBuffer = this.iterator.next();
        String replacement = StandardCharsets.UTF_8.decode(byteBuffer).toString();

        return replacement + EncodingUtils.getString(bytes, match.getMatchedLength(), bytes.length - match.getMatchedLength());
    }

    public List<Token> getTransducedTokens(CharSequence value) {
        List<Token> result = new ArrayList<>();
        Token lastToken = null;

        byte[] bytes = this.getBytes(value);
        Result match = new Result();

        for (int i = 0; i < bytes.length - 1; i++) {
            this.traversal.match(match, bytes, i, bytes.length - i);
            if (match.getMatch() != Match.NON_TERMINAL_MATCH) {
                continue;
            }

            Token token = this.createToken(value, bytes, i, match.getMatchedLength());

            if (token.getEnd() < value.length()) {
                char nextChar = value.charAt(token.getEnd());
                if (Character.isLetterOrDigit(nextChar)) {
                    continue;
                }
            }

            this.iterator.restartFrom(match.getNode());
            ByteBuffer byteBuffer = this.iterator.next();
            token.setValue(StandardCharsets.UTF_8.decode(byteBuffer).toString());

            if (lastToken == null || lastToken.isDisjunct(token)) {
                result.add(token);
                lastToken = token;
                continue;
            }

            if (lastToken.getLength() >= token.getLength()) {
                continue;
            }

            result.remove(result.size() - 1);
            result.add(token);
            lastToken = token;
        }

        return result;
    }

    public String transduce(CharSequence input) {
        return this.transduce(input, null);
    }

    public String transduce(CharSequence input, String defaultValue) {
        List<WordData> result = this.dictionaryLookup.lookup(input);

        if (result.isEmpty()) {
            return defaultValue;
        }

        return result.get(0).getStem().toString();
    }

    private Token createToken(CharSequence original, byte[] bytes, int start, int length) {
        int charOffset = EncodingUtils.getString(bytes, 0, start).length();

        if (this.caseSensitive) {
            return Token.create(charOffset, EncodingUtils.getString(bytes, start, length));
        }

        final int charLength = EncodingUtils.getString(bytes, start, length).length();
        return Token.create(charOffset, original.subSequence(charOffset, charOffset + charLength).toString());
    }

    private byte[] getBytes(CharSequence value) {
        if (this.caseSensitive) {
            return EncodingUtils.getBytes(value);
        }

        return EncodingUtils.getBytes(value.toString().toLowerCase(Locale.ROOT));
    }
}
