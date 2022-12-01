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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.indoqa.fsa.TransducerBuilder;

public class CharTransducerBuilder implements TransducerBuilder {

    public static final char DEFAULT_SEPARATOR = Character.MAX_VALUE;

    private final CharAcceptorBuilder acceptorBuilder;
    private final char separator;

    public CharTransducerBuilder(boolean caseSensitive) {
        this(caseSensitive, DEFAULT_SEPARATOR);
    }

    public CharTransducerBuilder(boolean caseSensitive, char separator) {
        this(caseSensitive, separator, CharAcceptorBuilder.DEFAULT_CAPACITY_INCREMENT, CharAcceptorBuilder.DEFAULT_SHRINK_LIMIT);
    }

    public CharTransducerBuilder(boolean caseSensitive, char separator, int capacityIncrement, int shrinkLimit) {
        super();

        this.acceptorBuilder = new CharAcceptorBuilder(caseSensitive, capacityIncrement, shrinkLimit);
        this.separator = separator;
    }

    public static CharTransducer build(boolean caseSensitive, String splitPattern, Iterable<String> values) {
        CharTransducerBuilder builder = new CharTransducerBuilder(caseSensitive);

        for (String eachValue : values) {
            String[] parts = eachValue.split(splitPattern, 2);
            builder.add(parts[0], parts[1]);
        }

        return builder.build();
    }

    public static CharTransducer build(boolean caseSensitive, String splitPattern, String... value) {
        return build(caseSensitive, splitPattern, Arrays.asList(value));
    }

    public static CharTransducer empty() {
        return build(true, String.valueOf(DEFAULT_SEPARATOR));
    }

    public static CharTransducer read(InputStream inputStream) throws IOException {
        CharAcceptor charAcceptor = CharAcceptorBuilder.read(inputStream);
        char separator = (char) (inputStream.read() & 0xFF | (inputStream.read() & 0xFF) << 8);

        return new CharTransducer(charAcceptor, separator);
    }

    public void add(Iterable<? extends CharSequence> input, String output) {
        List<Integer> nodes = new ArrayList<>();

        for (CharSequence eachInput : input) {
            nodes.add(this.acceptorBuilder.addAcceptedInput(eachInput, 0, eachInput.length(), 0, false));
        }

        if (nodes.isEmpty()) {
            return;
        }

        int separatorNode = this.acceptorBuilder.addAcceptedInput(String.valueOf(this.separator), 0, 1, nodes.get(0), false);

        for (int i = 1; i < nodes.size(); i++) {
            this.acceptorBuilder.addArc(nodes.get(i), this.separator, separatorNode, false);
        }

        this.acceptorBuilder.addAcceptedInput(output, 0, output.length(), separatorNode, true);
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

    public void setAbortSupplier(BooleanSupplier abortSupplier) {
        this.acceptorBuilder.setAbortSupplier(abortSupplier);
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
