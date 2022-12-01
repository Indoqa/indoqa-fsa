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
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.indoqa.fsa.AcceptorBuilder;

public class CharAcceptorBuilder implements AcceptorBuilder {

    public static final int FILE_VERSION = 2;
    public static final int DEFAULT_CAPACITY_INCREMENT = 16 * 1024;
    public static final int DEFAULT_SHRINK_LIMIT = 1_000;

    private final boolean caseSensitive;

    private char[][] nodes = new char[0][];
    private int nodeCount;
    private final int capacityIncrement;
    private final int shrinkLimit;

    private Replacements replacements;

    private Consumer<String> messageConsumer;
    private BooleanSupplier abortSupplier;

    private boolean minified;
    private boolean remapped;
    private int requiredLength;

    public CharAcceptorBuilder(boolean caseSensitive) {
        this(caseSensitive, DEFAULT_CAPACITY_INCREMENT, DEFAULT_SHRINK_LIMIT);
    }

    public CharAcceptorBuilder(boolean caseSensitive, int capacityIncrement, int shrinkLimit) {
        super();

        this.caseSensitive = caseSensitive;
        this.capacityIncrement = capacityIncrement;
        this.shrinkLimit = shrinkLimit;
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

    public static CharAcceptor empty() {
        return build(true);
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

    private static String formatNumber(long number) {
        return NumberFormat.getInstance(Locale.ENGLISH).format(number);
    }

    private static String getKey(char[] node) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < node.length; i += CharDataAccessor.NODE_SIZE) {
            stringBuilder.append(CharDataAccessor.getLabel(node, i));
        }

        return stringBuilder.toString();
    }

    private static int sort(NodeReference n1, NodeReference n2) {
        if (n1 == null && n2 == null) {
            return 0;
        }

        if (n1 == null) {
            return 1;
        }

        if (n2 == null) {
            return -1;
        }

        return n1.compareTo(n2);
    }

    @Override
    public void addAcceptedInput(CharSequence value, int start, int length) {
        this.addAcceptedInput(value, start, length, 0, true);
    }

    @Override
    public CharAcceptor build() {
        this.replacements = new Replacements(this.nodeCount);
        this.minify();
        this.remap();
        this.replacements = null;

        char[] data = this.buildData();
        return new CharAcceptor(data, this.caseSensitive);
    }

    public void setAbortSupplier(BooleanSupplier abortSupplier) {
        this.abortSupplier = abortSupplier;
    }

    public void setMessageConsumer(Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        this.replacements = new Replacements(this.nodeCount);
        this.minify();
        this.remap();
        this.replacements = null;

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

    private boolean applyReplacements(char[] nodeData) {
        boolean result = false;

        for (int i = 0; i < nodeData.length; i += CharDataAccessor.NODE_SIZE) {
            int target = getTarget(nodeData, i);

            int replacement = this.replacements.getReplacement(target);
            if (replacement == -1) {
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

    private void applyReplacements(Set<String> changedGroups) {
        this.sendMessage("Applying " + formatNumber(this.replacements.getCount()) + " replacements");
        if (changedGroups != null) {
            changedGroups.clear();
        }

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            boolean updated = this.applyReplacements(node);
            if (updated && changedGroups != null) {
                changedGroups.add(getKey(node));
            }
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

            this.checkAborted();

            System.arraycopy(node, 0, data, offset, node.length);
            offset += node.length;
        }

        return data;
    }

    private Map<String, List<NodeReference>> buildGroups() {
        Map<String, List<NodeReference>> result = new HashMap<>();

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            String key = getKey(node);

            List<NodeReference> indexes = result.get(key);
            if (indexes == null) {
                indexes = new ArrayList<>();
                result.put(key, indexes);
            }

            indexes.add(new NodeReference(node, i));
        }

        for (Iterator<Entry<String, List<NodeReference>>> iterator = result.entrySet().iterator(); iterator.hasNext();) {
            if (iterator.next().getValue().size() < 2) {
                iterator.remove();
            }
        }

        return result;
    }

    private void checkAborted() {
        if (this.abortSupplier != null && this.abortSupplier.getAsBoolean()) {
            throw new AbortedException();
        }
    }

    private void findEndNodeReplacements() {
        for (int i = 0; i < this.nodeCount; i++) {
            if (this.nodes[i] == null || this.nodes[i].length != 0) {
                continue;
            }

            this.nodes[i] = null;
            this.replacements.setReplacement(i, 0);
        }
    }

    private void findReplacements(List<NodeReference> group) {
        group.sort(CharAcceptorBuilder::sort);

        NodeReference lastReference = null;
        for (ListIterator<NodeReference> iterator = group.listIterator(); iterator.hasNext();) {
            NodeReference reference = iterator.next();
            if (reference == null) {
                break;
            }

            if (lastReference == null || !reference.equals(lastReference)) {
                lastReference = reference;
                continue;
            }

            this.replacements.setReplacement(reference.getIndex(), lastReference.getIndex());
            this.nodes[reference.getIndex()] = null;
            iterator.set(null);
        }
    }

    private long getSize() {
        long length = 0;

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            if (node.length != 0) {
                length += node.length;
            }
        }

        return length;
    }

    private void minify() {
        if (this.minified) {
            return;
        }

        long start = System.currentTimeMillis();
        long previousSize = this.getSize();
        this.sendMessage("Minifying " + formatNumber(this.nodeCount) + " nodes");

        Map<String, List<NodeReference>> groups = this.buildGroups();
        Set<String> changedGroups = new HashSet<>();

        this.findEndNodeReplacements();
        this.applyReplacements(changedGroups);

        while (true) {
            this.replacements.clear();

            for (String eachChangedGroup : changedGroups) {
                this.checkAborted();

                List<NodeReference> group = groups.get(eachChangedGroup);
                if (group == null) {
                    continue;
                }

                this.findReplacements(group);
            }

            if (this.replacements.isEmpty()) {
                break;
            }

            this.applyReplacements(changedGroups);

            long size = this.getSize();
            long shrunk = previousSize - size;
            long minShrink = previousSize / this.shrinkLimit;
            this.sendMessage(
                "Shrunk size by " + formatNumber(shrunk) + " (from " + formatNumber(previousSize) + " to " + formatNumber(size) + "), "
                    + formatNumber((System.currentTimeMillis() - start) / 1_000) + " seconds");

            if (shrunk < minShrink) {
                this.sendMessage(
                    "Shrinking step smaller than limit (" + formatNumber(shrunk) + " < " + formatNumber(minShrink)
                        + "). Aborting ...");
                break;
            }

            previousSize = size;
        }

        long duration = System.currentTimeMillis() - start;
        this.sendMessage("Minified in " + formatNumber(duration / 1_000) + " seconds");

        this.minified = true;
    }

    private void remap() {
        if (this.remapped) {
            return;
        }

        this.sendMessage("Remapping node addresses ...");
        this.requiredLength = 0;

        for (int i = 0; i < this.nodeCount; i++) {
            char[] node = this.nodes[i];
            if (node == null) {
                continue;
            }

            if (node.length == 0) {
                this.replacements.setReplacement(i, 0);
            } else {
                this.replacements.setReplacement(i, this.requiredLength);
                this.requiredLength += node.length;
            }
        }

        this.sendMessage("Required length " + formatNumber(this.requiredLength));

        this.applyReplacements((Set<String>) null);

        this.remapped = true;
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

            this.checkAborted();

            for (char eachChar : node) {
                outputStream.writeChar(eachChar);
            }
        }
    }
}
