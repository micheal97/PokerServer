package com.hyphenated.card.controller.dto;

import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.domain.GameStatus;

import java.util.UUID;

public record GameDTO(
        UUID id,
        BlindLevel blindLevel,
        int maxPlayers,
        String name,
        int players,
        GameStatus gameStatus) {
}
