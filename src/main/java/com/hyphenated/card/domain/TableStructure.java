package com.hyphenated.card.domain;

import jakarta.persistence.*;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "table_structure")
@Setter
public class TableStructure implements Serializable {
    private UUID id;
    private BlindLevel blindLevel;
    private int maxPlayers;
    private Player playerInBTN;
    private String name;
    private Set<Player> players;
    private HandEntity currentHand;
    private GameStatus gameStatus;

    @Column(name = "table_structure_id")
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    public UUID getId() {
        return id;
    }

    @Column(name = "blind_level")
    @Enumerated(EnumType.STRING)
    public BlindLevel getBlindLevel() {
        return blindLevel;
    }

    @Column(name = "maxPlayers")
    public int getMaxPlayers() {
        return maxPlayers;
    }


    @OneToOne
    @JoinColumn(name = "btn_player_id")
    public Player getPlayerInBTN() {
        return playerInBTN;
    }


    @OneToOne(mappedBy = "table_structure", fetch = FetchType.LAZY)
    public Set<Player> getPlayers() {
        return players;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_hand_id")
    public HandEntity getCurrentHand() {
        return currentHand;
    }

    @Column(name = "game_status")
    public GameStatus getGameStatus() {
        return gameStatus;
    }

}
