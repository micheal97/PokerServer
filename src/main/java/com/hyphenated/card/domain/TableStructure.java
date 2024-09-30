package com.hyphenated.card.domain;

import com.hyphenated.card.controller.dto.TableStructureDTO;
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
    private Player privateGameCreator;

    public TableStructureDTO getTableStructureDTO() {
        return new TableStructureDTO(id, blindLevel, maxPlayers, name, players.size(), gameStatus, privateGameCreator);
    }

    @Column(name = "table_structure_id")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_hand_id")
    public HandEntity getCurrentHand() {
        return currentHand;
    }

    @Column(name = "game_status")
    public GameStatus getGameStatus() {
        return gameStatus;
    }

    @Column(name = "private_game_creator")
    public Player getPrivateGameCreator() {
        return privateGameCreator;
    }

}
