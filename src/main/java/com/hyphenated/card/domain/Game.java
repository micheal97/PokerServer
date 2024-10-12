package com.hyphenated.card.domain;

import com.hyphenated.card.controller.dto.GameDTO;
import jakarta.persistence.*;
import lombok.*;

import javax.annotation.Nullable;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

@Getter
@Entity
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @NonNull
    private final BlindLevel blindLevel;
    private final int maxPlayers;
    @NonNull
    private final String name;
    @Setter(value = AccessLevel.NONE)
    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    @NonNull
    private SortedSet<Player> players = new TreeSet<>();
    @Embedded
    @Nullable
    private HandEntity hand = null;
    @NonNull
    private GameStatus gameStatus = GameStatus.NOT_STARTED;
    private final boolean privateGame;

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
        int tableChips = player.getTableChips();
        player.addChips(tableChips);
        player.removeTableChips(tableChips);
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
