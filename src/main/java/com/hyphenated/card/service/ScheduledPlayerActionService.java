package com.hyphenated.card.service;

import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.enums.PlayerHandRoundAction;

public interface ScheduledPlayerActionService {

    void scheduleDefaultAction(Player next, Game game);

    void handlePlayerRoundAction(PlayerHandRoundAction action, Player player, int betAmount, Game game);


}
