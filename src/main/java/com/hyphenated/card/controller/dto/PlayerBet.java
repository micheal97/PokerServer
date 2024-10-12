package com.hyphenated.card.controller.dto;


import lombok.NonNull;

import java.io.Serializable;

public record PlayerBet(@NonNull String playerName, int betAmount) implements Serializable {

}
