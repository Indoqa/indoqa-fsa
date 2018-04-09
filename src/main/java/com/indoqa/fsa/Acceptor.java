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

import static com.indoqa.fsa.traversal.Result.Match.EXACT_MATCH;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.indoqa.fsa.traversal.AllMatchesFSATraversal;
import com.indoqa.fsa.traversal.PrefixFSATraversal;
import com.indoqa.fsa.traversal.Result;
import com.indoqa.fsa.utils.EncodingUtils;

import morfologik.fsa.FSA;

public class Acceptor {

    private static final String[] EMPTY_STRINGS = new String[0];

    private final boolean caseSensitive;
    private final int rootNode;
    private final PrefixFSATraversal prefixTraversal;
    private final AllMatchesFSATraversal allMatchesTraversal;

    protected Acceptor(FSA fsa, boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        this.rootNode = fsa.getRootNode();
        this.prefixTraversal = new PrefixFSATraversal(fsa);
        this.allMatchesTraversal = new AllMatchesFSATraversal(fsa);
    }

    private static List<Token> eliminateOverlapping(List<Token> tokens) {
        List<TokenCandidate> candidates = tokens.stream().map(TokenCandidate::create).collect(Collectors.toList());

        for (int i = 0; i < candidates.size() - 1; i++) {
            TokenCandidate candidate = candidates.get(i);

            for (int j = i + 1; j < candidates.size(); j++) {
                TokenCandidate otherCandidate = candidates.get(j);
                if (candidate.isDisjunct(otherCandidate)) {
                    break;
                }

                if (candidate.getLength() > otherCandidate.getLength()) {
                    candidate.addChallenges(otherCandidate);
                } else {
                    otherCandidate.addChallenges(candidate);
                }
            }
        }

        boolean changed;
        do {
            changed = false;

            for (TokenCandidate eachCandidate : candidates) {
                if (eachCandidate.canBeSelected()) {
                    eachCandidate.select();
                    changed = true;
                }
            }
        } while (changed);

        return candidates.stream().filter(TokenCandidate::isSelected).map(TokenCandidate::getToken).collect(Collectors.toList());
    }

    public boolean accepts(CharSequence value) {
        if (value == null) {
            return false;
        }

        byte[] bytes = this.getBytes(value);

        Result result = this.prefixTraversal.match(bytes);
        return result.getMatch() == EXACT_MATCH;
    }

    /**
     * Find the all accepted inputs at the beginning of given <code>charSequence</code>.<br/>
     * <br/>
     * Given the sequence <code>aa bbb cccc ddddd</code><br/>
     * and the accepted inputs <code>a</code>, <code>aa</code>, <code>aaa</code>, <code>b</code>, <code>bb</code>,
     * <code>bbb</code><br/>
     * the matches will be <code>a</code> and <code>aa</code>
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return All accepted inputs at the beginning of the charSequence.
     */
    public String[] getAllMatches(CharSequence value) {
        if (value == null) {
            return EMPTY_STRINGS;
        }

        byte[] bytes = this.getBytes(value);
        return this.getAllMatches(bytes, 0, bytes.length);
    }

    /**
     * Find all accepted inputs in the given <code>charSequence</code>.<br/>
     * <p>
     * The only difference to {@link #getAllTokens(CharSequence)} is that the accepted input may occur at any position within the
     * <code>charSequence</code> (specifically start and end inside a token).
     * </p>
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return all occurrences of accepted input
     */
    public List<Token> getAllOccurrences(CharSequence charSequence) {
        List<Token> result = new ArrayList<>();

        byte[] bytes = this.getBytes(charSequence);
        byte[] originalBytes = this.caseSensitive ? bytes : EncodingUtils.getBytes(charSequence);

        for (int offset = 0; offset < bytes.length - 1; offset++) {
            int[] allMatches = this.allMatchesTraversal.getAllMatches(bytes, offset, bytes.length - offset);
            for (int eachMatch : allMatches) {
                int charOffset = EncodingUtils.getString(bytes, 0, offset).length();
                Token token = Token.create(charOffset, EncodingUtils.getString(originalBytes, offset, eachMatch - offset));
                result.add(token);

            }
        }

        return result;
    }

