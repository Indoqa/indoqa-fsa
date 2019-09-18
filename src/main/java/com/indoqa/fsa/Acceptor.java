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

import java.util.List;

public interface Acceptor {

    /**
     * Checks whether or not this {@link Acceptor} accepts the given <code>sequence</code>.
     *
     * @param sequence The {@link CharSequence} to check.
     * @return <code>true</code> if and only if this {@link Acceptor} accepts the given <code>sequence</code>
     */
    boolean accepts(CharSequence sequence);

    /**
     * Performs the same opeation as {@link #accepts(CharSequence)} but on the part of <code>sequence</code> denoted by
     * <code>start</code> and <code>length</code>.
     *
     * @see #accepts(CharSequence)
     */
    boolean accepts(CharSequence sequence, int start, int length);

    /**
     * Find all accepted inputs at the beginning of given <code>charSequence</code>.<br/>
     * <br/>
     * Given the sequence <code>aa bbb cccc ddddd</code><br/>
     * and the accepted inputs <code>a</code>, <code>aa</code>, <code>aaa</code>, <code>b</code>, <code>bb</code>,
     * <code>bbb</code><br/>
     * the matches will be <code>a</code> and <code>aa</code>
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return All accepted inputs at the beginning of the charSequence.
     */
    String[] getAllMatches(CharSequence sequence);

    /**
     * Performs the same opeation as {@link #getAllMatches(CharSequence, int, int)} but on the part of <code>sequence</code> denoted by
     * <code>start</code> and <code>length</code>.
     *
     * @see #getAllMatches(CharSequence)
     */
    String[] getAllMatches(CharSequence sequence, int start, int length);

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
    List<Token> getAllOccurrences(CharSequence sequence);

    /**
     * Performs the same operation as {@link #getAllOccurrences(CharSequence)} but on the part of <code>sequence</code> denoted by
     * <code>start</code> and <code>length</code>.
     *
     * @see #getAllOccurrences(CharSequence)
     */
    List<Token> getAllOccurrences(CharSequence sequence, int start, int length);

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
    List<Token> getAllTokens(CharSequence sequence);

    /**
     * Performs the same operation as {@link #getAllTokens(CharSequence)} but on the part of <code>sequence</code> denoted by
     * <code>start</code> and <code>length</code>.
     *
     * @see #getAllTokens(CharSequence)
     */
    List<Token> getAllTokens(CharSequence sequence, int start, int length);

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
    String getLongestMatch(CharSequence sequence);

    /**
     * Performs the same operation as {@link #getLongestMatch(CharSequence)} but on the part of <code>sequence</code> denoted by
     * <code>start</code> and <code>length</code>.
     *
     * @see #getLongestMatch(CharSequence)
     */
    String getLongestMatch(CharSequence sequence, int start, int length);

    /**
     * Performs {@link #getAllOccurrences(CharSequence)} and then eliminates overlapping {@link Token Tokens} by only keeping the
     * longest.
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return The longest occurrences of accepted input.
     */
    List<Token> getLongestOccurrences(CharSequence sequence);

    /**
     * Performs the same operation as {@link #getLongestOccurrences(CharSequence)} but on the part of <code>sequence</code> denoted by
     * <code>start</code> and <code>length</code>.
     *
     * @see #getLongestOccurrences(CharSequence)
     */
    List<Token> getLongestOccurrences(CharSequence sequence, int start, int length);

    /**
     * Performs {@link #getAllTokens(CharSequence)} and then eliminates overlapping {@link Token Tokens} by only keeping the longest.
     *
     * @param charSequence The {@link CharSequence} to examine.
     * @return The longest tokens of accepted input.
     */
    List<Token> getLongestTokens(CharSequence sequence);

    /**
     * Performs the same operation as {@link #getLongestTokens(CharSequence)} but on the part of <code>sequence</code> denoted by
     * <code>start</code> and <code>length</code>.
     *
     * @see #getLongestTokens(CharSequence)
     */
    List<Token> getLongestTokens(CharSequence sequence, int start, int length);

}
