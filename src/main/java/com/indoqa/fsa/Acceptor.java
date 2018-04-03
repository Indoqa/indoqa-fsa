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

import static com.indoqa.fsa.traversal.Result.Match.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.indoqa.fsa.traversal.AllMatchesFSATraversal;
import com.indoqa.fsa.traversal.PrefixFSATraversal;
import com.indoqa.fsa.traversal.Result;
import com.indoqa.fsa.utils.EncodingUtils;

import morfologik.fsa.FSA;

public class Acceptor {

    private static final String[] EMPTY_STRINGS = new String[0];

    private final boolean caseSensitive;
    private final int rootNode;
    private final PrefixFSATraversal prefixTraversal;
    private final AllMatchesFSATraversal allMatchesTraversal;

    protected Acceptor(FSA fsa, boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        this.rootNode = fsa.getRootNode();
        this.prefixTraversal = new PrefixFSATraversal(fsa);
        this.allMatchesTraversal = new AllMatchesFSATraversal(fsa);
    }

    public boolean accepts(CharSequence value) {
        if (value == null) {
            return false;
        }

        byte[] bytes = this.getBytes(value);

        Result result = this.prefixTraversal.match(bytes);
        return result.getMatch() == EXACT_MATCH;
    }

    public String[] getAllMatches(CharSequence value) {
        if (value == null) {
            return EMPTY_STRINGS;
        }

        byte[] bytes = this.getBytes(value);
        return this.getAllMatches(bytes, 0, bytes.length);
    }

    public String getLongestMatch(CharSequence value) {
        byte[] bytes = this.getBytes(value);

        return this.getLongestMatch(bytes, 0, bytes.length);
    }

    public List<Token> getTokens(CharSequence value) {
        List<Token> result = new ArrayList<>();

        byte[] bytes = this.getBytes(value);

        Token lastToken = null;

        for (int i = 0; i < bytes.length - 1; i++) {
            String longestMatch = this.getLongestMatch(bytes, i, bytes.length - i);
            if (longestMatch == null) {
                continue;
            }

            int charOffset = EncodingUtils.getString(bytes, 0, i).length();
            Token token = Token.create(charOffset, longestMatch);
            if (lastToken == null || lastToken.isDisjunct(token)) {
                result.add(token);
                lastToken = token;
                continue;
            }

            if (lastToken.getLength() >= token.getLength()) {
                continue;
            }

            result.remove(result.size() - 1);
            result.add(token);
            lastToken = token;
        }

        return result;
    }

    private String[] getAllMatches(byte[] bytes, int offset, int length) {
        int[] allMatches = this.allMatchesTraversal.getAllMatches(bytes, offset, length);
        String[] result = new String[allMatches.length];

        for (int i = 0; i < allMatches.length; i++) {
            result[i] = EncodingUtils.getString(bytes, offset, allMatches[i]);
        }

        return result;
    }

    private byte[] getBytes(CharSequence value) {
        if (this.caseSensitive) {
            return EncodingUtils.getBytes(value);
        }

        return EncodingUtils.getBytes(value.toString().toLowerCase(Locale.ROOT));
    }

    private String getLongestMatch(byte[] bytes, int offset, int length) {
        Result result = new Result();

        Result match = this.prefixTraversal.match(result, bytes, offset, length, this.rootNode);

        if (match.getMatch() == EXACT_MATCH) {
            return EncodingUtils.getString(bytes, offset, length);
        }

        if (match.getMatch() == PARTIAL_MATCH) {
            return EncodingUtils.getString(bytes, offset, match.getMatchedLength());
        }

        return null;
    }
}
