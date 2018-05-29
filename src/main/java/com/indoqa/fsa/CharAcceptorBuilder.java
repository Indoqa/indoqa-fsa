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

import static com.indoqa.fsa.CharAcceptor.NODE_SIZE;

import java.io.*;
import java.util.*;

public class CharAcceptorBuilder implements AcceptorBuilder {

    private final List<Node> nodes = new ArrayList<>();
    private final boolean caseSensitive;

    public CharAcceptorBuilder(boolean caseSensitive) {
        super();

        this.caseSensitive = caseSensitive;
        this.nodes.add(new Node(caseSensitive));
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
            Node node = this.nodes.get(0);

            for (int i = 0; i < eachValue.length(); i++) {
                int target = node.getTarget(eachValue.charAt(i));
                if (target != -1) {
                    node = this.nodes.get(target);
                    continue;
                }

                node.add(eachValue.charAt(i), this.nodes.size(), i == eachValue.length() - 1);
                node = new Node(this.caseSensitive);
                this.nodes.add(node);
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

    private char[] buildData() {
        this.minify();

        int offset = 0;

        Map<Integer, Integer> replacements = new HashMap<>();

        for (int i = 0; i < this.nodes.size(); i++) {
            Node node = this.nodes.get(i);
            if (node == null) {
                continue;
            }

            replacements.put(i, offset);
            offset += node.data.length;
        }

        char[] result = new char[offset];
        offset = 0;
        for (Node eachNode : this.nodes) {
            if (eachNode == null) {
                continue;
            }

            eachNode.applyReplacements(replacements);

            char[] data = eachNode.data;

            while (result.length < offset + data.length) {
                char[] newResult = new char[result.length + 1024];
                System.arraycopy(result, 0, newResult, 0, result.length);
                result = newResult;
            }

            System.arraycopy(data, 0, result, offset, data.length);
            offset += data.length;
        }
        return result;
    }

    private void minify() {
        Map<Integer, Integer> replacements = new HashMap<>();
        Map<String, Integer> hashCodes = new HashMap<>();

        while (true) {
            hashCodes.clear();
            replacements.clear();

            for (int i = 0; i < this.nodes.size(); i++) {
                Node node = this.nodes.get(i);
                if (node == null) {
                    continue;
                }

                String hashCode = node.getDataHashCode();
                if (hashCode.length() == 0) {
                    replacements.put(i, -1);
                    this.nodes.set(i, null);
                    continue;
                }

                Integer previous = hashCodes.putIfAbsent(hashCode, i);
                if (previous != null) {
                    Node previousNode = this.nodes.get(previous);
                    if (!previousNode.hasSameData(node)) {
                        continue;
                    }

                    replacements.put(i, previous);
                    this.nodes.set(i, null);
                }
            }

            if (replacements.isEmpty()) {
                break;
            }

            for (int i = 0; i < this.nodes.size(); i++) {
                Node eachNode = this.nodes.get(i);
                if (eachNode == null) {
                    continue;
                }
                eachNode.applyReplacements(replacements);
            }
        }
    }

    private static class Node {

        private char[] data = new char[0];
        private String dataHashCode;
        private final boolean caseSensitive;

        public Node(boolean caseSensitive) {
            super();
            this.caseSensitive = caseSensitive;
        }

        public void add(char label, int target, boolean terminal) {
            char[] newData = new char[this.data.length + NODE_SIZE];

            int insertIndex = 0;
            for (int i = 0; i < this.data.length; i += NODE_SIZE) {
                if (CharAcceptor.getLabel(this.data, i) < label) {
                    insertIndex = i + NODE_SIZE;
                }
            }

            System.arraycopy(this.data, 0, newData, 0, insertIndex);
            if (this.data.length > insertIndex) {
                System.arraycopy(this.data, insertIndex, newData, insertIndex + NODE_SIZE, this.data.length - insertIndex);
            }

            this.data = newData;
            this.dataHashCode = null;

            this.setLabel(insertIndex, label);
            this.setTarget(insertIndex, target);
            this.setTerminal(insertIndex, terminal);

            for (int i = 0; i < this.data.length; i += NODE_SIZE) {
                this.setLast(i, i == this.data.length - NODE_SIZE);
            }
        }

        public boolean applyReplacements(Map<Integer, Integer> replacements) {
            boolean result = false;

            for (int i = 0; i < this.data.length; i += NODE_SIZE) {
                int target = CharAcceptor.getTarget(this.data, i);

                if (!replacements.containsKey(target)) {
                    continue;
                }

                target = replacements.get(target);

                boolean isTerminal = CharAcceptor.isTerminal(this.data, i);
                boolean isLast = CharAcceptor.isLast(this.data, i);

                this.setTarget(i, target);
                this.setTerminal(i, isTerminal);
                this.setLast(i, isLast);

                result = true;
                this.dataHashCode = null;
            }

            return result;
        }

        public String getDataHashCode() {
            if (this.dataHashCode == null) {
                this.dataHashCode = new String(this.data);
            }

            return this.dataHashCode;
        }

        public int getTarget(char label) {
            int arc = CharAcceptor.getArc(this.data, 0, label, this.caseSensitive);
            if (arc != -1) {
                return CharAcceptor.getTarget(this.data, arc);
            }

            return -1;
        }

        public boolean hasSameData(Node otherNode) {
            return this.getDataHashCode().equals(otherNode.getDataHashCode());
        }

        private void setLabel(int index, char label) {
            this.data[index] = label;
        }

        private void setLast(int index, boolean last) {
            if (last) {
                this.data[index + CharAcceptor.FLAGS_OFFSET] |= CharAcceptor.MASK_LAST;
            } else {
                this.data[index + CharAcceptor.FLAGS_OFFSET] &= ~CharAcceptor.MASK_LAST;
            }
        }

        private void setTarget(int index, int target) {
            this.data[index + CharAcceptor.ADDRESS_OFFSET] = (char) (target >> 16 & 0x3FFF);
            this.data[index + CharAcceptor.ADDRESS_OFFSET + 1] = (char) (target & 0xFFFF);
        }

        private void setTerminal(int index, boolean terminal) {
            if (terminal) {
                this.data[index + CharAcceptor.FLAGS_OFFSET] |= CharAcceptor.MASK_TERMINAL;
            } else {
                this.data[index + CharAcceptor.FLAGS_OFFSET] &= ~CharAcceptor.MASK_TERMINAL;
            }
        }
    }
}
