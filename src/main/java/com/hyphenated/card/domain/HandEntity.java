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
import com.hyphenated.card.holder.Board;
import jakarta.persistence.*;
import lombok.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

@Getter
@Setter
@NoArgsConstructor(force = true)
@EqualsAndHashCode
@Embeddable
public class HandEntity {

    @Embedded
    @OneToMany
    @Setter(value = AccessLevel.NONE)
    @NonNull
    private final SortedSet<Player> players = new TreeSet<>();
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    private Player currentToAct = null;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Nullable
    private Player lastBetOrRaise = null;
    @Embedded
    @NonNull
    private final Deck deck = new Deck();
    @Embedded
    @NonNull
    private final BoardEntity boardEntity = new BoardEntity(deck);
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

    public Board getBoard() {
        return new Board(boardEntity.getFlop1(), boardEntity.getFlop2(), boardEntity.getFlop3(), boardEntity.getRiver(), boardEntity.getTurn());
    }

    public Optional<Player> findPlayerInBTN() {
        return players.stream().filter(Player::isPlayerInButton).findAny();
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
