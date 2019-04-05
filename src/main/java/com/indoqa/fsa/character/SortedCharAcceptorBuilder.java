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

import static com.indoqa.fsa.character.CharDataAccessor.getTarget;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.indoqa.fsa.AcceptorBuilder;

public class SortedCharAcceptorBuilder implements AcceptorBuilder {

    public static final int FILE_VERSION = 2;
    public static final int DEFAULT_CAPACITY_INCREMENT = 16 * 1024;

    private final boolean caseSensitive;
    private final int capacityIncrement;

    private final List<Integer> vacantNodes = new LinkedList<>();
    private final Set<String> inputs;
    private Consumer<String> messageConsumer;

    private char[][] nodes;
    private int nodeCount;
    private int requiredLength;

    private Map<String, Integer> foldedNodes;

    public SortedCharAcceptorBuilder(boolean caseSensitive) {
        this(caseSensitive, DEFAULT_CAPACITY_INCREMENT);
    }

    public SortedCharAcceptorBuilder(boolean caseSensitive, int capacityIncrement) {
        super();

        this.caseSensitive = caseSensitive;
        this.capacityIncrement = capacityIncrement;

        if (caseSensitive) {
            this.inputs = new TreeSet<>();
        } else {
            this.inputs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        }
    }

    @Override
    public void addAcceptedInput(CharSequence value, int start, int length) {
        if (this.nodes != null) {
            throw new IllegalStateException("Acceptor has already been built.");
        }

        this.inputs.add(value.subSequence(start, length).toString());
    }

    @Override
    public CharAcceptor build() {
        this.buildNodes();

        char[] data = this.buildData();
        return new CharAcceptor(data, this.caseSensitive);
    }

    public void setMessageConsumer(Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        this.buildNodes();

        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
        dataOutputStream.writeInt(FILE_VERSION);
        dataOutputStream.writeBoolean(this.caseSensitive);
        dataOutputStream.writeInt(this.requiredLength);

        this.serialize(dataOutputStream);

        dataOutputStream.flush();
    }

    protected void addArc(int node, char label, int target, boolean terminal) {
        char[] oldNodeData = this.nodes[node];

        this.nodes[node] = new char[oldNodeData.length + CharDataAccessor.NODE_SIZE];
        char[] newNodeData = this.nodes[node];

        int index = 0;
        for (int i = 0; i < oldNodeData.length; i += CharDataAccessor.NODE_SIZE) {
            if (CharDataAccessor.getLabel(oldNodeData, i) < label) {
                index = i + CharDataAccessor.NODE_SIZE;
            }
        }

        System.arraycopy(oldNodeData, 0, newNodeData, 0, index);
        if (oldNodeData.length > index) {
            System.arraycopy(
                oldNodeData,
                index,
                newNodeData,
                index + CharDataAccessor.NODE_SIZE,
                oldNodeData.length - index);
        }

        CharDataAccessor.setLabel(this.nodes[node], index, label);
        CharDataAccessor.setTarget(this.nodes[node], index, target);

        if (terminal) {
            CharDataAccessor.setTerminal(this.nodes[node], index, terminal);
        }

        if (index < oldNodeData.length) {
            return;
        }

        CharDataAccessor.setLast(this.nodes[node], newNodeData.length - CharDataAccessor.NODE_SIZE, true);
        if (newNodeData.length > CharDataAccessor.NODE_SIZE) {
            CharDataAccessor.setLast(this.nodes[node], newNodeData.length - 2 * CharDataAccessor.NODE_SIZE, false);
        }
    }

    private int addNode() {
        if (!this.vacantNodes.isEmpty()) {
            Integer index = this.vacantNodes.remove(0);
            this.nodes[index] = new char[0];
            return index;
        }

        if (this.nodeCount + 1 >= this.nodes.length) {
            char[][] newData = new char[this.nodes.length + this.capacityIncrement][];
            System.arraycopy(this.nodes, 0, newData, 0, this.nodes.length);
            this.nodes = newData;
        }

        this.nodes[this.nodeCount] = new char[0];
        return this.nodeCount++;
    }

