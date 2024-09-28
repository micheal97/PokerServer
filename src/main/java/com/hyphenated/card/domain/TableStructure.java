package com.hyphenated.card.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Set;

@Entity
@Table(name = "table_structure")
public class TableStructure implements Serializable {
    private long id;
    private BlindLevel blindLevel;
    private int maxPlayers;
    private Player playerInBTN;
    private String name;
    private boolean isStarted;
    private Set<Player> players;
    private HandEntity currentHand;

    @Column(name = "table_structure_id")
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name = "blind_level")
    @Enumerated(EnumType.STRING)
    public BlindLevel getBlindLevel() {
        return blindLevel;
    }

    public void setBlindLevel(BlindLevel blindLevel) {
        this.blindLevel = blindLevel;
    }

    @Column(name = "maxPlayers")
    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    @OneToOne
    @JoinColumn(name = "btn_player_id")
    public Player getPlayerInBTN() {
        return playerInBTN;
    }

    public void setPlayerInBTN(Player playerInBTN) {
        this.playerInBTN = playerInBTN;
    }

    @OneToMany(mappedBy = "table_structure", fetch = FetchType.LAZY)
    public Set<Player> getPlayers() {
        return players;
    }

    public void setPlayers(Set<Player> players) {
        this.players = players;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "is_started")
    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean isStarted) {
        this.isStarted = isStarted;
    }

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_hand_id")
    public HandEntity getCurrentHand() {
        return currentHand;
    }

    public void setCurrentHand(HandEntity currentHand) {
        this.currentHand = currentHand;
    }

}
