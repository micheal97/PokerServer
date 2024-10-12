/*
The MIT License (MIT)

Copyright (c) 2013 Jacob Kanipe-Illig

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package com.hyphenated.card.domain;

import com.hyphenated.card.Card;
import com.hyphenated.card.Deck;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Embeddable
public class BoardEntity {
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @NonNull
    private final Card flop1;
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @NonNull
    private final Card flop2;
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @NonNull
    private final Card flop3;
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @NonNull
    private final Card turn;
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @NonNull
    private final Card river;

    private List<Card> flop = new ArrayList<>();

    public BoardEntity(Deck deck) {
        flop1 = deck.dealCard();
        flop2 = deck.dealCard();
        flop3 = deck.dealCard();
        flop = List.of(flop1, flop2, flop3);
        turn = deck.dealCard();
        river = deck.dealCard();
    }
}
