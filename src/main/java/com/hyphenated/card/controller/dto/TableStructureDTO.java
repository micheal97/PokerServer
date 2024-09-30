package com.hyphenated.card.controller.dto;

import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.domain.GameStatus;
import com.hyphenated.card.domain.Player;

import java.util.UUID;

public record TableStructureDTO(
        UUID id,
        BlindLevel blindLevel,
        int maxPlayers,
        String name,
        int players,
        GameStatus gameStatus,
        Player privateGameCreator) {
}
