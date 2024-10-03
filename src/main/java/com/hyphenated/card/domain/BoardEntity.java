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
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "board")
public class BoardEntity implements Serializable {

    @Column(name = "board_id")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "flop1")
    @Enumerated(EnumType.STRING)
    private Card flop1;
    @Column(name = "flop2")
    @Enumerated(EnumType.STRING)
    private Card flop2;
    @Column(name = "flop3")
    @Enumerated(EnumType.STRING)
    private Card flop3;
    @Column(name = "turn")
    @Enumerated(EnumType.STRING)
    private Card turn;
    @Column(name = "river")
    @Enumerated(EnumType.STRING)
    private Card river;

    /**
     * Get all of the cards on the board in list form.
     * The list is ordered with flop first, then turn, then river.
     *
     * @return
     */
    @Transient
    public List<Card> getBoardCards() {
        List<Card> cards = new ArrayList<Card>();
        if (flop1 != null) {
            cards.add(flop1);
            cards.add(flop2);
            cards.add(flop3);
        }
        if (turn != null) {
            cards.add(turn);
        }
        if (river != null) {
            cards.add(river);
        }
        return cards;
    }

}
