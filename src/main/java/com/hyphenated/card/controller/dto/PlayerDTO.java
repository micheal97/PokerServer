package com.hyphenated.card.controller.dto;

import java.util.UUID;

public record PlayerDTO(
        UUID id,
        String name,
        int chips,
        int tableChips,
        int gamePosition,
        boolean sittingOut) {
}
