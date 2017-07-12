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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public final class EncodingUtils {

    protected static final Charset CHARSET = Charset.forName("UTF-8");
    protected static final CharsetEncoder ENCODER = CHARSET.newEncoder();

    private EncodingUtils() {
        // hide utility class constructor
    }

    public static ByteBuffer encode(char c, ByteBuffer byteBuffer) {
        return encode(String.valueOf(c), byteBuffer);
    }

    protected static ByteBuffer encode(CharSequence value, ByteBuffer byteBuffer) {
        ByteBuffer result = byteBuffer;
        if (byteBuffer == null) {
            result = grow(byteBuffer);
        }

        CharBuffer charBuffer = CharBuffer.wrap(value);

        while (true) {
            CoderResult coderResult = ENCODER.encode(charBuffer, result, true);
            if (coderResult.isUnderflow()) {
                return result;
            }

            if (coderResult.isOverflow()) {
                ByteBuffer buffer = ByteBuffer.allocate(result.capacity() + 1024);
                result.flip();
                buffer.put(result);
                result = buffer;
                charBuffer.rewind();
            }

            if (coderResult.isError()) {
                try {
                    coderResult.throwException();
                } catch (CharacterCodingException e) {
                    throw new IllegalArgumentException("Could not encode '" + value + "'.", e);
                }
            }
        }
    }

    protected static byte[] getBytes(String value) {
        return value.getBytes(CHARSET);
    }

    protected static ByteBuffer grow(ByteBuffer byteBuffer) {
        int oldCapacity = byteBuffer == null ? 0 : byteBuffer.capacity();
        return ByteBuffer.allocate(oldCapacity + 1024);
    }

}
