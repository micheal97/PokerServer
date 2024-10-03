package com.hyphenated.card.service;

import com.hyphenated.card.controller.TableTasksController;
import com.hyphenated.card.controller.dto.PlayerBet;
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
    public ScheduledExecutorService scheduleDefaultAction(PlayerHand playerHand) {
        ScheduledExecutorService executor = getIdleScheduler().getScheduledExecutor();
        executor.schedule(() ->
                        handlePlayerRoundAction(PlayerHandRoundAction.FOLD, playerHand, 0),
                8,
                TimeUnit.SECONDS);
        return executor;
    }

    @Override
    public void handlePlayerRoundAction(PlayerHandRoundAction action, PlayerHand playerHand, int betAmount) {
        PlayerHand nextPlayerHand = switch (action) {
            case FOLD -> playerActionService.fold(playerHand);
            case CALL_ANY -> playerActionService.callAny(playerHand);
            case CALL_CURRENT -> playerActionService.callCurrent(playerHand, betAmount);
            case CHECK -> playerActionService.check(playerHand);
            case BET -> playerActionService.bet(playerHand, betAmount);
        };
        if (nextPlayerHand == null) {
            tableTasksController.gameStopped(playerHand.getPlayer().getTableStructure().getId());
        } else {
            String playerName = playerHand.getPlayer().getName();
            UUID tableStructureId = nextPlayerHand.getHandEntity().getTableStructure().getId();
            switch (action) {
                case FOLD -> tableTasksController.playerFolded(playerName, tableStructureId);
                case CALL_ANY, CALL_CURRENT -> tableTasksController.playerCalled(playerName, tableStructureId);
                case CHECK -> tableTasksController.playerChecked(playerName, tableStructureId);
                case BET ->
                        tableTasksController.playerBet(new PlayerBet(playerName, playerHand.getRoundBetAmount()), tableStructureId);
            }
            playerHand.getScheduledExecutorService().shutdownNow();
            nextPlayerHand.setScheduledExecutorService(scheduleDefaultAction(nextPlayerHand));
            tableTasksController.playersTurn(playerHand.getPlayer().getName(), tableStructureId);
            if (nextPlayerHand.getRoundAction() != null) {
                handlePlayerRoundAction(nextPlayerHand.getRoundAction(), nextPlayerHand, nextPlayerHand.getBetAmount());
            }
        }
    }


}
