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
package com.indoqa.fsa.morfologik;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.indoqa.fsa.Token;
import com.indoqa.fsa.Transducer;
import com.indoqa.fsa.morfologik.Result.Match;
import com.indoqa.fsa.utils.EncodingUtils;
import com.indoqa.fsa.utils.TokenCandidate;

import morfologik.fsa.ByteSequenceIterator;
import morfologik.stemming.Dictionary;

public class MorfologikTransducer implements Transducer {

    private final boolean caseSensitive;
    private final TransducerTraversal traversal;
    private final Dictionary dictionary;
    private final ByteSequenceIterator iterator;

    protected MorfologikTransducer(Dictionary dictionary, boolean caseSensitive) {
        this.dictionary = dictionary;
        this.traversal = new TransducerTraversal(this.dictionary.fsa, this.dictionary.metadata.getSeparator());
        this.iterator = new ByteSequenceIterator(this.dictionary.fsa, this.dictionary.fsa.getRootNode());
        this.caseSensitive = caseSensitive;
    }

    @Override
    public List<Token> getAllMatches(CharSequence sequence, int start, int length, List<Token> result) {
        byte[] bytes = this.getBytes(sequence, start, length);
        Result match = new Result();

        int maxLength = length;
        while (maxLength > 0) {
            this.traversal.match(match, bytes, 0, maxLength);
            if (match.getMatch() != Match.NON_TERMINAL_MATCH) {
                break;
            }

            this.iterator.restartFrom(match.getNode());
            ByteBuffer byteBuffer = this.iterator.next();

            Token token = this.createToken(sequence, bytes, 0, match.getMatchedLength());
            token.setStart(token.getStart() + start);
            token.setValue(StandardCharsets.UTF_8.decode(byteBuffer).toString());
            result.add(token);

            maxLength = match.getMatchedLength() - 1;
        }

        return result;
    }

    @Override
    public List<Token> getAllTokens(CharSequence sequence, int start, int length) {
        List<Token> result = new ArrayList<>();

        byte[] bytes = this.getBytes(sequence, start, length);
        Result match = new Result();

        for (int i = 0; i < bytes.length - 1; i++) {
            if (!EncodingUtils.isTokenStart(bytes, i)) {
                continue;
            }

            this.traversal.match(match, bytes, i, bytes.length - i);
            if (match.getMatch() != Match.NON_TERMINAL_MATCH) {
                continue;
            }

            if (!EncodingUtils.isTokenEnd(bytes, i + match.getMatchedLength())) {
                continue;
            }

            this.iterator.restartFrom(match.getNode());
            ByteBuffer byteBuffer = this.iterator.next();

            Token token = this.createToken(sequence, bytes, i, match.getMatchedLength());
            token.setStart(token.getStart() + start);
            token.setValue(StandardCharsets.UTF_8.decode(byteBuffer).toString());
            result.add(token);
        }

        return result;
    }

    @Override
    public Token getLongestMatch(CharSequence sequence) {
        byte[] bytes = this.getBytes(sequence, 0, sequence.length());
        Result match = new Result();

        this.traversal.match(match, bytes, 0, bytes.length);
        if (match.getMatch() != Match.NON_TERMINAL_MATCH) {
            return null;
        }

        this.iterator.restartFrom(match.getNode());
        ByteBuffer byteBuffer = this.iterator.next();
        String replacement = StandardCharsets.UTF_8.decode(byteBuffer).toString();

        Token token = Token.create(0, sequence.subSequence(0, match.getMatchedLength()).toString());
        token.setValue(replacement);
        return token;
    }

    @Override
    public List<Token> getLongestOccurrences(CharSequence sequence) {
        return TokenCandidate.eliminateOverlapping(this.getAllOccurrences(sequence));
    }

    @Override
    public List<Token> getLongestTokens(CharSequence sequence) {
        return TokenCandidate.eliminateOverlapping(this.getAllTokens(sequence));
    }

    @Override
    public CharSequence transduce(CharSequence sequence, CharSequence defaultValue) {
        byte[] bytes = this.getBytes(sequence, 0, sequence.length());
        Result match = new Result();

        this.traversal.match(match, bytes, 0, bytes.length);
        if (match.getMatch() != Match.NON_TERMINAL_MATCH || match.getMatchedLength() != sequence.length()) {
            return defaultValue;
        }

        this.iterator.restartFrom(match.getNode());
        ByteBuffer byteBuffer = this.iterator.next();
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    @Override
    public CharSequence transduce(CharSequence sequence, int start, int length) {
        byte[] bytes = this.getBytes(sequence, start, length);
        Result match = new Result();

        this.traversal.match(match, bytes, 0, bytes.length);
        if (match.getMatch() != Match.NON_TERMINAL_MATCH || match.getMatchedLength() != sequence.length()) {
            return null;
        }

        this.iterator.restartFrom(match.getNode());
        ByteBuffer byteBuffer = this.iterator.next();
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    private Token createToken(CharSequence sequence, byte[] bytes, int start, int length) {
        int charOffset = EncodingUtils.getString(bytes, 0, start).length();

        if (this.caseSensitive) {
            return Token.create(charOffset, EncodingUtils.getString(bytes, start, length));
        }

        final int charLength = EncodingUtils.getString(bytes, start, length).length();
        return Token.create(charOffset, sequence.subSequence(charOffset, charOffset + charLength).toString());
    }

    private byte[] getBytes(CharSequence sequence, int offset, int length) {
        if (this.caseSensitive) {
            return EncodingUtils.getBytes(sequence, offset, length);
        }

        return EncodingUtils.getBytes(sequence.subSequence(offset, offset + length).toString().toLowerCase(Locale.ROOT));
    }
}
