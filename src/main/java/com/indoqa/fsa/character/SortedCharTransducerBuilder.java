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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

import com.indoqa.fsa.TransducerBuilder;

public class SortedCharTransducerBuilder implements TransducerBuilder {

    public static final char DEFAULT_SEPARATOR = Character.MAX_VALUE;

    private final SortedCharAcceptorBuilder acceptorBuilder;
    private final char separator;

    public SortedCharTransducerBuilder(boolean caseSensitive) {
        this(caseSensitive, DEFAULT_SEPARATOR);
    }

    public SortedCharTransducerBuilder(boolean caseSensitive, char separator) {
        this(caseSensitive, separator, SortedCharAcceptorBuilder.DEFAULT_CAPACITY_INCREMENT);
    }

    public SortedCharTransducerBuilder(boolean caseSensitive, char separator, int capacityIncrement) {
        super();

        this.acceptorBuilder = new SortedCharAcceptorBuilder(caseSensitive, capacityIncrement);
        this.separator = separator;
    }

    public static CharTransducer build(boolean caseSensitive, String splitPattern, Iterable<String> values) {
        SortedCharTransducerBuilder builder = new SortedCharTransducerBuilder(caseSensitive);

        for (String eachValue : values) {
            String[] parts = eachValue.split(splitPattern, 2);
            builder.add(parts[0], parts[1]);
        }

        return builder.build();
    }

    public static CharTransducer build(boolean caseSensitive, String splitPattern, String... value) {
        return build(caseSensitive, splitPattern, Arrays.asList(value));
    }

    public static CharTransducer read(InputStream inputStream) throws IOException {
        CharAcceptor charAcceptor = CharAcceptorBuilder.read(inputStream);
        char separator = (char) (inputStream.read() & 0xFF | (inputStream.read() & 0xFF) << 8);

        return new CharTransducer(charAcceptor, separator);
    }

    @Override
    public void add(String input, String output) {
        this.acceptorBuilder.addAcceptedInput(input + this.separator + output);
    }

    @Override
    public CharTransducer build() {
        CharAcceptor charAcceptor = this.acceptorBuilder.build();
        return new CharTransducer(charAcceptor, DEFAULT_SEPARATOR);
    }

    public void setMessageConsumer(Consumer<String> messageConsumer) {
        this.acceptorBuilder.setMessageConsumer(messageConsumer);
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        this.acceptorBuilder.write(outputStream);

        outputStream.write(DEFAULT_SEPARATOR);
        outputStream.write(DEFAULT_SEPARATOR >> 8);
    }
}
