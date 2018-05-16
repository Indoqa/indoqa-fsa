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

import com.indoqa.fsa.utils.EncodingUtils;

public class CharAcceptor implements Acceptor {

    protected static final int ADDRESS_OFFSET = 1;
    protected static final int FLAGS_OFFSET = 1;
    protected static final int MASK_SIZE = 0x7FFF;
    protected static final int MASK_TERMINAL = 0x8000;
    protected static final int MASK_LAST = 0x4000;
    protected static final int NODE_SIZE = 3;

    private static final char[] CASE_INSENSITIVE = new char[Character.MAX_VALUE];
    static {
        for (char value = 0; value < CASE_INSENSITIVE.length; value++) {
            if (Character.isLowerCase(value)) {
                CASE_INSENSITIVE[value] = Character.toUpperCase(value);
            } else if (Character.isUpperCase(value)) {
                CASE_INSENSITIVE[value] = Character.toLowerCase(value);
            } else {
                CASE_INSENSITIVE[value] = value;
            }
        }
    }

    private final char[] data;
    private boolean caseSensitive;

    protected CharAcceptor(char[] data, boolean caseSensitive) {
        this.data = data;
        this.caseSensitive = caseSensitive;
    }

    protected static boolean equals(char required, char actual, boolean caseSensitive) {
        if (required == actual) {
            return true;
        }

        return !caseSensitive && CASE_INSENSITIVE[required] == actual;
    }

    @Override
    public boolean accepts(CharSequence sequence) {
        return this.accepts(sequence, 0, sequence.length());
    }

    @Override
    public boolean accepts(CharSequence sequence, int start, int length) {
        int index = 0;
        int arc = 0;

        for (int i = start; i < start + length; i++) {
            arc = this.getArc(index, sequence.charAt(i));
            if (arc == -1) {
                return false;
            }

            index = this.getTarget(arc);
        }

        return this.isTerminal(arc);
    }

    @Override
    public String[] getAllMatches(CharSequence sequence) {
        return this.getAllMatches(sequence, 0, sequence.length());
    }

    @Override
    public String[] getAllMatches(CharSequence sequence, int start, int length) {
        List<String> result = new ArrayList<>();
        int index = 0;

        for (int i = start; i < start + length; i++) {
            index = this.getArc(index, sequence.charAt(i));
            if (index == -1) {
                break;
            }

            if (this.isTerminal(index)) {
                result.add(sequence.subSequence(start, i + 1).toString());
            }

            index = this.getTarget(index);
        }

        return result.toArray(new String[result.size()]);
    }

    @Override
    public List<Token> getAllOccurrences(CharSequence sequence) {
        return this.getAllOccurrences(sequence, 0, sequence.length());
    }

    @Override
    public List<Token> getAllOccurrences(CharSequence sequence, int start, int length) {
        List<Token> result = new ArrayList<>();

        for (int i = start; i < start + length - 1; i++) {
            String[] allMatches = this.getAllMatches(sequence, i, sequence.length() - i);
            for (String eachMatch : allMatches) {
                result.add(Token.create(i, eachMatch));
            }
        }

        return result;
    }

    @Override
    public List<Token> getAllTokens(CharSequence sequence) {
        return this.getAllTokens(sequence, 0, sequence.length());
    }

    @Override
    public List<Token> getAllTokens(CharSequence sequence, int start, int length) {
        List<Token> result = new ArrayList<>();

        for (int i = start; i < start + length - 1; i++) {
            if (!EncodingUtils.isTokenStart(sequence, i)) {
                continue;
            }

            String[] allMatches = this.getAllMatches(sequence, i, sequence.length() - i);
            for (String eachMatch : allMatches) {
                if (!EncodingUtils.isTokenEnd(sequence, i + eachMatch.length())) {
                    continue;
                }

                Token token = Token.create(i, eachMatch);
                result.add(token);
            }
        }

        return result;
    }

    @Override
    public String getLongestMatch(CharSequence sequence) {
        return this.getLongestMatch(sequence, 0, sequence.length());
    }

    @Override
    public String getLongestMatch(CharSequence sequence, int start, int length) {
        int result = 0;
        int index = 0;

        for (int i = start; i < start + length; i++) {
            index = this.getArc(index, sequence.charAt(i));
            if (index == -1) {
                break;
            }

            if (this.isTerminal(index)) {
                result = i + 1;
            }

            index = this.getTarget(index);
        }

        if (result == 0) {
            return null;
        }

        return sequence.subSequence(0, result).toString();
    }

    @Override
    public List<Token> getLongestOccurrences(CharSequence sequence) {
        return this.getLongestOccurrences(sequence, 0, sequence.length());
    }

    @Override
    public List<Token> getLongestOccurrences(CharSequence sequence, int start, int length) {
        return TokenCandidate.eliminateOverlapping(this.getAllOccurrences(sequence, start, length));
    }

    @Override
    public List<Token> getLongestTokens(CharSequence charSequence) {
        return this.getLongestTokens(charSequence, 0, charSequence.length());
    }

    @Override
    public List<Token> getLongestTokens(CharSequence sequence, int start, int length) {
        return TokenCandidate.eliminateOverlapping(this.getAllTokens(sequence, start, length));
    }

    private int getArc(int index, char label) {
        for (int i = index; index < this.data.length; i += NODE_SIZE) {
            if (equals(this.getLabel(i), label, this.caseSensitive)) {
                return i;
            }

            if (this.isLast(i)) {
                break;
            }
        }

        return -1;
    }

    private char getLabel(int index) {
        return this.data[index];
    }

    private int getTarget(int index) {
        return (this.data[index + ADDRESS_OFFSET] & 0x3FFF) << 16 | this.data[index + ADDRESS_OFFSET + 1];
    }

    private boolean isLast(int index) {
        return (this.data[index + FLAGS_OFFSET] & MASK_LAST) != 0;
    }

    private boolean isTerminal(int index) {
        return (this.data[index + FLAGS_OFFSET] & MASK_TERMINAL) != 0;
    }
}
