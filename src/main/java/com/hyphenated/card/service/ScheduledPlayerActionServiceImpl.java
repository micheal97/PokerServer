package com.hyphenated.card.service;

import com.hyphenated.card.controller.TableTasksController;
import com.hyphenated.card.controller.dto.PlayerBet;
import com.hyphenated.card.dao.PlayerDao;
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
    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private PokerHandService pokerHandService;
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
    public ScheduledExecutorService scheduleDefaultAction(Player player, Game game) {
        ScheduledExecutorService executor = getIdleScheduler().getScheduledExecutor();
        executor.schedule(() -> {
                    player.addStrike();
                    if (player.getStrikes() > 2) {
                        game.removePlayer(player);
                    }
                    playerDao.save(player);
                    handlePlayerRoundAction(PlayerHandRoundAction.FOLD, player, 0, game);
                },
                8,
                TimeUnit.SECONDS);
        return executor;
    }

    @Override
    public void handlePlayerRoundAction(PlayerHandRoundAction action, Player player, int betAmount, Game game) {
        Player nextPlayer = switch (action) {
            case FOLD -> playerActionService.fold(player, game);
            case CALL_ANY -> playerActionService.callAny(player, game);
            case CALL_CURRENT -> playerActionService.callCurrent(player, game, betAmount);
            case CHECK -> playerActionService.check(player, game);
            case BET -> playerActionService.bet(player, game, betAmount);
        };
        if (nextPlayer == null) {
            pokerHandService.handleNextGameStatus(game);
            tableTasksController.gameStopped(game.getId());
        } else {
            String playerName = player.getName();
            UUID gameId = game.getId();
            PlayerHand playerHand = player.getPlayerHand();
            PlayerHand nextPlayerHand = nextPlayer.getPlayerHand();
            switch (action) {
                case FOLD -> tableTasksController.playerFolded(playerName, gameId);
                case CALL_ANY, CALL_CURRENT -> tableTasksController.playerCalled(playerName, gameId);
                case CHECK -> tableTasksController.playerChecked(playerName, gameId);
                case BET -> tableTasksController.playerBet(new PlayerBet(
                                playerName,
                                playerHand.getBetAmount()),
                        gameId);
            }
            playerHand.getScheduledExecutorService().shutdownNow();
            nextPlayerHand.setScheduledExecutorService(scheduleDefaultAction(nextPlayer, game));
            tableTasksController.playersTurn(player.getName(), gameId);
            if (nextPlayerHand.getRoundAction() != null) {
                if (nextPlayerHand.getRoundAction() == PlayerHandRoundAction.CALL_CURRENT) {
                    handlePlayerRoundAction(nextPlayerHand.getRoundAction(), nextPlayer, playerHand.getBetAmount(), game);
                }
                handlePlayerRoundAction(nextPlayerHand.getRoundAction(), nextPlayer, 0, game);
            }
        }
    }


}
