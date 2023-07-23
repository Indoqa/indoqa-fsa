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

import static com.indoqa.fsa.character.CharDataAccessor.*;

import java.util.*;

import com.indoqa.fsa.Acceptor;
import com.indoqa.fsa.Token;
import com.indoqa.fsa.utils.CharMatch;
import com.indoqa.fsa.utils.EncodingUtils;
import com.indoqa.fsa.utils.TokenCandidate;

public class CharAcceptor implements Acceptor {

    protected final char[] data;
    private boolean caseSensitive;

    protected CharAcceptor(char[] data, boolean caseSensitive) {
        this.data = data;
        this.caseSensitive = caseSensitive;
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
            arc = getArc(this.data, index, sequence.charAt(i), this.caseSensitive);
            if (arc == -1) {
                return false;
            }

            index = getTarget(this.data, arc);
            if (index == 0 && i < start + length - 1) {
                return false;
            }
        }

        return isTerminal(this.data, arc);
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
            index = getArc(this.data, index, sequence.charAt(i), this.caseSensitive);
            if (index == -1) {
                break;
            }

            if (isTerminal(this.data, index)) {
                result.add(sequence.subSequence(start, i + 1).toString());
            }

            index = getTarget(this.data, index);
            if (index == 0) {
                break;
            }
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

