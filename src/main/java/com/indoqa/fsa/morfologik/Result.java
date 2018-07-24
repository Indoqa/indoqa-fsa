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

import java.nio.ByteBuffer;

import com.indoqa.fsa.utils.EncodingUtils;

public class Result {

    private Match match;
    private int matchedLength;
    private int node;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(10);

    public void appendOutput(byte value) {
        if (!this.byteBuffer.hasRemaining()) {
            ByteBuffer allocate = ByteBuffer.allocate(this.byteBuffer.capacity() + 10);
            this.byteBuffer.rewind();
            allocate.put(this.byteBuffer);
            this.byteBuffer = allocate;
        }

        this.byteBuffer.put(value);
    }

    public Match getMatch() {
        return this.match;
    }

    public String getMatched(byte[] bytes, int offset) {
        return EncodingUtils.getString(bytes, offset, this.matchedLength);
    }

    public int getMatchedLength() {
        return this.matchedLength;
    }

    public int getNode() {
        return this.node;
    }

    public boolean isTerminalMatch() {
        return this.match == Match.EXACT_MATCH || this.match == Match.PARTIAL_MATCH;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public void setMatchedLength(int matchedLength) {
        this.matchedLength = matchedLength;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public enum Match {
        NO_MATCH, EXACT_MATCH, PARTIAL_MATCH, NON_TERMINAL_MATCH;
    }
}
