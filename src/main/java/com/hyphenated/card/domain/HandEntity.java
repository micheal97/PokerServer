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

import com.hyphenated.card.Deck;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.SortedSet;

@Getter
@Setter
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@EqualsAndHashCode
@Embeddable
public class HandEntity implements Serializable {

    @Embedded
    @OneToMany
    @Setter(value = AccessLevel.NONE)
    private final SortedSet<Player> players;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Player currentToAct;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Player lastBetOrRaise;
    @Embedded
    private final Deck deck = new Deck();
    @Embedded
    private final BoardEntity board = new BoardEntity(deck);
    private int pot;
    private int betAmount;
    /**
     * -- GETTER --
     * In No Limit poker, the minimum bet size is twice the previous bet.  Use this field to determine
     * what that amount would be.
     *
     * @return The last bet/raise amount
     */
    private int lastBetAmount;

    public HandEntity(Game game) {
        players = game.getPlayers();
        currentToAct = players.first();
        lastBetOrRaise = null;
        pot = 0;
        betAmount = 0;
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void addAllPlayers(Collection<Player> players) {
        this.players.addAll(players);
    }
}
