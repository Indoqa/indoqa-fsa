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
package com.indoqa.fsa.morfologik;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.indoqa.fsa.TransducerBuilder;
import com.indoqa.fsa.utils.EncodingUtils;

import morfologik.fsa.FSA;
import morfologik.fsa.builders.FSA5Serializer;
import morfologik.fsa.builders.FSABuilder;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryMetadata;
import morfologik.stemming.DictionaryMetadataBuilder;
import morfologik.stemming.EncoderType;

public class MorfologikTransducerBuilder implements TransducerBuilder {

    public static final char DEFAULT_SEPARATOR = 0x7F;

    private final Set<byte[]> inputs = new TreeSet<>(FSABuilder.LEXICAL_ORDERING);

    private final char separator;
    private final boolean caseSensitive;

    private final DictionaryMetadata dictionaryMetadata;

    public MorfologikTransducerBuilder(char separator) {
        this(separator, true);
    }

    public MorfologikTransducerBuilder(char separator, boolean caseSensitive) {
        super();

        this.separator = separator;
        this.caseSensitive = caseSensitive;

        DictionaryMetadataBuilder dictionaryMetadataBuilder = new DictionaryMetadataBuilder();
        dictionaryMetadataBuilder.separator(separator);
        dictionaryMetadataBuilder.encoder(EncoderType.NONE);
        dictionaryMetadataBuilder.encoding(EncodingUtils.CHARSET);
        this.dictionaryMetadata = dictionaryMetadataBuilder.build();
    }

    public static MorfologikTransducer build(boolean caseSensitive, String splitPattern, Iterable<String> values) {
        MorfologikTransducerBuilder builder = new MorfologikTransducerBuilder(DEFAULT_SEPARATOR, caseSensitive);

        for (String eachValue : values) {
            String[] parts = eachValue.split(splitPattern, 2);
            builder.add(parts[0], parts[1]);
        }

        return builder.build();
    }

    public static MorfologikTransducer build(boolean caseSensitive, String splitPattern, String... value) {
        return build(caseSensitive, splitPattern, Arrays.asList(value));
    }

    public static MorfologikTransducer read(InputStream inputStream) throws IOException {
        char separator = (char) (inputStream.read() & 0xFFFF);
        boolean caseSensitive = (inputStream.read() & 0xFFFF) == 1;

        DictionaryMetadataBuilder dictionaryMetadataBuilder = new DictionaryMetadataBuilder();
        dictionaryMetadataBuilder.separator(separator);
        dictionaryMetadataBuilder.encoder(EncoderType.NONE);
        dictionaryMetadataBuilder.encoding(EncodingUtils.CHARSET);
        DictionaryMetadata dictionaryMetadata = dictionaryMetadataBuilder.build();

        FSA fsa = FSA.read(inputStream);
        return new MorfologikTransducer(new Dictionary(fsa, dictionaryMetadata), caseSensitive);
    }

    @Override
    public void add(String input, String output) {
        StringBuilder stringBuilder = new StringBuilder();

        if (this.caseSensitive) {
            stringBuilder.append(input);
        } else {
            stringBuilder.append(input.toLowerCase(Locale.ROOT));
        }
        stringBuilder.append(this.separator);
        stringBuilder.append(output);

        this.inputs.add(EncodingUtils.getBytes(stringBuilder));
    }

    @Override
    public MorfologikTransducer build() {
        FSA fsa = this.buildFSA();

        return new MorfologikTransducer(new Dictionary(fsa, this.dictionaryMetadata), this.caseSensitive);
    }

    public int size() {
        return this.inputs.size();
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        outputStream.write(this.dictionaryMetadata.getSeparatorAsChar());
        outputStream.write(this.caseSensitive ? 1 : 0);

        FSA fsa = this.buildFSA();

        FSA5Serializer fsa5Serializer = new FSA5Serializer();
        fsa5Serializer.serialize(fsa, outputStream);
    }

    private FSA buildFSA() {
        FSABuilder builder = new FSABuilder(64 * 1024);

        for (byte[] eachInput : this.inputs) {
            builder.add(eachInput, 0, eachInput.length);
        }

        return builder.complete();
    }
}
