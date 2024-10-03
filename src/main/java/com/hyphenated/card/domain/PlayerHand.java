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
import com.hyphenated.card.holder.Hand;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

@Getter
@Setter
@Entity
@Table(name = "player_hand")
public class PlayerHand implements Comparable<PlayerHand>, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "player_hand_id")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;
    @ManyToOne
    @JoinColumn(name = "hand_id")
    private HandEntity handEntity;
    @Column(name = "card1")
    @Enumerated(EnumType.STRING)
    @Getter(value = AccessLevel.NONE)
    private Card card1;
    @Column(name = "card2")
    @Enumerated(EnumType.STRING)
    @Getter(value = AccessLevel.NONE)
    private Card card2;
    /**
     * -- GETTER --
     * Represents the amount of chips the player has contributed to the pot
     * for this entire hand
     */
    @Column(name = "bet_amount")
    private int betAmount;
    /**
     * -- GETTER --
     * Represents the amount of chips the player has contributed to the pot for only this
     * current betting round
     */
    @Column(name = "round_bet_amount")
    private int roundBetAmount;
    @Column(name = "round_action")
    private PlayerHandRoundAction roundAction;
    private ScheduledExecutorService scheduledExecutorService;


    /**
     * Get the hole cards for the player
     *
     * @return {@link Hand} container for the two hole cards. Null if both hole cards not specified
     */
    @Transient
    public Hand getHand() {
        if (card1 == null || card2 == null) {
            return null;
        }
        return new Hand(card1, card2);
    }

    @Transient
    @Override
    public int compareTo(PlayerHand o) {
        return this.getPlayer().compareTo(o.getPlayer());
    }

}
