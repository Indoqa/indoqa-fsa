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

import java.util.ArrayList;
import java.util.List;

import morfologik.fsa.FSA;
import morfologik.fsa.builders.FSABuilder;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryMetadata;
import morfologik.stemming.DictionaryMetadataBuilder;
import morfologik.stemming.EncoderType;

public class TransducerBuilder {

    private List<byte[]> inputs = new ArrayList<>();

    private final char separator;

    private final DictionaryMetadata dictionaryMetadata;

    public TransducerBuilder(char separator) {
        super();

        this.separator = separator;

        DictionaryMetadataBuilder dictionaryMetadataBuilder = new DictionaryMetadataBuilder();
        dictionaryMetadataBuilder.separator(separator);
        dictionaryMetadataBuilder.encoder(EncoderType.NONE);
        dictionaryMetadataBuilder.encoding(EncodingUtils.CHARSET);
        this.dictionaryMetadata = dictionaryMetadataBuilder.build();
    }

    public void add(String input, String output) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(input);
        stringBuilder.append(this.separator);
        stringBuilder.append(output);

        this.inputs.add(EncodingUtils.getBytes(stringBuilder.toString()));
    }

    public Transducer build() {
        this.inputs.sort(FSABuilder.LEXICAL_ORDERING);

        FSA fsa = FSABuilder.build(this.inputs);
        return new Transducer(new Dictionary(fsa, this.dictionaryMetadata));
    }
}
