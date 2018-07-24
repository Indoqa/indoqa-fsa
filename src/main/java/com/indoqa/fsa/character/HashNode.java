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

import java.util.HashMap;
import java.util.Map;

public class HashNode {

    private int index = -1;

    private Map<Character, HashNode> children = new HashMap<>();

    public void add(char[] data, int index) {
        this.add(data, 0, index);
    }

    public int addIfMissing(char[] data, int index) {
        return this.addIfMissing(data, index, 0);
    }

    public void clear() {
        this.children.clear();
    }

    public int getMatchingIndex(char[] data) {
        return this.getMatchingIndex(data, 0);
    }

    private void add(char[] data, int offset, int index) {
        if (offset == data.length) {
            this.index = index;
            return;
        }

        HashNode child = this.children.get(data[offset]);
        if (child != null) {
            child.add(data, offset + 1, index);
            return;
        }

        child = new HashNode();
        this.children.put(data[offset], child);
        child.add(data, offset + 1, index);
    }

    private int addIfMissing(char[] data, int index, int offset) {
        if (offset == data.length) {
            if (this.index != -1) {
                return this.index;
            }

            this.index = index;
            return -1;
        }

        HashNode child = this.children.get(data[offset]);
        if (child != null) {
            return child.addIfMissing(data, index, offset + 1);
        }

        child = new HashNode();
        this.children.put(data[offset], child);
        return child.addIfMissing(data, index, offset + 1);
    }

    private int getMatchingIndex(char[] data, int offset) {
        if (offset == data.length) {
            return this.index;
        }

        HashNode child = this.children.get(data[offset]);
        if (child != null) {
            return child.getMatchingIndex(data, offset + 1);
        }

        return -1;
    }
}
