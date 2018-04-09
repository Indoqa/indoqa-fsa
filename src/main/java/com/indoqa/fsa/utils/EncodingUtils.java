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
package com.indoqa.fsa.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class EncodingUtils {

    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private EncodingUtils() {
        // hide utility class constructor
    }

    public static byte[] getBytes(CharSequence value) {
        return getBytes(value.toString());
    }

    public static byte[] getBytes(CharSequence value, int offset, int length) {
        return getBytes(value.subSequence(offset, length));
    }

    public static byte[] getBytes(String value) {
        return value.getBytes(CHARSET);
    }

    public static String getString(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, CHARSET);
    }

    public static boolean isTokenEnd(byte[] bytes, int offset) {
        if (offset == bytes.length) {
            return isWordPart(bytes[offset - 1]);
        }

        return isWordPart(bytes[offset - 1]) && !isWordPart(bytes[offset]);
    }

    public static boolean isTokenStart(byte[] bytes, int offset) {
        if (offset == 0) {
            return isWordPart(bytes[offset]);
        }

        return !isWordPart(bytes[offset - 1]) && isWordPart(bytes[offset]);
    }

    private static boolean isWordPart(byte value) {
        if ((value & 0xC0) == 0xC0) {
            return true;
        }

        if ((value & 0x80) == 0x80) {
            return true;
        }

        return Character.isAlphabetic(value) || Character.isDigit(value);
    }
}
