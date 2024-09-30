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
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Setter
@Entity
@Table(name = "hand")
public class HandEntity implements Serializable {

    private UUID id;
    private Game game;
    private TableStructure tableStructure;
    private BoardEntity board;
    private Set<PlayerHand> players;
    private Player currentToAct;
    private Player lastBetOrRaise;
    private BlindLevel blindLevel;
    private List<Card> cards;
    private int pot;
    private int totalBetAmount;
    private int lastBetAmount;

    public PlayerHand getPlayerHandById(UUID playerHandId) {
        return players.stream().filter(playerHand -> playerHand.getId() == playerHandId).findFirst().get();
    }

    public PlayerHand getPlayerHandByPlayerId(UUID playerId) {
        return players.stream().filter(playerHand -> playerHand.getPlayer().getId() == playerId).findFirst().get();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "hand_id")
    public UUID getId() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    public Game getGame() {
        return game;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_structure_id")
    public TableStructure getTableStructure() {
        return tableStructure;
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @JoinColumn(name = "board_id")
    public BoardEntity getBoard() {
        return board;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "handEntity", cascade = {CascadeType.ALL}, orphanRemoval = true)
    public Set<PlayerHand> getPlayers() {
        return players;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_to_act_id")
    public Player getCurrentToAct() {
        return currentToAct;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_bet_or_raise_id")
    public Player getLastBetOrRaise() {
        return lastBetOrRaise;
    }


    @Column(name = "blind_level")
    @Enumerated(EnumType.STRING)
    public BlindLevel getBlindLevel() {
        return blindLevel;
    }

    @ElementCollection(targetClass = Card.class)
    @JoinTable(name = "hand_deck", joinColumns = @JoinColumn(name = "hand_id"))
    @Column(name = "card", nullable = false)
    @Enumerated(EnumType.STRING)
    public List<Card> getCards() {
        return cards;
    }

    @Column(name = "pot")
    public int getPot() {
        return pot;
    }

    @Column(name = "total_bet_amount")
    public int getTotalBetAmount() {
        return totalBetAmount;
    }


    /**
     * In No Limit poker, the minimum bet size is twice the previous bet.  Use this field to determine
     * what that amount would be.
     *
     * @return The last bet/raise amount
     */
    @Column(name = "bet_amount")
    public int getLastBetAmount() {
        return lastBetAmount;
    }

    @Transient
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HandEntity)) {
            return false;
        }
        return ((HandEntity) o).getId() == this.getId();
    }
}
