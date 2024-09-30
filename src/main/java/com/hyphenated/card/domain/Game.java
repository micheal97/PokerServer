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

import jakarta.persistence.*;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Setter
@Entity
@Table(name = "game")
public class Game implements Serializable {

    private UUID id;
    private int playersRemaining;
    private Player playerInBTN;
    private GameType gameType;
    private String name;
    private boolean started;
    private Set<Player> players;
    private HandEntity currentHand;
    private GameStructure gameStructure;

    @Column(name = "game_id")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public UUID getId() {
        return id;
    }

    @Column(name = "players_left")
    public int getPlayersRemaining() {
        return playersRemaining;
    }

    @OneToOne
    @JoinColumn(name = "btn_player_id")
    public Player getPlayerInBTN() {
        return playerInBTN;
    }

    @Column(name = "game_type")
    @Enumerated(EnumType.STRING)
    public GameType getGameType() {
        return gameType;
    }

    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    public Set<Player> getPlayers() {
        return players;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    @Column(name = "is_started")
    public boolean isStarted() {
        return started;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_hand_id")
    public HandEntity getCurrentHand() {
        return currentHand;
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    @JoinColumn(name = "game_structure_id")
    public GameStructure getGameStructure() {
        return gameStructure;
    }

}