    /**
     * Find all accepted inputs that are tokens in the given <code>charSequence</code>.<br/>
     * <p>
     * A part of the given sequence is considered to be a <code>token</code>, when it starts and ends at a token boundary.<br/>
     * A token boundary is the change from a non-word character to a word character (or vice-versa), as well as the beginning and end
     * of the whole sequence.<br/>
     * Please note that a token may contain token boundaries.
     * </p>
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return All tokens of accepted inputs.
     */
    public List<Token> getAllTokens(CharSequence charSequence) {
        List<Token> result = new ArrayList<>();

        byte[] bytes = this.getBytes(charSequence);
        byte[] originalBytes = this.caseSensitive ? bytes : EncodingUtils.getBytes(charSequence);

        for (int offset = 0; offset < bytes.length - 1; offset++) {
            if (!EncodingUtils.isTokenStart(bytes, offset)) {
                continue;
            }

            int[] allMatches = this.allMatchesTraversal.getAllMatches(bytes, offset, bytes.length - offset);
            for (int eachMatch : allMatches) {
                if (!EncodingUtils.isTokenEnd(bytes, eachMatch)) {
                    continue;
                }

                int charOffset = EncodingUtils.getString(bytes, 0, offset).length();
                Token token = Token.create(charOffset, EncodingUtils.getString(originalBytes, offset, eachMatch - offset));
                result.add(token);
            }
        }

        return result;
    }

    /**
     * Find the longest accepted input at the beginning of given <code>charSequence</code>.<br/>
     * <br/>
     * Given the sequence <code>aa bbb cccc ddddd</code><br/>
     * and the accepted inputs <code>a</code>, <code>aa</code>, <code>aaa</code>, <code>b</code>, <code>bb</code>,
     * <code>bbb</code><br/>
     * the longest match will be <code>aa</code>
     *
     * @param charSequence The charSequence to examine.
     * @return The longest accepted input at the beginning of the charSequence.
     */
    public String getLongestMatch(CharSequence charSequence) {
        byte[] bytes = this.getBytes(charSequence);

        Result result = this.getLongestMatch(bytes, 0, bytes.length);
        if (result.isTerminalMatch()) {
            return EncodingUtils.getString(bytes, 0, result.getMatchedLength());
        }

        return null;
    }

    /**
     * Performs {@link #getAllOccurrences(CharSequence)} and then eliminates overlapping {@link Token Tokens} by only keeping the
     * longest.
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return The longest occurrences of accepted input.
     */
    public List<Token> getLongestOccurrences(CharSequence charSequence) {
        return eliminateOverlapping(this.getAllOccurrences(charSequence));
    }

    /**
     * Performs {@link #getAllTokens(CharSequence)} and then eliminates overlapping {@link Token Tokens} by only keeping the longest.
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return The longest tokens of accepted input.
     */
    public List<Token> getLongestTokens(CharSequence charSequence) {
        return eliminateOverlapping(this.getAllTokens(charSequence));
    }

    private String[] getAllMatches(byte[] bytes, int offset, int length) {
        int[] allMatches = this.allMatchesTraversal.getAllMatches(bytes, offset, length);
        String[] result = new String[allMatches.length];

        for (int i = 0; i < allMatches.length; i++) {
            result[i] = EncodingUtils.getString(bytes, offset, allMatches[i]);
        }

        return result;
    }

    private byte[] getBytes(CharSequence value) {
        if (this.caseSensitive) {
            return EncodingUtils.getBytes(value);
        }

        return EncodingUtils.getBytes(value.toString().toLowerCase(Locale.ROOT));
    }

    private Result getLongestMatch(byte[] bytes, int offset, int length) {
        return this.prefixTraversal.match(new Result(), bytes, offset, length, this.rootNode);
    }

    private static class TokenCandidate {

        private Token token;
        private final List<TokenCandidate> challenges = new ArrayList<>();
        private int challengedBy;
        private boolean selected = true;

        public static TokenCandidate create(Token token) {
            TokenCandidate result = new TokenCandidate();
            result.token = token;
            return result;
        }

        public void addChallenges(TokenCandidate otherCandidate) {
            this.challenges.add(otherCandidate);
            otherCandidate.challengedBy++;
        }

        public boolean canBeSelected() {
            return this.selected && this.challengedBy == 0;
        }

        public int getLength() {
            return this.token.getLength();
        }

        public Token getToken() {
            return this.token;
        }

        public boolean isDisjunct(TokenCandidate otherCandidate) {
            return this.token.isDisjunct(otherCandidate.getToken());
        }

        public boolean isSelected() {
            return this.selected;
        }

        public void select() {
            for (TokenCandidate eachChallenges : this.challenges) {
                eachChallenges.eliminate();
            }

            this.challengedBy--;
        }

        private void eliminate() {
            for (TokenCandidate eachIntersect : this.challenges) {
                eachIntersect.challengedBy--;
            }

            this.selected = false;
        }
    }
}
