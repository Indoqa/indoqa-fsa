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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.indoqa.fsa.AcceptorBuilder;

public class CharAcceptorBuilder implements AcceptorBuilder {

    public static final int FILE_VERSION = 2;
    public static final int DEFAULT_CAPACITY_INCREMENT = 16 * 1024;

    private static final float LOAD_FACTOR = 0.9f;

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

        int fileVersion = dataInputStream.readInt();
        if (FILE_VERSION != fileVersion) {
            throw new IllegalArgumentException("Invalid file version. Expected " + FILE_VERSION + ", but found " + fileVersion + ".");
        }

        boolean caseSensitive = dataInputStream.readBoolean();

        char[] data = new char[dataInputStream.readInt()];
        for (int i = 0; i < data.length; i++) {
            data[i] = dataInputStream.readChar();
        }

        return new CharAcceptor(data, caseSensitive);
    }

    private static <K, V> ConcurrentHashMap<K, V> createHashMap(int size) {
        return new ConcurrentHashMap<>((int) (size / LOAD_FACTOR + 1), LOAD_FACTOR, 1);
    }

    private static String getKey(char[] node) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            if (node.length <= CharDataAccessor.NODE_SIZE * i) {
                break;
            }

            stringBuilder.append(node[CharDataAccessor.NODE_SIZE * i]);
        }

        stringBuilder.append('_');
        stringBuilder.append(node.length);

        return stringBuilder.toString();
    }

    @Override
    public void addAcceptedInput(CharSequence value, int start, int length) {
        this.addAcceptedInput(value, start, length, 0, true);
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

        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
        dataOutputStream.writeInt(FILE_VERSION);
        dataOutputStream.writeBoolean(this.caseSensitive);
        dataOutputStream.writeInt(this.requiredLength);

        this.serialize(dataOutputStream);

        dataOutputStream.flush();
    }

    protected int addAcceptedInput(CharSequence value, int start, int length, int startNode, boolean makeTerminal) {
        if (this.minified || this.remapped) {
            throw new IllegalStateException("The data have already been minified / remapped.");
        }

        int node = startNode;

        for (int i = start; i < start + length; i++) {
            boolean terminal = makeTerminal && i == start + length - 1;

            int arc = CharDataAccessor.getArc(this.nodes[node], 0, value.charAt(i), this.caseSensitive);
            if (arc == -1) {
                this.addArc(node, value.charAt(i), this.nodeCount, terminal);
                node = this.nodeCount;
                this.addNode();
                continue;
            }

            if (terminal) {
                CharDataAccessor.setTerminal(this.nodes[node], arc, true);
                break;
            }

            node = getTarget(this.nodes[node], arc);
        }

        return node;
    }

    protected void addArc(int node, char label, int target, boolean terminal) {
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

            Integer replacement = replacements.get(target);
            if (replacement == null) {
                continue;
            }

            boolean isTerminal = isTerminal(nodeData, i);
            boolean isLast = isLast(nodeData, i);

            CharDataAccessor.setTarget(nodeData, i, replacement);
            CharDataAccessor.setTerminal(nodeData, i, isTerminal);
            CharDataAccessor.setLast(nodeData, i, isLast);

            result = true;
        }

        return result;
    }

    private Set<String> applyReplacements(Map<Integer, Integer> replacements) {
        this.sendMessage("Applying " + replacements.size() + " replacements");

        Set<String> result = new HashSet<>();

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            if (this.applyReplacements(node, replacements)) {
                result.add(getKey(node));
            }
        }

        return result;
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

    private Map<String, List<Integer>> buildGroups() {
        this.sendMessage("Building groups");

        Map<String, List<Integer>> result = createHashMap(1000);

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            String key = getKey(node);

            List<Integer> indexes = result.get(key);
            if (indexes == null) {
                indexes = new LinkedList<>();
                result.put(key, indexes);
            }

            indexes.add(i);
        }

        return result;
    }

    private Map<Integer, Integer> findReplacements(List<Integer> group) {
        Map<Integer, Integer> result = new HashMap<>();

        List<NodeReference> references = new ArrayList<>(group.size());

        for (Iterator<Integer> iterator = group.iterator(); iterator.hasNext();) {
            Integer index = iterator.next();

            char[] node = this.nodes[index];
            if (node == null) {
                iterator.remove();
                continue;
            }

            references.add(new NodeReference(node, index));
        }

        references.sort(null);

        NodeReference lastReference = null;
        for (NodeReference eachReference : references) {
            if (lastReference == null || !eachReference.equals(lastReference)) {
                lastReference = eachReference;
            } else {
                result.put(eachReference.getIndex(), lastReference.getIndex());
                this.nodes[eachReference.getIndex()] = null;
            }
        }

        return result;
    }

    private void minify() {
        if (this.minified) {
            return;
        }

        this.sendMessage("Minifying " + this.nodeCount + " nodes");

        Map<Integer, Integer> replacements = this.replaceEndNodes();
        this.applyReplacements(replacements);

        Map<String, List<Integer>> groups = this.buildGroups();
        Set<String> changedGroups = new HashSet<>(groups.keySet());

        while (true) {
            replacements.clear();

            this.sendMessage("Finding duplicates in " + changedGroups.size() + " groups");
            for (String eachChangedGroup : changedGroups) {
                List<Integer> group = groups.get(eachChangedGroup);
                if (group == null || group.size() < 2) {
                    continue;
                }

                replacements.putAll(this.findReplacements(group));
            }

            if (replacements.isEmpty()) {
                break;
            }

            changedGroups = this.applyReplacements(replacements);
        }

        this.minified = true;
    }

    private void remap() {
        if (this.remapped) {
            return;
        }

        this.sendMessage("Remapping node addresses ...");
        this.requiredLength = 0;

        Map<Integer, Integer> replacements = createHashMap(this.nodeCount);

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

        this.remapped = true;
    }

    private Map<Integer, Integer> replaceEndNodes() {
        Map<Integer, Integer> result = createHashMap(1000);

        for (int i = 0; i < this.nodeCount; i++) {
            if (this.nodes[i] == null || this.nodes[i].length != 0) {
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