    private void applyReplacement(char[] nodeData, int from, int to) {
        for (int i = 0; i < nodeData.length; i += CharDataAccessor.NODE_SIZE) {
            int target = getTarget(nodeData, i);
            if (target != from) {
                continue;
            }

            CharDataAccessor.setTarget(nodeData, i, to);
        }
    }

    private void applyReplacements(char[] nodeData, Map<Integer, Integer> replacements) {
        for (int i = 0; i < nodeData.length; i += CharDataAccessor.NODE_SIZE) {
            int target = getTarget(nodeData, i);

            Integer replacement = replacements.get(target);
            if (replacement == null) {
                continue;
            }

            CharDataAccessor.setTarget(nodeData, i, replacement);
        }
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

    private void buildNodes() {
        if (this.nodes != null) {
            return;
        }

        this.sendMessage("Building nodes ...");
        this.nodes = new char[0][];
        this.addNode();

        Path lastPath = new Path();
        Path currentPath = new Path();
        this.foldedNodes = new HashMap<>(this.nodeCount);

        int count = 0;

        for (String eachInput : this.inputs) {
            currentPath.clear();
            int node = 0;
            int commonLength = 1;
            currentPath.add(node);

            for (int i = 0; i < eachInput.length(); i++) {
                boolean terminal = i == eachInput.length() - 1;

                int arc = CharDataAccessor.getArc(this.nodes[node], 0, eachInput.charAt(i), this.caseSensitive);
                if (arc == -1) {
                    int nextNode = this.addNode();
                    this.addArc(node, eachInput.charAt(i), nextNode, terminal);
                    node = nextNode;
                    currentPath.add(node);
                    continue;
                }

                if (terminal) {
                    CharDataAccessor.setTerminal(this.nodes[node], arc, true);
                    break;
                }

                node = getTarget(this.nodes[node], arc);
                currentPath.add(node);
                commonLength++;
            }

            this.foldNodes(lastPath, commonLength);
            lastPath.copy(currentPath);

            if (++count % 100_000 == 0) {
                this.sendMessage(count + " of " + this.inputs.size());
            }
        }

        this.foldNodes(lastPath, 0);

        this.remap();
    }

    private void foldNodes(Path path, int commonLength) {
        for (int i = path.length() - 1; i >= commonLength; i--) {
            int nodeIndex = path.get(i);
            char[] node = this.nodes[nodeIndex];

            Integer frozenIndex = this.foldedNodes.putIfAbsent(new String(node), nodeIndex);
            if (frozenIndex == null) {
                continue;
            }

            int previousNodeIndex = path.get(i - 1);
            char[] previousNode = this.nodes[previousNodeIndex];
            this.applyReplacement(previousNode, nodeIndex, frozenIndex);
            this.nodes[nodeIndex] = null;

            this.vacantNodes.add(nodeIndex);
        }
    }

    private void remap() {
        this.sendMessage("Remapping node addresses ...");
        this.requiredLength = 0;

        Map<Integer, Integer> replacements = new ConcurrentHashMap<>();

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

        this.sendMessage("RequiredLength: " + this.requiredLength);

        this.applyReplacements(replacements);
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

    private static class Path {

        private int[] elements = new int[16];
        private int insertIndex = 0;
        private int length = 0;

        public void add(int element) {
            if (this.insertIndex == this.elements.length - 1) {
                this.growCapacity(this.elements.length + 16);
            }

            this.elements[this.insertIndex++] = element;
            this.length++;
        }

        public void clear() {
            this.length = 0;
            this.insertIndex = 0;
        }

        public void copy(Path other) {
            if (this.elements.length < other.elements.length) {
                this.growCapacity(other.elements.length);
            }

            System.arraycopy(other.elements, 0, this.elements, 0, other.length);
            this.length = other.length;
            this.insertIndex = other.insertIndex;
        }

        public int get(int i) {
            return this.elements[i];
        }

        public int length() {
            return this.length;
        }

        private void growCapacity(int newLength) {
            int[] newElements = new int[newLength];
            System.arraycopy(this.elements, 0, newElements, 0, this.elements.length);
            this.elements = newElements;
        }
    }
}
