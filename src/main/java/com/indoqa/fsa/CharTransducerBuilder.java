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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class CharTransducerBuilder implements TransducerBuilder {

    private final CharAcceptorBuilder acceptorBuilder;
    private final char separator;

    public CharTransducerBuilder(boolean caseSensitive) {
        this(caseSensitive, Character.MAX_VALUE);
    }

    public CharTransducerBuilder(boolean caseSensitive, char separator) {
        this(caseSensitive, separator, CharAcceptorBuilder.DEFAULT_CAPACITY_INCREMENT);
    }

    public CharTransducerBuilder(boolean caseSensitive, char separator, int capacityIncrement) {
        super();

        this.acceptorBuilder = new CharAcceptorBuilder(caseSensitive, capacityIncrement);
        this.separator = separator;
    }

    public static CharTransducer build(char separator, boolean caseSensitive, Iterable<String> values) {
        CharTransducerBuilder builder = new CharTransducerBuilder(caseSensitive);

        for (String eachValue : values) {
            String[] parts = eachValue.split(String.valueOf(separator), 2);
            builder.add(parts[0], parts[1]);
        }

        return builder.build();
    }

    public static CharTransducer build(char separator, boolean caseSensitive, String... value) {
        return build(separator, caseSensitive, Arrays.asList(value));
    }

    public static CharTransducer read(InputStream inputStream) throws IOException {
        char separator = (char) (inputStream.read() & 0xFF | (inputStream.read() & 0xFF) << 8);

        CharAcceptor charAcceptor = CharAcceptorBuilder.read(inputStream);
        return new CharTransducer(charAcceptor, separator);
    }

    @Override
    public void add(String input, String output) {
        this.acceptorBuilder.addAcceptedInput(input + this.separator + output);
    }

    @Override
    public CharTransducer build() {
        CharAcceptor charAcceptor = this.acceptorBuilder.build();
        return new CharTransducer(charAcceptor, Character.MAX_VALUE);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        outputStream.write(Character.MAX_VALUE);
        outputStream.write(Character.MAX_VALUE >> 8);

        this.acceptorBuilder.write(outputStream);
    }
}
