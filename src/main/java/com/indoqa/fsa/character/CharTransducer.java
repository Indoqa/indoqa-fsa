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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.indoqa.fsa.Token;
import com.indoqa.fsa.Transducer;
import com.indoqa.fsa.utils.CharMatch;
import com.indoqa.fsa.utils.EncodingUtils;
import com.indoqa.fsa.utils.TokenCandidate;

public class CharTransducer implements Transducer {

    private final CharAcceptor charAcceptor;

    private char separator;

    public CharTransducer(CharAcceptor charAcceptor, char separator) {
        super();
        this.charAcceptor = charAcceptor;
        this.separator = separator;
    }

    @Override
    public List<Token> getAllMatches(CharSequence sequence, int start, int length, List<Token> result) {
        this.charAcceptor.getAllPrefixes(sequence, start, length, this.separator, result);

        CharMatch charMatch = CharMatch.partialMatchAllowed();
        for (Token token : result) {
            CharSequence transduce = this.transduce(token.getValue(), 0, token.getLength(), charMatch);
            token.setValue(transduce.toString());
        }

        return result;
    }

    @Override
    public List<Token> getAllTokens(CharSequence sequence, int start, int length) {
        List<Token> result = null;

        CharMatch charMatch = CharMatch.partialMatchAllowed();

        for (int i = start; i < start + length; i++) {
            if (!EncodingUtils.isTokenStart(sequence, i)) {
                continue;
            }

            CharSequence translated = this.transduceToken(sequence, i, length - i, charMatch);
            if (translated == null) {
                continue;
            }

            if (result == null) {
                result = new ArrayList<>();
            }

            Token token = Token.create(i, sequence.subSequence(i, i + charMatch.getLength()).toString());
            token.setValue(translated.toString());
            result.add(token);
        }

        if (result == null) {
            return Collections.emptyList();
        }

        return result;
    }

    @Override
    public String getLongestMatch(CharSequence sequence) {
        CharMatch charMatch = CharMatch.partialMatchAllowed();

        CharSequence transduce = this.transduce(sequence, 0, sequence.length(), charMatch);
        if (charMatch.getIndex() == -1) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder(transduce);
        stringBuilder.append(sequence, charMatch.getLength(), sequence.length());
        return stringBuilder.toString();
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
        CharSequence transduced = this.transduce(sequence, 0, sequence.length(), CharMatch.fullMatchRequired());
        if (transduced == null) {
            return defaultValue;
        }

        return transduced;
    }

    @Override
    public CharSequence transduce(CharSequence sequence, int start, int length) {
        return this.transduce(sequence, start, length, CharMatch.fullMatchRequired());
    }

    private CharSequence transduce(CharSequence sequence, int start, int length, CharMatch match) {
        this.charAcceptor.getLongestPrefix(sequence, start, length, this.separator, match);

        if (!match.isMatch(length)) {
            return null;
        }

        int index = this.charAcceptor.getNextIndex(this.separator, match.getIndex());
        if (index == -1) {
            return null;
        }

        return this.charAcceptor.getInput(index);
    }

    private CharSequence transduceToken(CharSequence sequence, int start, int length, CharMatch match) {
        this.charAcceptor.getLongestTokenPrefix(sequence, start, length, this.separator, match);

        if (!match.isMatch(length)) {
            return null;
        }

        int index = this.charAcceptor.getNextIndex(this.separator, match.getIndex());
        if (index == -1) {
            return null;
        }

        return this.charAcceptor.getInput(index);
    }
}
