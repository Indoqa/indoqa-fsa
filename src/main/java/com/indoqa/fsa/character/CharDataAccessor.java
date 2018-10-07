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
    protected static final int MASK_SIZE = 0x7FFF;
    protected static final int MASK_TERMINAL = 0x8000;
    protected static final int MASK_LAST = 0x4000;
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

    public static char switchCase(char character) {
        return CASE_INSENSITIVE[character];
    }

    protected static boolean equals(char required, char actual, boolean caseSensitive) {
        if (required == actual) {
            return true;
        }

        return !caseSensitive && required < CASE_INSENSITIVE.length && CASE_INSENSITIVE[required] == actual;
    }

    protected static int getArc(char[] data, int index, char label, boolean caseSensitive) {
        for (int i = index; index < data.length; i += NODE_SIZE) {
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

    protected static int getTarget(char[] data, int index) {
        return (data[index + ADDRESS_OFFSET] & 0x3FFF) << 16 | data[index + ADDRESS_OFFSET + 1];
    }

    protected static boolean isLast(char[] data, int index) {
        return (data[index + FLAGS_OFFSET] & MASK_LAST) != 0;
    }

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
        data[index + CharDataAccessor.ADDRESS_OFFSET] = (char) (target >> 16 & 0x3FFF);
        data[index + CharDataAccessor.ADDRESS_OFFSET + 1] = (char) (target & 0xFFFF);
    }

    protected static void setTerminal(char[] data, int index, boolean terminal) {
        setFlag(data, index, MASK_TERMINAL, terminal);
    }

    public static boolean isDifferentCase(char char1, char char2) {
        return Character.isUpperCase(char1) && !Character.isUpperCase(char2) ||
            Character.isLowerCase(char1) && !Character.isLowerCase(char2);
    }
}
