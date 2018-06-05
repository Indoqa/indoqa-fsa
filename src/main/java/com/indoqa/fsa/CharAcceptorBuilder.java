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

import static com.indoqa.fsa.CharDataAccessor.*;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CharAcceptorBuilder implements AcceptorBuilder {

    static final int DEFAULT_CAPACITY_INCREMENT = 16 * 1024;

    private final boolean caseSensitive;

    private char[][] nodes = new char[0][];
    private int nodeCount;
    private int capacityIncrement;

    public CharAcceptorBuilder(boolean caseSensitive) {
        this(caseSensitive, DEFAULT_CAPACITY_INCREMENT);
    }

    public CharAcceptorBuilder(boolean caseSensitive, int capacityIncrement) {
        super();

        this.caseSensitive = caseSensitive;
        this.capacityIncrement = capacityIncrement;
        this.addNode();
    }

    public static CharAcceptor build(boolean caseSensitive, CharSequence... input) {
        return build(caseSensitive, Arrays.asList(input));
    }

    public static CharAcceptor build(boolean caseSensitive, Iterable<? extends CharSequence> input) {
        CharAcceptorBuilder builder = new CharAcceptorBuilder(caseSensitive);
        builder.addAcceptedInput(input);
        return builder.build();
    }

    public static CharAcceptor read(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);

        boolean caseSensitive = dataInputStream.readBoolean();

        char[] data = new char[dataInputStream.readInt()];
        for (int i = 0; i < data.length; i++) {
            data[i] = dataInputStream.readChar();
        }

        return new CharAcceptor(data, caseSensitive);
    }

    @Override
    public void addAcceptedInput(CharSequence... value) {
        this.addAcceptedInput(Arrays.asList(value));
    }

    @Override
    public void addAcceptedInput(Iterable<? extends CharSequence> value) {
        for (CharSequence eachValue : value) {
            int node = 0;

            for (int i = 0; i < eachValue.length(); i++) {
                int arc = CharDataAccessor.getArc(this.nodes[node], 0, eachValue.charAt(i), this.caseSensitive);
                if (arc != -1) {
                    node = getTarget(this.nodes[node], arc);
                    continue;
                }

                this.addArc(node, eachValue.charAt(i), this.nodeCount, i == eachValue.length() - 1);
                node = this.nodeCount;
                this.addNode();
            }
        }
    }

    @Override
    public CharAcceptor build() {
        char[] data = this.buildData();
        return new CharAcceptor(data, this.caseSensitive);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        char[] data = this.buildData();

        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeBoolean(this.caseSensitive);
        dataOutputStream.writeInt(data.length);

        for (char eachChar : data) {
            dataOutputStream.writeChar(eachChar);
        }

        dataOutputStream.flush();
    }

    private void addArc(int node, char label, int target, boolean terminal) {
        char[] oldNodeData = this.nodes[node];

        this.nodes[node] = new char[oldNodeData.length + CharDataAccessor.NODE_SIZE];
        char[] newNodeData = this.nodes[node];

        int insertIndex = 0;
        for (int i = 0; i < oldNodeData.length; i += CharDataAccessor.NODE_SIZE) {
            if (CharDataAccessor.getLabel(oldNodeData, i) < label) {
                insertIndex = i + CharDataAccessor.NODE_SIZE;
            }
        }

        System.arraycopy(oldNodeData, 0, newNodeData, 0, insertIndex);
        if (oldNodeData.length > insertIndex) {
            System.arraycopy(oldNodeData, insertIndex, newNodeData, insertIndex + CharDataAccessor.NODE_SIZE,
                oldNodeData.length - insertIndex);
        }

        CharDataAccessor.setLabel(this.nodes[node], insertIndex, label);
        CharDataAccessor.setTarget(this.nodes[node], insertIndex, target);

        if (terminal) {
            CharDataAccessor.setTerminal(this.nodes[node], insertIndex, terminal);
        }

        if (insertIndex < oldNodeData.length) {
            return;
        }

        CharDataAccessor.setLast(this.nodes[node], newNodeData.length - CharDataAccessor.NODE_SIZE, true);
        if (newNodeData.length > CharDataAccessor.NODE_SIZE) {
            CharDataAccessor.setLast(this.nodes[node], newNodeData.length - 2 * CharDataAccessor.NODE_SIZE, false);
        }
    }

    private void addNode() {
        if (this.nodeCount + 1 >= this.nodes.length) {
            char[][] newData = new char[this.nodes.length + this.capacityIncrement][];
            System.arraycopy(this.nodes, 0, newData, 0, this.nodes.length);
            this.nodes = newData;
        }

        this.nodes[this.nodeCount] = new char[0];
        this.nodeCount++;
    }

    private boolean applyReplacements(char[] nodeData, Map<Integer, Integer> replacements) {
        boolean result = false;

        for (int i = 0; i < nodeData.length; i += CharDataAccessor.NODE_SIZE) {
            int target = getTarget(nodeData, i);

            if (!replacements.containsKey(target)) {
                continue;
            }

            int replacement = replacements.get(target);

            boolean isTerminal = isTerminal(nodeData, i);
            boolean isLast = isLast(nodeData, i);

            CharDataAccessor.setTarget(nodeData, i, replacement);
            CharDataAccessor.setTerminal(nodeData, i, isTerminal);
            CharDataAccessor.setLast(nodeData, i, isLast);

            result = true;
        }

        return result;
    }

    private char[] buildData() {
        this.minify();

        int offset = 0;

        Map<Integer, Integer> replacements = new HashMap<>();

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            replacements.put(i, offset);
            offset += node.length;
        }

        char[] result = new char[offset];
        offset = 0;

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            this.applyReplacements(node, replacements);

            System.arraycopy(node, 0, result, offset, node.length);
            offset += node.length;
        }

        return result;
    }

    private void minify() {
        Map<Integer, Integer> replacements = new HashMap<>();
        HashNode hashNode = new HashNode();

        while (true) {
            replacements.clear();

            for (int i = 0; i < this.nodeCount; i++) {
                char[] node = this.nodes[i];
                if (node == null) {
                    continue;
                }

                int previous = hashNode.addIfMissing(node, i);
                if (previous == -1 || previous == i) {
                    continue;
                }

                replacements.put(i, previous);
                this.nodes[i] = null;
            }

            if (replacements.isEmpty()) {
                break;
            }

            for (int i = 0; i < this.nodeCount; i++) {
                char[] node = this.nodes[i];
                if (node == null) {
                    continue;
                }

                this.applyReplacements(node, replacements);
            }
        }
    }
}
