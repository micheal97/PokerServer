package com.hyphenated.card.domain;

import com.hyphenated.card.SharedUtils;
import com.hyphenated.card.dto.GameDTO;
import com.hyphenated.card.enums.BlindLevel;
import com.hyphenated.card.enums.GameStatus;
import jakarta.persistence.*;
import lombok.*;

import javax.annotation.Nullable;
import java.util.SortedSet;
import java.util.TreeSet;

@Getter
@Entity
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
public class Game {
    @Id
    private String id = SharedUtils.generateIdStrings();
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
    @Setter(value = AccessLevel.NONE)
    private final String password;

    @Nullable
    public Player getPrivateGameCreator() {
        if (isPrivateGame()) {
            return players.stream().filter(Player::isPrivateGameCreator).findAny().orElseThrow(() -> new IllegalStateException("Private Game Creator left"));
        } else {
            return null;
        }
    }

    public GameDTO getGameDTO() {
        return new GameDTO(id, blindLevel, maxPlayers, name, players.stream().map(Player::getPlayerDTO).toList(), gameStatus);
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
