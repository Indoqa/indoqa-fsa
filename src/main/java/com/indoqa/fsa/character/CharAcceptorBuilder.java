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

import static com.indoqa.fsa.character.CharDataAccessor.*;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.indoqa.fsa.AcceptorBuilder;
import com.indoqa.fsa.utils.IntList;
import com.indoqa.fsa.utils.NodeData;

public class CharAcceptorBuilder implements AcceptorBuilder {

    static final int DEFAULT_CAPACITY_INCREMENT = 16 * 1024;

    private final boolean caseSensitive;

    private char[][] nodes = new char[0][];
    private int nodeCount;
    private int capacityIncrement;

    private Consumer<String> messageConsumer;

    private boolean minified;
    private boolean remapped;
    private int requiredLength;

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

    private static String getKey(char[] node) {
        StringBuilder stringBuilder = new StringBuilder();

        if (node.length > 0) {
            stringBuilder.append(node[0]);
        }

        if (node.length > CharDataAccessor.NODE_SIZE) {
            stringBuilder.append(node[CharDataAccessor.NODE_SIZE]);
        }

        if (node.length > CharDataAccessor.NODE_SIZE * 2) {
            stringBuilder.append(node[CharDataAccessor.NODE_SIZE * 2]);
        }

        stringBuilder.append('_');
        stringBuilder.append(node.length);

        return stringBuilder.toString();
    }

    @Override
    public void addAcceptedInput(CharSequence... value) {
        for (CharSequence eachValue : value) {
            this.addAcceptedInput(eachValue);
        }
    }

    public void addAcceptedInput(CharSequence value) {
        this.addAcceptedInput(value, 0, value.length());
    }

    public void addAcceptedInput(CharSequence value, int start, int length) {
        if (this.minified || this.remapped) {
            throw new IllegalStateException("The data have already been minified / remapped.");
        }

        int node = 0;

        for (int i = start; i < start + length; i++) {
            boolean lastChar = i == start + length - 1;

            int arc = CharDataAccessor.getArc(this.nodes[node], 0, value.charAt(i), this.caseSensitive);
            if (arc == -1) {
                this.addArc(node, value.charAt(i), this.nodeCount, lastChar);
                node = this.nodeCount;
                this.addNode();
                continue;
            }

            if (lastChar) {
                CharDataAccessor.setTerminal(this.nodes[node], arc, true);
                break;
            }

            node = getTarget(this.nodes[node], arc);
        }
    }

    @Override
    public void addAcceptedInput(Iterable<? extends CharSequence> value) {
        for (CharSequence eachValue : value) {
            this.addAcceptedInput(eachValue);
        }
    }

    @Override
    public CharAcceptor build() {
        this.minify();
        this.remap();

        char[] data = this.buildData();
        return new CharAcceptor(data, this.caseSensitive);
    }

    public void setMessageConsumer(Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        this.minify();
        this.remap();

        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeBoolean(this.caseSensitive);
        dataOutputStream.writeInt(this.requiredLength);

        this.serialize(dataOutputStream);

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
            System.arraycopy(
                oldNodeData,
                insertIndex,
                newNodeData,
                insertIndex + CharDataAccessor.NODE_SIZE,
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

    private void applyReplacements(Map<Integer, Integer> replacements) {
        this.sendMessage("Applying " + replacements.size() + " replacements");

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            this.applyReplacements(node, replacements);
        }
    }

    private char[] buildData() {
        char[] data = new char[this.requiredLength];
        int offset = 0;

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            System.arraycopy(node, 0, data, offset, node.length);
            offset += node.length;
        }
        return data;
    }

    private Map<String, IntList> buildGroups() {
        Map<String, IntList> result = new TreeMap<>();

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            String key = getKey(node);

            IntList indexes = result.get(key);
            if (indexes == null) {
                indexes = new IntList();
                result.put(key, indexes);
            }

            indexes.add(i);
        }

        return result;
    }

    private Map<Integer, Integer> findReplacements(IntList group) {
        Map<Integer, Integer> result = new HashMap<>();

        Map<NodeData, Integer> hashes = new HashMap<>(group.size());

        for (int i = 0; i < group.size(); i++) {
            int eachIndex = group.get(i);
            char[] node = this.nodes[eachIndex];
            if (node == null) {
                continue;
            }

            Integer previous = hashes.putIfAbsent(new NodeData(node), eachIndex);

            if (previous != null) {
                result.put(eachIndex, previous);
            }
        }

        return result;
    }

    private void minify() {
        if (this.minified) {
            return;
        }

        this.sendMessage("Minifying " + this.nodeCount + " nodes ...");

        Map<Integer, Integer> replacements = this.replaceEndNodes();
        this.applyReplacements(replacements);

        Map<String, IntList> groups = this.buildGroups();

        while (true) {
            replacements.clear();

            for (IntList eachGroup : groups.values()) {
                if (eachGroup.size() < 2) {
                    continue;
                }

                Map<Integer, Integer> groupReplacements = this.findReplacements(eachGroup);
                for (Entry<Integer, Integer> eachReplacement : groupReplacements.entrySet()) {
                    this.nodes[eachReplacement.getKey()] = null;
                }

                replacements.putAll(groupReplacements);
            }

            if (replacements.isEmpty()) {
                break;
            }

            this.applyReplacements(replacements);
        }

        this.minified = true;
    }

    private void remap() {
        if (this.remapped) {
            return;
        }

        this.sendMessage("Remapping node addresses ...");
        this.requiredLength = 0;

        Map<Integer, Integer> replacements = new HashMap<>();

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            if (node.length == 0) {
                replacements.put(i, 0);
            } else {
                replacements.put(i, this.requiredLength);
                this.requiredLength += node.length;
            }
        }

        this.applyReplacements(replacements);

        this.remapped = true;
    }

    private Map<Integer, Integer> replaceEndNodes() {
        Map<Integer, Integer> result = new HashMap<>();

        for (int i = 0; i < this.nodeCount; i++) {
            if (this.nodes[i].length != 0) {
                continue;
            }

            this.nodes[i] = null;
            result.put(i, 0);
        }

        return result;
    }

    private void sendMessage(String message) {
        if (this.messageConsumer != null) {
            this.messageConsumer.accept(message);
        }
    }

    private void serialize(DataOutputStream outputStream) throws IOException {
        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            for (char eachChar : node) {
                outputStream.writeChar(eachChar);
            }
        }
    }
}
