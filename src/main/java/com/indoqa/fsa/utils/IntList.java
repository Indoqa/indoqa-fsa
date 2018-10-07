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
package com.indoqa.fsa.utils;

public class IntList {

    private int[] elements = new int[1024];
    private int size;

    public void add(int value) {
        if (this.size == this.elements.length) {
            this.growCapacity();
        }

        this.elements[this.size++] = value;
    }

    public int get(int index) {
        return this.elements[index];
    }

    public int size() {
        return this.size;
    }

    private void growCapacity() {
        int[] newElements = new int[this.elements.length + 1024];
        System.arraycopy(this.elements, 0, newElements, 0, this.elements.length);
        this.elements = newElements;
    }
}
