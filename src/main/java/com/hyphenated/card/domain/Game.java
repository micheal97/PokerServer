package com.hyphenated.card.domain;

import com.hyphenated.card.controller.dto.GameDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Setter
public class Game implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Enumerated(EnumType.STRING)
    private BlindLevel blindLevel;
    private int maxPlayers;
    private String name;
    @Setter(value = AccessLevel.NONE)
    @OneToMany(mappedBy = "table_structure", fetch = FetchType.LAZY)
    @JoinColumn
    private Set<Player> players;
    @Embedded
    private HandEntity currentHand;
    private GameStatus gameStatus = GameStatus.NOT_STARTED;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Player privateGameCreator;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Player playerInBTN;

    public GameDTO getGameDTO() {
        return new GameDTO(id, blindLevel, maxPlayers, name, players.size(), gameStatus, privateGameCreator);
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }


    public void setNextGameStatus() {
        this.gameStatus = gameStatus.next();
    }

    public void setGameStatusEndHand() {
        this.gameStatus = GameStatus.END_HAND;
    }

    public void setGameStatusNotStarted() {
        this.gameStatus = GameStatus.NOT_STARTED;
    }

}
