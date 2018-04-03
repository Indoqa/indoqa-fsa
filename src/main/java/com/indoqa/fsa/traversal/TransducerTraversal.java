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
package com.indoqa.fsa.traversal;

import static com.indoqa.fsa.traversal.Result.Match.*;

import morfologik.fsa.FSA;
import morfologik.fsa.MatchResult;

public class TransducerTraversal {

    /**
     * Target automaton.
     */
    private final FSA fsa;
    private final byte separator;
    private final int rootNode;

    /**
     * Traversals of the given FSA.
     *
     * @param fsa The target automaton for traversals.
     */
    public TransducerTraversal(FSA fsa, byte separator) {
        this.fsa = fsa;
        this.separator = separator;
        this.rootNode = fsa.getRootNode();
    }

    public int getRootNode() {
        return this.rootNode;
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
    public Result match(byte[] sequence, int start, int length) {
        return this.match(new Result(), sequence, start, length);
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
    public Result match(Result reuse, byte[] sequence, int start, int length) {
        int node = this.rootNode;

        reuse.setMatch(NO_MATCH);
        reuse.setMatchedLength(0);

        int currentNode = node;
        int end = start + length;

        for (int i = start; i < end; i++) {
            int arc = this.fsa.getArc(currentNode, sequence[i]);
            if (arc == 0) {
                break;
            }

            if (this.fsa.isArcTerminal(arc)) {
                break;
            }

            currentNode = this.fsa.getEndNode(arc);

            arc = this.fsa.getArc(currentNode, this.separator);
            if (arc != 0) {
                reuse.setMatchedLength(i - start + 1);
                reuse.setNode(this.fsa.getEndNode(arc));
                reuse.setMatch(NON_TERMINAL_MATCH);
            }
        }

        return reuse;
    }
}
