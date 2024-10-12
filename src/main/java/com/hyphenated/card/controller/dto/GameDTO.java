package com.hyphenated.card.controller.dto;

import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.domain.GameStatus;
import lombok.NonNull;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record GameDTO(
        @NonNull UUID id,
        @NonNull BlindLevel blindLevel,
        int maxPlayers,
        @NonNull String name,
        @NonNull List<String> players,
        @NonNull GameStatus gameStatus) implements Serializable {
}
