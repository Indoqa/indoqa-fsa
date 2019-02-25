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

import java.util.Arrays;

public class NodeReference implements Comparable<NodeReference> {

    private final char[] data;
    private final Integer index;

    public NodeReference(char[] data, Integer index) {
        super();
        this.data = data;
        this.index = index;
    }

    @Override
    public int compareTo(NodeReference other) {
        for (int i = 0; i < this.data.length; i++) {
            if (other.data.length <= i) {
                return 1;
            }

            if (this.data[i] < other.data[i]) {
                return -1;
            }

            if (this.data[i] > other.data[i]) {
                return 1;
            }
        }

        if (other.data.length > this.data.length) {
            return -1;
        }

        return this.index.compareTo(other.index);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        NodeReference other = (NodeReference) obj;
        return Arrays.equals(this.data, other.data);
    }

    public Integer getIndex() {
        return this.index;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.data);
    }
}
