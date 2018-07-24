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

import static com.indoqa.fsa.morfologik.Result.Match.*;

import com.indoqa.fsa.morfologik.Result.Match;

import morfologik.fsa.FSA;
import morfologik.fsa.MatchResult;

public class PrefixFSATraversal {

    /**
     * Target automaton.
     */
    private final FSA fsa;
    private final int rootNode;

    /**
     * Traversals of the given FSA.
     *
     * @param fsa The target automaton for traversals.
     */
    public PrefixFSATraversal(FSA fsa) {
        this.fsa = fsa;
        this.rootNode = fsa.getRootNode();
    }

    public int getRootNode() {
        return this.rootNode;
    }

    /**
     * @param sequence Input sequence to look for in the automaton.
     *
     * @see #match(byte [], int)
     * @return {@link MatchResult} with updated match {@link MatchResult#kind}.
     */
    public Result match(byte[] sequence) {
        return this.match(sequence, this.rootNode);
    }

    /**
     * @param sequence Input sequence to look for in the automaton.
     * @param node The node to start traversal from, typically the {@linkplain FSA#getRootNode() root node}.
     *
     * @see #match(byte [], int)
     * @return {@link MatchResult} with updated match {@link MatchResult#kind}.
     */
    public Result match(byte[] sequence, int node) {
        return this.match(sequence, 0, sequence.length, node);
    }

    /**
     * Finds a matching path in the dictionary for a given sequence of labels from <code>sequence</code> and starting at node
     * <code>node</code>.
     *
     * @param sequence Input sequence to look for in the automaton.
     * @param start Start index in the sequence array.
     * @param length Length of the byte sequence, must be at least 1.
     * @param node The node to start traversal from, typically the {@linkplain FSA#getRootNode() root node}.
     *
     * @see #match(byte [], int)
     * @return {@link MatchResult} with updated match {@link MatchResult#kind}.
     */
    public Result match(byte[] sequence, int start, int length, int node) {
        return this.match(new Result(), sequence, start, length, node);
    }

    /**
     * Same as {@link #match(byte[], int, int, int)}, but allows passing a reusable {@link MatchResult} object so that no intermediate
     * garbage is produced.
     *
     * @param reuse The {@link MatchResult} to reuse.
     * @param sequence Input sequence to look for in the automaton.
     * @param start Start index in the sequence array.
     * @param length Length of the byte sequence, must be at least 1.
     * @param node The node to start traversal from, typically the {@linkplain FSA#getRootNode() root node}.
     *
     * @return The same object as <code>reuse</code>, but with updated match {@link MatchResult#kind} and other relevant fields.
     */
    public Result match(Result reuse, byte[] sequence, int start, int length, int node) {
        if (node == 0) {
            reuse.setMatch(NO_MATCH);
            return reuse;
        }

        reuse.setMatchedLength(0);

        int currentNode = node;
        int end = start + length;
        int lastFinalLength = 0;

        for (int i = start; i < end; i++) {
            int arc = this.fsa.getArc(currentNode, sequence[i]);
            if (arc == 0) {
                break;
            }

            if (this.fsa.isArcFinal(arc)) {
                lastFinalLength = i - start + 1;

                if (this.fsa.isArcTerminal(arc)) {
                    break;
                }
            }

            currentNode = this.fsa.getEndNode(arc);
            reuse.setNode(currentNode);
            reuse.setMatchedLength(i - start + 1);
        }

        if (node == currentNode) {
            reuse.setMatch(NO_MATCH);
        } else if (lastFinalLength == length) {
            reuse.setMatchedLength(lastFinalLength);
            reuse.setMatch(Match.EXACT_MATCH);
        } else if (lastFinalLength > 0) {
            reuse.setMatchedLength(lastFinalLength);
            reuse.setMatch(PARTIAL_MATCH);
        } else {
            reuse.setMatch(NON_TERMINAL_MATCH);
        }

        return reuse;
    }
}
