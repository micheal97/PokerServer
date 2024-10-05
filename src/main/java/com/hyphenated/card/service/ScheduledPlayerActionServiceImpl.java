package com.hyphenated.card.service;

import com.hyphenated.card.controller.TableTasksController;
import com.hyphenated.card.controller.dto.PlayerBet;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.domain.PlayerHand;
import com.hyphenated.card.domain.PlayerHandRoundAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduledPlayerActionServiceImpl implements ScheduledPlayerActionService {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final int POOL_SIZE = 10;
    @Autowired
    private PlayerActionService playerActionService;
    @Autowired
    private TableTasksController tableTasksController;
    List<ThreadPoolTaskScheduler> schedulers = new ArrayList<>();

    {
        List<ThreadPoolTaskScheduler> idleSchedulers = new ArrayList<>(2);
        scheduler.scheduleAtFixedRate(() -> {
            schedulers.forEach(scheduler1 -> {
                if (scheduler1.getActiveCount() == 0) {
                    idleSchedulers.add(scheduler1);
                }
            });
            if (idleSchedulers.size() > 1) {
                idleSchedulers.remove(0);
                idleSchedulers.forEach(ExecutorConfigurationSupport::destroy);
            }
        }, 0, 15, TimeUnit.SECONDS);
    }

    private ThreadPoolTaskScheduler newScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        schedulers.add(scheduler);
        return scheduler;
    }

    private ThreadPoolTaskScheduler getIdleScheduler() {
        return schedulers.stream()
                .filter(scheduler1 -> scheduler1.getActiveCount() < POOL_SIZE)
                .findAny()
                .orElse(newScheduler());
    }

    @Override
    public ScheduledExecutorService scheduleDefaultAction(Player player) {
        ScheduledExecutorService executor = getIdleScheduler().getScheduledExecutor();
        executor.schedule(() ->
                        handlePlayerRoundAction(PlayerHandRoundAction.FOLD, player, 0),
                8,
                TimeUnit.SECONDS);
        return executor;
    }

    @Override
    public void handlePlayerRoundAction(PlayerHandRoundAction action, Player player, int betAmount) {
        Player nextPlayer = switch (action) {
            case FOLD -> playerActionService.fold(player);
            case CALL_ANY -> playerActionService.callAny(player);
            case CALL_CURRENT -> playerActionService.callCurrent(player, betAmount);
            case CHECK -> playerActionService.check(player);
            case BET -> playerActionService.bet(player, betAmount);
        };
        if (nextPlayer == null) {
            tableTasksController.gameStopped(player.getGame().getId());
        } else {
            String playerName = player.getName();
            Game game = nextPlayer.getGame();
            UUID gameId = game.getId();
            PlayerHand playerHand = player.getPlayerHand();
            PlayerHand nextPlayerHand = nextPlayer.getPlayerHand();
            switch (action) {
                case FOLD -> tableTasksController.playerFolded(playerName, gameId);
                case CALL_ANY, CALL_CURRENT -> tableTasksController.playerCalled(playerName, gameId);
                case CHECK -> tableTasksController.playerChecked(playerName, gameId);
                case BET -> tableTasksController.playerBet(new PlayerBet(
                                playerName,
                                playerHand.getRoundBetAmount()),
                        gameId);
            }
            playerHand.getScheduledExecutorService().shutdownNow();
            nextPlayerHand.setScheduledExecutorService(scheduleDefaultAction(nextPlayer));
            tableTasksController.playersTurn(player.getName(), gameId);
            if (nextPlayerHand.getRoundAction() != null) {
                handlePlayerRoundAction(nextPlayerHand.getRoundAction(), nextPlayer, nextPlayerHand.getBetAmount());
            }
        }
    }


}