        for (int i = start; i < start + length; i++) {
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
                if (!EncodingUtils.isTokenEnd(sequence, i + eachMatch.length() - 1)) {
                    continue;
                }

                Token token = Token.create(i, eachMatch);
                result.add(token);
            }
        }

        return result;
    }

    public List<String> getCompletions(CharSequence sequence, int maxCount) {
        if (maxCount < 1) {
            return Collections.emptyList();
        }

        // first navigate the graph to match the input sequence
        int index = 0;
        int arc = 0;

        for (int i = 0; i < sequence.length(); i++) {
            arc = getArc(this.data, index, sequence.charAt(i), this.caseSensitive);
            if (arc == -1) {
                return Collections.emptyList();
            }

            index = getTarget(this.data, arc);
            if (index == 0 && i < sequence.length() - 1) {
                return Collections.emptyList();
            }
        }

        List<String> completions = new ArrayList<>();

        if (sequence.length() > 0 && isTerminal(this.data, arc)) {
            // if we have a sequence and that sequence is terminal, add it to the result
            completions.add(sequence.toString());
        }

        if (sequence.length() == 0 || index != 0) {
            // index == 0 is either "no more nodes" or "beginning of graph"
            // the length of the sequence tells the difference
            Iterator<String> iterator = new AcceptorIterator(index, sequence);
            while (completions.size() < maxCount && iterator.hasNext()) {
                completions.add(iterator.next());
            }
        }

        return completions;
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
            index = getArc(this.data, index, sequence.charAt(i), this.caseSensitive);
            if (index == -1) {
                break;
            }

            if (isTerminal(this.data, index)) {
                result = i + 1;
            }

            index = getTarget(this.data, index);
            if (index == 0) {
                break;
            }
        }

        if (result == 0) {
            return null;
        }

        return sequence.subSequence(start, result).toString();
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

    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    public Iterator<String> iterator() {
        return new AcceptorIterator(0, "");
    }

    protected void getAllPrefixes(CharSequence sequence, int start, int length, char separator, List<Token> result) {
        int matchedLength = 0;
        int index = 0;
        int arc = 0;

        for (int i = start; i < start + length; i++) {
            arc = getArc(this.data, index, sequence.charAt(i), this.caseSensitive);
            if (arc == -1) {
                break;
            }

            index = getTarget(this.data, arc);
            if (index == 0) {
                break;
            }

            matchedLength++;
            if (getArc(this.data, index, separator, this.caseSensitive) != -1) {
                result.add(Token.create(start, sequence.subSequence(start, start + matchedLength).toString()));
            }
        }
    }

    protected String getInput(int startIndex) {
        StringBuilder stringBuilder = new StringBuilder();

        int index = startIndex;
        while (true) {
            char label = getLabel(this.data, index);
            stringBuilder.append(label);

            if (isTerminal(this.data, index)) {
                break;
            }

            index = getTarget(this.data, index);
            if (index == 0) {
                break;
            }
        }

        return stringBuilder.toString();
    }

    protected void getLongestPrefix(CharSequence sequence, int start, int length, char separator, CharMatch charMatch) {
        int index = 0;
        int arc = 0;
        charMatch.setIndex(-1);

        for (int i = start; i < start + length; i++) {
            arc = getArc(this.data, index, sequence.charAt(i), this.caseSensitive);
            if (arc == -1) {
                return;
            }

            index = getTarget(this.data, arc);
            if (index == 0) {
                break;
            }

            if (getArc(this.data, index, separator, this.caseSensitive) != -1) {
                charMatch.setIndex(index);
                charMatch.setLength(i - start + 1);
            }
        }
    }

    protected void getLongestTokenPrefix(CharSequence sequence, int start, int length, char separator, CharMatch charMatch) {
        int index = 0;
        charMatch.setIndex(-1);

        for (int i = start; i < start + length; i++) {
            index = getArc(this.data, index, sequence.charAt(i), this.caseSensitive);
            if (index == -1) {
                return;
            }

            index = getTarget(this.data, index);
            if (index == 0) {
                break;
            }

            if (EncodingUtils.isTokenEnd(sequence, i) && getArc(this.data, index, separator, this.caseSensitive) != -1) {
                charMatch.setIndex(index);
                charMatch.setLength(i - start + 1);
            }
        }
    }

    protected int getNextIndex(char label, int index) {
        int arc = getArc(this.data, index, label, this.caseSensitive);
        if (arc == -1) {
            return -1;
        }

        return getTarget(this.data, arc);
    }

    private class AcceptorIterator implements Iterator<String> {

        private final StringBuilder stringBuilder;

        private int[] indexes;
        private int offset;
        private String next;
        private boolean first;

        public AcceptorIterator(int startIndex, CharSequence prefix) {
            super();

            this.indexes = new int[64];
            this.indexes[0] = startIndex;

            this.stringBuilder = new StringBuilder(prefix);
            this.offset = 0;
            this.first = true;
        }

        @Override
        public boolean hasNext() {
            if (this.next != null) {
                return true;
            }

            while (true) {
                int currentIndex = this.indexes[this.offset];

                if (currentIndex == 0) {
                    if (this.offset != 0) {
                        // move back
                        this.stringBuilder.setLength(this.stringBuilder.length() - 1);
                        this.offset--;

                        if (isLast(CharAcceptor.this.data, this.indexes[this.offset])) {
                            // the previous node does not have a sibling, move back again
                            this.indexes[this.offset] = 0;
                        } else {
                            // move to the next sibling of the previous node
                            this.indexes[this.offset] += NODE_SIZE;
                        }

                        continue;
                    }

                    if (!this.first) {
                        return false;
                    }
                }

                char label = getLabel(CharAcceptor.this.data, currentIndex);
                this.stringBuilder.append(label);
                this.offset++;
                this.addIndex(getTarget(CharAcceptor.this.data, currentIndex));

                if (isTerminal(CharAcceptor.this.data, currentIndex)) {
                    this.next = this.stringBuilder.toString();
                    this.first = false;
                    return true;
                }
            }
        }

        @Override
        public String next() {
            if (this.next == null && !this.hasNext()) {
                throw new NoSuchElementException();
            }

            String result = this.next;
            this.next = null;
            return result;
        }

        private void addIndex(int index) {
            if (this.offset >= this.indexes.length) {
                int[] newIndexes = new int[this.offset + 1];
                System.arraycopy(this.indexes, 0, newIndexes, 0, this.indexes.length);
                this.indexes = newIndexes;
            }

            this.indexes[this.offset] = index;
        }
    }
}
