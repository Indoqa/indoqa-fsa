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

import java.util.Arrays;

public class Replacements {

    private int[] targets;
    private int count;

    public Replacements(int capacity) {
        this.targets = new int[capacity];
    }

    public void clear() {
        this.count = 0;
        Arrays.fill(this.targets, -1);
    }

    public int getCount() {
        return this.count;
    }

    public int getReplacement(int from) {
        if (this.targets.length <= from) {
            return -1;
        }

        return this.targets[from];
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public void setReplacement(int from, int to) {
        this.targets[from] = to;
        this.count++;
    }
}
