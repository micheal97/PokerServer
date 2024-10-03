package com.hyphenated.card.domain;

import com.hyphenated.card.controller.dto.TableStructureDTO;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Table(name = "table_structure")
@Setter
public class TableStructure implements Serializable {
    @Column(name = "table_structure_id")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "blind_level")
    @Enumerated(EnumType.STRING)
    private BlindLevel blindLevel;
    @Column(name = "maxPlayers")
    private int maxPlayers;
    @OneToOne
    @JoinColumn(name = "btn_player_id")
    private Player playerInBTN;
    @Column(name = "name")
    private String name;
    @Setter(value = AccessLevel.NONE)
    @OneToMany(mappedBy = "table_structure", fetch = FetchType.LAZY)
    private Set<Player> players;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_hand_id")
    private HandEntity currentHand;
    @Column(name = "game_status")
    private GameStatus gameStatus = GameStatus.NOT_STARTED;
    @Column(name = "private_game_creator")
    private Player privateGameCreator;

    public TableStructureDTO getTableStructureDTO() {
        return new TableStructureDTO(id, blindLevel, maxPlayers, name, players.size(), gameStatus, privateGameCreator);
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
