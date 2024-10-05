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

@Service
public class GameServiceImpl implements GameService {

    @Autowired
    private GameDao GameDao;

    @Autowired
    private PlayerDao playerDao;

    @Override
    @Transactional(readOnly = true)//TODO:readonly everywhere
    public Optional<Game> findGameById(UUID id) {
        return GameDao.findById(id);
    }

    @Override
    @Transactional
    public Game saveGame(Game game) {
        return GameDao.save(game);
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
            Collections.shuffle(players);
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                p.setGamePosition(i + 1);
                players.forEach(player -> playerDao.save(player));
            }

            //Set Button and Big Blind.  Button is position 1 (index 0)
            Collections.sort(players);
            game.setPlayerInBTN(players.get(0));

            //Save and return the updated game
            GameDao.save(game);
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
        player.setGame(game);
        player = playerDao.save(player);
        game.addPlayer(player);
        GameDao.save(game);
    }

    @Override
    @Transactional
    public void removePlayerFromGame(Player player) {
        Game game = player.getGame();
        game.removePlayer(player);
        GameDao.save(game);
    }


    @Override
    public List<Game> findAll() {
        return GameDao.findAll();
    }

    @Override
    public void updateTables(List<String> blindLevels) {
        blindLevels.forEach(blindLevel -> GameDao.updateTables(blindLevel));
    }


}
