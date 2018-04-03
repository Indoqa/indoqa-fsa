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
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.indoqa.fsa.utils.EncodingUtils;

import morfologik.fsa.FSA;
import morfologik.fsa.builders.FSA5Serializer;
import morfologik.fsa.builders.FSABuilder;

public class AcceptorBuilder {

    private final boolean caseSensitive;
    private final Set<byte[]> inputs = new TreeSet<>(FSABuilder.LEXICAL_ORDERING);

    public AcceptorBuilder() {
        this(false);
    }

    public AcceptorBuilder(boolean caseSensitive) {
        super();
        this.caseSensitive = caseSensitive;
    }

    public static Acceptor build(boolean caseSensitive, Iterable<String> acceptedInputs) {
        AcceptorBuilder builder = new AcceptorBuilder(caseSensitive);

        builder.addAcceptedInput(acceptedInputs);

        return builder.build();
    }

    public static Acceptor build(boolean caseSensitive, String... acceptedInputs) {
        return build(caseSensitive, Arrays.asList(acceptedInputs));
    }

    public static Acceptor read(InputStream inputStream) throws IOException {
        boolean caseSensitive = (inputStream.read() & 0xFFFF) == 1;

        FSA fsa = FSA.read(inputStream);
        return new Acceptor(fsa, caseSensitive);
    }

    public void addAcceptedInput(Iterable<String> input) {
        for (String eachInput : input) {
            this.addAcceptedInput(eachInput);
        }
    }

    public void addAcceptedInput(String... input) {
        for (String eachInput : input) {
            this.addAcceptedInput(eachInput);
        }
    }

    public Acceptor build() {
        FSA fsa = this.buildFSA();
        return new Acceptor(fsa, this.caseSensitive);
    }

    public int size() {
        return this.inputs.size();
    }

    public void write(OutputStream outputStream) throws IOException {
        outputStream.write(this.caseSensitive ? 1 : 0);

        FSA fsa = this.buildFSA();

        FSA5Serializer fsa5Serializer = new FSA5Serializer();
        fsa5Serializer.serialize(fsa, outputStream);
    }

    private void addAcceptedInput(String input) {
        if (input.length() == 0) {
            return;
        }

        if (!this.caseSensitive) {
            this.inputs.add(EncodingUtils.getBytes(input.toLowerCase(Locale.ROOT)));
        } else {
            this.inputs.add(EncodingUtils.getBytes(input));
        }
    }

    private FSA buildFSA() {
        FSABuilder builder = new FSABuilder(1024 * 1024);

        for (byte[] eachInput : this.inputs) {
            builder.add(eachInput, 0, eachInput.length);
        }

        return builder.complete();
    }
}
