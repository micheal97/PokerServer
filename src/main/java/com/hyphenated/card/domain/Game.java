package com.hyphenated.card.domain;

import com.hyphenated.card.controller.dto.GameDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;
import java.util.SortedSet;
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
    private SortedSet<Player> players;
    @Embedded
    @Nullable
    private HandEntity hand;
    private GameStatus gameStatus = GameStatus.NOT_STARTED;
    private boolean privateGame;

    public Optional<Player> findPlayerInBTN() {
        return players.stream().filter(Player::isPlayerInButton).findAny();
    }

    @Nullable
    public Player getPrivateGameCreator() {
        if (isPrivateGame()) {
            return players.stream().filter(Player::isPrivateGameCreator).findAny().orElseThrow(() -> new IllegalArgumentException("Private Game Creator left"));
        } else {
            return null;
        }
    }

    public GameDTO getGameDTO() {
        return new GameDTO(id, blindLevel, maxPlayers, name, players.size(), gameStatus);
    }

    public boolean addPlayer(Player player) {
        return players.add(player);
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
