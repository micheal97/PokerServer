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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@EqualsAndHashCode
@Table(name = "hand")
public class HandEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "hand_id")
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_structure_id")
    private TableStructure tableStructure;
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @JoinColumn(name = "board_id")
    private BoardEntity board;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "handEntity", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private Set<PlayerHand> players;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_to_act_id")
    private Player currentToAct;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_bet_or_raise_id")
    private Player lastBetOrRaise;
    @Column(name = "blind_level")
    @Enumerated(EnumType.STRING)
    private BlindLevel blindLevel;
    @ElementCollection(targetClass = Card.class)
    @JoinTable(name = "hand_deck", joinColumns = @JoinColumn(name = "hand_id"))
    @Column(name = "card", nullable = false)
    @Enumerated(EnumType.STRING)
    private List<Card> cards;
    @Column(name = "pot")
    private int pot;
    @Column(name = "total_bet_amount")
    private int totalBetAmount;
    /**
     * -- GETTER --
     * In No Limit poker, the minimum bet size is twice the previous bet.  Use this field to determine
     * what that amount would be.
     *
     * @return The last bet/raise amount
     */
    @Column(name = "bet_amount")
    private int lastBetAmount;

    public Optional<PlayerHand> findPlayerHandById(UUID playerHandId) {
        return players.stream().filter(playerHand -> playerHand.getId() == playerHandId).findAny();
    }

    public Optional<PlayerHand> findPlayerHandByPlayerId(UUID playerId) {
        return players.stream().filter(playerHand -> playerHand.getPlayer().getId() == playerId).findAny();
    }

    public void removePlayer(PlayerHand playerHand) {
        players.remove(playerHand);
    }

    public Set<PlayerHand> addPlayer(PlayerHand playerHand) {
        players.add(playerHand);
        return players;
    }
}
