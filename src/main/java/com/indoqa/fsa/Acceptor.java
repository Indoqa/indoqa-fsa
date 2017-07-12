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

import java.nio.ByteBuffer;

import morfologik.fsa.FSA;
import morfologik.fsa.FSATraversal;
import morfologik.fsa.MatchResult;

public class Acceptor {

    private final FSA fsa;
    private final FSATraversal fsaTraversal;

    private final ObjectPool<MatchResult> matchResults = new ObjectPool<>(10);
    private final ObjectPool<ByteBuffer> byteBuffers = new ObjectPool<>(10);

    public Acceptor(FSA fsa) {
        this.fsa = fsa;
        this.fsaTraversal = new FSATraversal(fsa);
    }

    public boolean accepts(CharSequence value) {
        if (value == null) {
            return false;
        }

        MatchResult matchResult = this.getMatchResult();
        ByteBuffer byteBuffer = EncodingUtils.encode(value, this.getByteBuffer());
        byteBuffer.flip();

        try {
            MatchResult match = this.fsaTraversal.match(matchResult, byteBuffer.array(), 0, byteBuffer.limit(),
                this.fsa.getRootNode());
            return match.kind == MatchResult.EXACT_MATCH;
        } finally {
            this.poolMatchResult(matchResult);
            this.poolByteBuffer(byteBuffer);
        }
    }

    private ByteBuffer getByteBuffer() {
        ByteBuffer result = this.byteBuffers.get();

        if (result == null) {
            result = ByteBuffer.allocate(1024);
        } else {
            result.clear();
        }

        return result;
    }

    private MatchResult getMatchResult() {
        MatchResult result = this.matchResults.get();

        if (result == null) {
            result = new MatchResult();
        }

        return result;
    }

    private void poolByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffers.put(byteBuffer);
    }

    private void poolMatchResult(MatchResult matchResult) {
        this.matchResults.put(matchResult);
    }
}
