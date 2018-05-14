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

import static com.indoqa.fsa.TokenCandidate.eliminateOverlapping;
import static com.indoqa.fsa.traversal.Result.Match.EXACT_MATCH;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.indoqa.fsa.traversal.AllMatchesFSATraversal;
import com.indoqa.fsa.traversal.PrefixFSATraversal;
import com.indoqa.fsa.traversal.Result;
import com.indoqa.fsa.utils.EncodingUtils;

import morfologik.fsa.FSA;

public class MorfologikAcceptor implements Acceptor {

    private final boolean caseSensitive;
    private final int rootNode;
    private final PrefixFSATraversal prefixTraversal;
    private final AllMatchesFSATraversal allMatchesTraversal;

    protected MorfologikAcceptor(FSA fsa, boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        this.rootNode = fsa.getRootNode();
        this.prefixTraversal = new PrefixFSATraversal(fsa);
        this.allMatchesTraversal = new AllMatchesFSATraversal(fsa);
    }

    @Override
    public boolean accepts(CharSequence sequence) {
        if (sequence == null) {
            return false;
        }

        byte[] bytes = this.getBytes(sequence);

        Result result = this.prefixTraversal.match(bytes);
        return result.getMatch() == EXACT_MATCH;
    }

    @Override
    public boolean accepts(CharSequence sequence, int start, int length) {
        return this.accepts(sequence.subSequence(start, start + length));
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
    @Override
    public String[] getAllMatches(CharSequence sequence) {
        byte[] bytes = this.getBytes(sequence);
        int[] allMatches = this.allMatchesTraversal.getAllMatches(bytes, 0, bytes.length);
        String[] result = new String[allMatches.length];

        for (int i = 0; i < allMatches.length; i++) {
            result[i] = EncodingUtils.getString(bytes, 0, allMatches[i]);
        }

        return result;
    }

    @Override
    public String[] getAllMatches(CharSequence sequence, int start, int length) {
        return this.getAllMatches(sequence.subSequence(start, start + length));
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
    @Override
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

    @Override
    public List<Token> getAllOccurrences(CharSequence sequence, int start, int length) {
        return this.getAllOccurrences(sequence.subSequence(start, start + length));
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
    @Override
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

    @Override
    public List<Token> getAllTokens(CharSequence sequence, int start, int length) {
        return this.getAllTokens(sequence.subSequence(start, start + length));
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
    @Override
    public String getLongestMatch(CharSequence charSequence) {
        byte[] bytes = this.getBytes(charSequence);

        Result result = this.prefixTraversal.match(new Result(), bytes, 0, bytes.length, this.rootNode);
        if (result.isTerminalMatch()) {
            return EncodingUtils.getString(bytes, 0, result.getMatchedLength());
        }

        return null;
    }

    @Override
    public String getLongestMatch(CharSequence sequence, int start, int length) {
        return this.getLongestMatch(sequence.subSequence(start, start + length));
    }

    /**
     * Performs {@link #getAllOccurrences(CharSequence)} and then eliminates overlapping {@link Token Tokens} by only keeping the
     * longest.
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return The longest occurrences of accepted input.
     */
    @Override
    public List<Token> getLongestOccurrences(CharSequence charSequence) {
        return eliminateOverlapping(this.getAllOccurrences(charSequence));
    }

    @Override
    public List<Token> getLongestOccurrences(CharSequence sequence, int start, int length) {
        return this.getLongestOccurrences(sequence.subSequence(start, start + length));
    }

    /**
     * Performs {@link #getAllTokens(CharSequence)} and then eliminates overlapping {@link Token Tokens} by only keeping the longest.
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return The longest tokens of accepted input.
     */
    @Override
    public List<Token> getLongestTokens(CharSequence charSequence) {
        return eliminateOverlapping(this.getAllTokens(charSequence));
    }

    @Override
    public List<Token> getLongestTokens(CharSequence sequence, int start, int length) {
        return this.getLongestOccurrences(sequence.subSequence(start, start + length));
    }

    private byte[] getBytes(CharSequence value) {
        if (this.caseSensitive) {
            return EncodingUtils.getBytes(value);
        }

        return EncodingUtils.getBytes(value.toString().toLowerCase(Locale.ROOT));
    }
}
