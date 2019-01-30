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

public interface Transducer {

    /**
     * Find all transducible matches starting at the beginning of the given <code>sequence</code>.<br/>
     * <br/>
     * Matches are not required to happen at a token boundary.
     *
     * @param sequence The sequence in which to find matches.
     *
     * @return All transduced {@link Token tokens}. Never <code>null</code>.
     */
    default List<Token> getAllMatches(CharSequence sequence) {
        return this.getAllMatches(sequence, 0, sequence.length());
    }

    /**
     * Find all transducible matches starting at <code>start</code> with a length of up to <code>length</code> in the given
     * <code>sequence</code>.<br/>
     * <br/>
     * Matches are not required to happen at a token boundary.
     *
     * @param sequence The word in which to find matches.
     * @param start The index within the word at which matches must begin.
     * @param length The maximum length to consider.
     *
     * @return All transduced {@link Token tokens}. Never <code>null</code>.
     */
    List<Token> getAllMatches(CharSequence sequence, int start, int length);

    /**
     * Find all transducible tokens anywhere in the given <code>sequence</code>.<br/>
     * <br/>
     * Matches are not required to happen at a token boundary. <br/>
     * This might include overlapping tokens.
     *
     * @param sequence The sequence in which to find tokens.
     *
     * @return All tokens found. Never <code>null</code>.
     */
    default List<Token> getAllOccurrences(CharSequence sequence) {
        return this.getAllOccurrences(sequence, 0, sequence.length());
    }

    /**
     * Find all transducible tokens anywhere between <code>start</code> and <code>start + length</code> in the given
     * <code>sequence</code>.<br/>
     * <br/>
     * Matches are not required to happen at a token boundary. <br/>
     * This might include overlapping tokens.
     *
     * @param sequence The sequence in which to find tokens.
     *
     * @return All tokens found. Never <code>null</code>.
     */
    default List<Token> getAllOccurrences(CharSequence sequence, int start, int length) {
        List<Token> result = new ArrayList<>();

        for (int i = start; i < start + length; i++) {
            result.addAll(this.getAllMatches(sequence, i, length - i));
        }

        return result;
    }

    /**
     * Find all transducible tokens anywhere in the given <code>sequence</code>.<br/>
     * <br/>
     * Matches must happen at a token boundary. <br/>
     * This might include overlapping tokens.
     *
     * @param sequence The sequence in which to find tokens.
     *
     * @return All tokens found. Never <code>null</code>.
     */
    default List<Token> getAllTokens(CharSequence sequence) {
        return this.getAllTokens(sequence, 0, sequence.length());
    }

    /**
     * Find all transducible tokens anywhere between <code>start</code> and <code>start + length</code> in the given
     * <code>sequence</code>.<br/>
     * <br/>
     * Matches must happen at a token boundary. <br/>
     * This might include overlapping tokens.
     *
     * @param sequence The sequence in which to find tokens.
     *
     * @return All tokens found. Never <code>null</code>.
     */
    List<Token> getAllTokens(CharSequence sequence, int start, int length);

    /**
     * Find the longest transducible match from the beginning of the given <code>sequence</code>. <br/>
     * <br/>
     * Matches are not required to happen at a token boundary. <br/>
     * Uses the length of the match (not of the transduction) for comparison.
     *
     * @param sequence The sequence in which to find matches.
     *
     * @return The transduction of the longest match or <code>null</code>.
     */
    String getLongestMatch(CharSequence sequence);

    /**
     * Find all transducible tokens anywhere in the given <code>sequence</code>.<br/>
     * <br/>
     * Matches must happen at a token boundary. <br/>
     * If tokens overlap, only the longest will be returned. Uses the length of the match (not of the transduction) for comparison.
     *
     * @param sequence The sequence in which to find tokens.
     *
     * @return All tokens found. Never <code>null</code>.
     */
    List<Token> getLongestTokens(CharSequence sequence);

    /**
     * Return the transduction of the complete given <code>sequence</code>.<br/>
     * <br/>
     * If the complete sequence is not transducible, the given <code>sequence</code> will be returned.
     *
     * @param sequence The seqeunce to transduce.
     *
     * @return Either the transduction or the original sequence.
     */
    default CharSequence transduce(CharSequence sequence) {
        return this.transduce(sequence, sequence);
    }

    /**
     * Return the transduction of the complete given <code>sequence</code>.<br/>
     * <br/>
     * If the complete sequence is not transducible, the given <code>defaultValue</code> will be returned.
     *
     * @param sequence The seqeunce to transduce.
     * @param defaultValue The default value to return if <code>sequence</code> is not transducible.
     *
     * @return Either the transduction or the defaultValue.
     */
    CharSequence transduce(CharSequence sequence, CharSequence defaultValue);

}
