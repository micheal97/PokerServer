package com.hyphenated.card.service;

import com.hyphenated.card.domain.PlayerHand;
import com.hyphenated.card.domain.PlayerHandRoundAction;

import java.util.concurrent.ScheduledExecutorService;

public interface ScheduledPlayerActionService {

    ScheduledExecutorService scheduleDefaultAction(PlayerHand playerHand);

    void handlePlayerRoundAction(PlayerHandRoundAction action, PlayerHand playerHand, int betAmount);


}
