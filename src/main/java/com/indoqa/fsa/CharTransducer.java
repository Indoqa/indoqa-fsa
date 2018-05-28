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

import com.indoqa.fsa.traversal.CharMatch;
import com.indoqa.fsa.utils.EncodingUtils;

public class CharTransducer implements Transducer {

    private final CharAcceptor charAcceptor;

    private char separator;

    public CharTransducer(CharAcceptor charAcceptor, char separator) {
        super();
        this.charAcceptor = charAcceptor;
        this.separator = separator;
    }

    @Override
    public String getLongestTransducedMatch(CharSequence sequence) {
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
    public List<Token> getLongestTransducedTokens(CharSequence sequence) {
        return TokenCandidate.eliminateOverlapping(this.getTransducedTokens(sequence));
    }

    @Override
    public List<Token> getTransducedTokens(CharSequence sequence) {
        List<Token> result = new ArrayList<>();

        CharMatch charMatch = CharMatch.partialMatchAllowed();

        for (int start = 0; start < sequence.length(); start++) {
            if (!EncodingUtils.isTokenStart(sequence, start)) {
                continue;
            }

            CharSequence translated = this.transduceToken(sequence, start, sequence.length() - start, charMatch);
            if (translated == null) {
                continue;
            }

            Token token = Token.create(start, sequence.subSequence(start, start + charMatch.getLength()).toString());
            token.setValue(translated.toString());
            result.add(token);
        }

        return result;
    }

    @Override
    public CharSequence transduce(CharSequence sequence) {
        return this.transduce(sequence, sequence);
    }

    @Override
    public CharSequence transduce(CharSequence sequence, CharSequence defaultValue) {
        CharSequence transduced = this.transduce(sequence, 0, sequence.length(), CharMatch.fullMatchRequired());
        if (transduced == null) {
            return defaultValue;
        }

        return transduced;
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
