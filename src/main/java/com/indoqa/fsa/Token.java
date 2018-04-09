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

public class Token {

    private int start;
    private int end;
    private String value;
    private String original;

    public static Token create(int start, String value) {
        Token token = new Token();

        token.setStart(start);
        token.setEnd(start + value.length());
        token.setValue(value);
        token.setOriginal(value);

        return token;
    }

    public int getDistance(Token other) {
        if (this.end <= other.start) {
            return other.start - this.end;
        }

        if (other.end <= this.start) {
            return this.start - other.end;
        }

        return 0;
    }

    public int getEnd() {
        return this.end;
    }

    public int getLength() {
        return this.end - this.start;
    }

    public String getOriginal() {
        return this.original;
    }

    public int getStart() {
        return this.start;
    }

    public String getValue() {
        return this.value;
    }

    public boolean isDisjunct(Token token) {
        return this.end <= token.start || this.start >= token.end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value + " [" + this.start + ", " + this.end + "]";
    }
}
