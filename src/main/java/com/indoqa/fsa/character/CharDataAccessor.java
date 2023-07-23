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

public class CharDataAccessor {

    protected static final int ADDRESS_OFFSET = 1;
    protected static final int FLAGS_OFFSET = 1;

    protected static final int MASK_TERMINAL = 0x8000;
    protected static final int MASK_LAST = 0x4000;
    protected static final int MASK_FLAGS = MASK_TERMINAL | MASK_LAST;
    protected static final int MASK_ADDRESS_LOW = 0xFFFF;
    protected static final int MASK_ADDRESS_HIGH = MASK_ADDRESS_LOW & ~MASK_FLAGS;
    protected static final int NODE_SIZE = 3;

    private static final char[] CASE_INSENSITIVE = new char[Character.MAX_VALUE];
    static {
        for (char value = 0; value < CASE_INSENSITIVE.length; value++) {
            if (Character.isLowerCase(value)) {
                CASE_INSENSITIVE[value] = Character.toUpperCase(value);
            } else if (Character.isUpperCase(value)) {
                CASE_INSENSITIVE[value] = Character.toLowerCase(value);
            } else {
                CASE_INSENSITIVE[value] = value;
            }
        }
    }

    public static int compare(char c1, char c2, boolean caseSensitive) {
        if (c1 == c2) {
            return 0;
        }

        if (caseSensitive) {
            return c1 - c2;
        }

        char sc1 = switchCase(c1);
        char sc2 = switchCase(c2);

        char normalizedC1 = c1 <= sc1 ? c1 : sc1;
        char normalizedC2 = c2 <= sc2 ? c2 : sc2;

        return normalizedC1 - normalizedC2;
    }

    public static int compare(CharSequence s1, CharSequence s2, boolean caseSensitive) {
        for (int i = 0; i < s1.length(); i++) {
            if (s2.length() == i) {
                return s1.length() - s2.length();
            }

            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);

            int result = c1 - c2;
            if (result == 0 || CharDataAccessor.equals(c1, c2, caseSensitive)) {
                continue;
            }

            return result;
        }

        return s1.length() - s2.length();
    }

    public static boolean isDifferentCase(char char1, char char2) {
        return Character.isUpperCase(char1) && !Character.isUpperCase(char2) ||
            Character.isLowerCase(char1) && !Character.isLowerCase(char2);
    }

    public static char switchCase(char character) {
        if (character == Character.MAX_VALUE) {
            return character;
        }

        return CASE_INSENSITIVE[character];
    }

    protected static boolean equals(char required, char actual, boolean caseSensitive) {
        if (required == actual) {
            return true;
        }

        return !caseSensitive && required < CASE_INSENSITIVE.length && CASE_INSENSITIVE[required] == actual;
    }

    /**
     * Examine a node to find an outgoing connection matching the given <code>label</code>
     *
     * @param data The graph data
     * @param index The index of the node.
     * @param label The label to match.
     * @param caseSensitive whether or not to match labels in a case-sensitive manner.
     *
     * @return The index of the outgoing connection or <code>-1</code> if no matching connection exists.
     */
    protected static int getArc(char[] data, int index, char label, boolean caseSensitive) {
        for (int i = index; i < data.length; i += NODE_SIZE) {
            if (equals(getLabel(data, i), label, caseSensitive)) {
                return i;
            }

            if (isLast(data, i)) {
                break;
            }
        }

        return -1;
    }

    protected static char getLabel(char[] data, int index) {
        return data[index];
    }

    /**
     * Get the nodex index and outgoing connection points to.
     *
     * @param data The graph data.
     * @param index The index of the outgoing connection.
     *
     * @return The target node index.
     */
    protected static int getTarget(char[] data, int index) {
        return (data[index + ADDRESS_OFFSET] & MASK_ADDRESS_HIGH) << 16 | data[index + ADDRESS_OFFSET + 1];
    }

    protected static boolean isLast(char[] data, int index) {
        return (data[index + FLAGS_OFFSET] & MASK_LAST) != 0;
    }

    /**
     * Determines whether and outgoing connection is marked as "terminal" or not.
     *
     * Terminal connection mark the end of an accepted input.
     *
     * @param data The graph data.
     * @param index The index of the connection.
     * @return <code>true</code> when the connection is "terminal", <code>false</code> otherwise.
     */
    protected static boolean isTerminal(char[] data, int index) {
        return (data[index + FLAGS_OFFSET] & MASK_TERMINAL) != 0;
    }

    protected static void setFlag(char[] data, int index, int bitMask, boolean enabled) {
        if (enabled) {
            data[index + CharDataAccessor.FLAGS_OFFSET] |= bitMask;
        } else {
            data[index + CharDataAccessor.FLAGS_OFFSET] &= ~bitMask;
        }
    }

    protected static void setLabel(char[] data, int index, char label) {
        data[index] = label;
    }

    protected static void setLast(char[] data, int index, boolean last) {
        setFlag(data, index, MASK_LAST, last);
    }

    protected static void setTarget(char[] data, int index, int target) {
        char currentFlags = (char) (data[index + CharDataAccessor.ADDRESS_OFFSET] & MASK_FLAGS);
        data[index + CharDataAccessor.ADDRESS_OFFSET] = (char) (target >> 16 & MASK_ADDRESS_HIGH | currentFlags);
        data[index + CharDataAccessor.ADDRESS_OFFSET + 1] = (char) (target & MASK_ADDRESS_LOW);
    }

    protected static void setTerminal(char[] data, int index, boolean terminal) {
        setFlag(data, index, MASK_TERMINAL, terminal);
    }
}
