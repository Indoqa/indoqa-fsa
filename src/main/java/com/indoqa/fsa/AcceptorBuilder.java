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
import java.util.Collections;
import java.util.List;

import morfologik.fsa.FSA;
import morfologik.fsa.builders.FSABuilder;

public class AcceptorBuilder {

    private List<byte[]> inputs = new ArrayList<>();

    public static Acceptor build(Iterable<String> acceptedInputs) {
        AcceptorBuilder builder = new AcceptorBuilder();

        builder.addAcceptedInput(acceptedInputs);

        return builder.build();
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
        Collections.sort(this.inputs, FSABuilder.LEXICAL_ORDERING);

        FSA fsa = FSABuilder.build(this.inputs);

        return new Acceptor(fsa);
    }

    private void addAcceptedInput(String input) {
        this.inputs.add(EncodingUtils.getBytes(input));
    }
}
