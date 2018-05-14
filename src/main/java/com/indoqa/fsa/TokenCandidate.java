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
import java.util.stream.Collectors;

public class TokenCandidate {

    private Token token;
    private final List<TokenCandidate> challenges = new ArrayList<>();
    private int challengedBy;
    private boolean selected = true;

    public static TokenCandidate create(Token token) {
        TokenCandidate result = new TokenCandidate();
        result.token = token;
        return result;
    }

    public static List<Token> eliminateOverlapping(List<Token> tokens) {
        List<TokenCandidate> candidates = tokens.stream().map(TokenCandidate::create).collect(Collectors.toList());

        for (int i = 0; i < candidates.size() - 1; i++) {
            TokenCandidate candidate = candidates.get(i);

            for (int j = i + 1; j < candidates.size(); j++) {
                TokenCandidate otherCandidate = candidates.get(j);
                if (candidate.isDisjunct(otherCandidate)) {
                    break;
                }

                if (candidate.getLength() > otherCandidate.getLength()) {
                    candidate.addChallenges(otherCandidate);
                } else {
                    otherCandidate.addChallenges(candidate);
                }
            }
        }

        boolean changed;
        do {
            changed = false;

            for (TokenCandidate eachCandidate : candidates) {
                if (eachCandidate.canBeSelected()) {
                    eachCandidate.select();
                    changed = true;
                }
            }
        } while (changed);

        return candidates.stream().filter(TokenCandidate::isSelected).map(TokenCandidate::getToken).collect(Collectors.toList());
    }

    public void addChallenges(TokenCandidate otherCandidate) {
        this.challenges.add(otherCandidate);
        otherCandidate.challengedBy++;
    }

    public boolean canBeSelected() {
        return this.selected && this.challengedBy == 0;
    }

    public int getLength() {
        return this.token.getLength();
    }

    public Token getToken() {
        return this.token;
    }

    public boolean isDisjunct(TokenCandidate otherCandidate) {
        return this.token.isDisjunct(otherCandidate.getToken());
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void select() {
        for (TokenCandidate eachChallenges : this.challenges) {
            eachChallenges.eliminate();
        }

        this.challengedBy--;
    }

    private void eliminate() {
        for (TokenCandidate eachIntersect : this.challenges) {
            eachIntersect.challengedBy--;
        }

        this.selected = false;
    }
}
