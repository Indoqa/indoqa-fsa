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

public class CharMatch {

    private int start;
    private int length;
    private int index;
    private final boolean fullMatchRequired;

    private CharMatch(boolean fullMatchRequired) {
        super();
        this.fullMatchRequired = fullMatchRequired;
    }

    public static CharMatch fullMatchRequired() {
        return new CharMatch(true);
    }

    public static CharMatch partialMatchAllowed() {
        return new CharMatch(false);
    }

    public int getIndex() {
        return this.index;
    }

    public int getLength() {
        return this.length;
    }

    public int getStart() {
        return this.start;
    }

    public boolean isFullMatchRequired() {
        return this.fullMatchRequired;
    }

    public boolean isMatch(int actualLength) {
        if (this.index == -1) {
            return false;
        }

        return !this.fullMatchRequired || this.length == actualLength;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setStart(int start) {
        this.start = start;
    }
}
