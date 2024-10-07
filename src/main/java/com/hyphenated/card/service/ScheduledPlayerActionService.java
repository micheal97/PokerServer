package com.hyphenated.card.service;

import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.domain.PlayerHandRoundAction;

import java.util.concurrent.ScheduledExecutorService;

public interface ScheduledPlayerActionService {

    ScheduledExecutorService scheduleDefaultAction(Player player, Game game);

    void handlePlayerRoundAction(PlayerHandRoundAction action, Player player, int betAmount, Game game);


}
