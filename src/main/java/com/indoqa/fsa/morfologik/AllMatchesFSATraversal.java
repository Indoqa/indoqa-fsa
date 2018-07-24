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

import java.util.Arrays;

import morfologik.fsa.FSA;

public class AllMatchesFSATraversal {

    private final FSA fsa;
    private final int rootNode;

    public AllMatchesFSATraversal(FSA fsa) {
        super();
        this.fsa = fsa;
        this.rootNode = fsa.getRootNode();
    }

    public int[] getAllMatches(byte[] input, int start, int length) {
        int[] result = new int[length];
        int resultCount = 0;

        int currentNode = this.rootNode;
        int end = start + length;

        for (int i = start; i < end; i++) {
            int arc = this.fsa.getArc(currentNode, input[i]);
            if (arc == 0) {
                break;
            }

            if (this.fsa.isArcFinal(arc)) {
                result[resultCount++] = i + 1;

                if (this.fsa.isArcTerminal(arc)) {
                    break;
                }
            }

            currentNode = this.fsa.getEndNode(arc);
        }

        return Arrays.copyOf(result, resultCount);
    }
}
