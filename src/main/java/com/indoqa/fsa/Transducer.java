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

import java.util.List;
import java.util.stream.Collectors;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.WordData;

public class Transducer {

    private final Dictionary dictionary;
    private final DictionaryLookup dictionaryLookup;

    public Transducer(Dictionary dictionary) {
        this.dictionary = dictionary;
        this.dictionaryLookup = new DictionaryLookup(dictionary);
    }

    public String transduce(CharSequence input) {
        return this.transduce(input, null);
    }

    public String transduce(CharSequence input, String defaultValue) {
        List<WordData> result = this.dictionaryLookup.lookup(input);

        if (result.isEmpty()) {
            return defaultValue;
        }

        return result.get(0).getStem().toString();
    }

    public List<CharSequence> transduceAll(CharSequence input) {
        List<WordData> result = this.dictionaryLookup.lookup(input);
        return result.stream().map(WordData::getStem).collect(Collectors.toList());
    }
}
