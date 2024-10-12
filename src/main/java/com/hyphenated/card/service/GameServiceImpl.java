/*
The MIT License (MIT)

Copyright (c) 2013 Jacob Kanipe-Illig

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package com.hyphenated.card.service;

import com.hyphenated.card.dao.GameDao;
import com.hyphenated.card.dao.PlayerDao;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.GameStatus;
import com.hyphenated.card.domain.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ScheduledExecutorTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
public class GameServiceImpl implements GameService {

    @Autowired
    private GameDao gameDao;

    @Autowired
    private PlayerDao playerDao;

    @Autowired
    ScheduledPlayerActionService scheduledPlayerActionService;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findGameById(UUID id) {
        return gameDao.findById(id);
    }

    @Override
    @Transactional
    public Game saveGame(Game game) {
        return gameDao.save(game);
    }

    @Override
    @Transactional
    public void startGame(Game game) {
        new ScheduledExecutorTask(() -> {
            if (game.getPlayers().size() < 2) {
                throw new IllegalStateException("Not Enough Players");
            }
            if (game.getPlayers().size() > 10 || game.getPlayers().size() > game.getMaxPlayers()) {
                throw new IllegalStateException("Too Many Players");
            }
            if (!game.getGameStatus().equals(GameStatus.NOT_STARTED)) {
                throw new IllegalStateException("Game already started");
            }

            //Get all players associated with the game.
            //Assign random position.  Save the player.
            List<Player> players = new ArrayList<>(game.getPlayers());
            players.forEach(player -> {
                player.setGamePosition(players.size() - 1 >= player.getGamePosition() ? player.getGamePosition() + 1 : 0);
                playerDao.save(player);
            });

            //Set Button and Big Blind.  Button is position 1 (index 0)
            Collections.sort(players);
            Player player = players.get(0);
            player.setPlayerInButton(true);
            scheduledPlayerActionService.scheduleDefaultAction(player, game);
            //Save and return the updated game
            playerDao.save(player);
            gameDao.save(game);
        }, 4000);
    }

    @Override
    @Transactional
    public void addNewPlayerToGame(Game game, Player player, int startingTableChips) {
        if (game.getMaxPlayers() >= game.getPlayers().size()) {
            throw new IllegalStateException("No new players may join");
        }
        if (game.getPlayers().size() >= 10) {
            throw new IllegalStateException("Cannot have more than 10 players in one game");
        }
        if (startingTableChips > player.getChips() || game.getBlindLevel().getBigBlind() > player.getChips()) {
            throw new IllegalStateException("Not enough Chips");
        }
        if (game.getBlindLevel().getBigBlind() * 50 < player.getChips()) {
            throw new IllegalStateException("Too many Chips");
        }
        player.removeChips(startingTableChips);
        player.addTableChips(startingTableChips);
        game.addPlayer(player);
        gameDao.save(game);
    }

    @Override
    public void removePlayerFromGame(Game game, Player player) {
        game.removePlayer(player);
        gameDao.save(game);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Game> findAll() {
        return gameDao.findAll();
    }

    @Override
    public void updateTables(List<String> blindLevels) {
        blindLevels.forEach(blindLevel -> gameDao.updateTables(blindLevel));
    }


}
