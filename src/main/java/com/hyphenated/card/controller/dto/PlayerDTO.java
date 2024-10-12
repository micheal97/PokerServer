package com.hyphenated.card.controller.dto;

import lombok.NonNull;

import java.io.Serializable;
import java.util.UUID;

public record PlayerDTO(
        @NonNull UUID id,
        @NonNull String name,
        int chips,
        int tableChips,
        int gamePosition,
        boolean sittingOut) implements Serializable {
}
