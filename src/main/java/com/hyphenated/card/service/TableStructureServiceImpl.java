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

import com.hyphenated.card.dao.PlayerDao;
import com.hyphenated.card.dao.TableStructureDao;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.domain.TableStructure;
import com.hyphenated.card.view.GameAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TableStructureServiceImpl implements TableStructureService {

    @Autowired
    private TableStructureDao tableStructureDao;

    @Autowired
    private PlayerDao playerDao;

    @Override
    @Transactional(readOnly = true)
    public TableStructure getTableStructureById(long id) {
        return tableStructureDao.findById(id);
    }

    @Override
    @Transactional
    public TableStructure saveTableStructure(TableStructure tableStructure) {
        return tableStructureDao.save(tableStructure);
    }

    @Override
    @Transactional
    @GameAction
    public TableStructure startGame(TableStructure tableStructure) {
        tableStructure = tableStructureDao.merge(tableStructure);
        if (tableStructure.getPlayers().size() < 2) {
            throw new IllegalStateException("Not Enough Players");
        }
        if (tableStructure.getPlayers().size() > 10) {
            throw new IllegalStateException("Too Many Players");
        }
        if (tableStructure.isStarted()) {
            throw new IllegalStateException("Game already started");
        }

        //Set started flag
        tableStructure.setStarted(true);

        //Get all players associated with the game.
        //Assign random position.  Save the player.
        List<Player> players = new ArrayList<Player>(tableStructure.getPlayers());
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            p.setGamePosition(i + 1);
            players.forEach(player -> playerDao.save(player));
        }

        //Set Button and Big Blind.  Button is position 1 (index 0)
        Collections.sort(players);
        tableStructure.setPlayerInBTN(players.get(0));

        //Save and return the updated game
        return tableStructureDao.merge(tableStructure);
    }

    @Override
    @Transactional
    public Player addNewPlayerToTableStructure(TableStructure tableStructure, Player player, int startingTableChips) {
        if (tableStructure.getMaxPlayers() == tableStructure.getPlayers().size()) {
            throw new IllegalStateException("No new players may join");
        }
        if (tableStructure.getPlayers().size() >= 10) {
            throw new IllegalStateException("Cannot have more than 10 players in one game");
        }
        if (startingTableChips > player.getChips() || tableStructure.getBlindLevel().getBigBlind() > player.getChips()) {
            throw new IllegalStateException("Not enough Chips");
        }
        if (tableStructure.getBlindLevel().getBigBlind() * 50 < player.getChips()) {
            throw new IllegalStateException("Too many Chips");
        }
        player.setTableStructure(tableStructure);
        player = playerDao.save(player);
        tableStructure.setPlayers(tableStructure.getPlayers());
        tableStructureDao.save(tableStructure);
        return player;
    }

    @Override
    @Transactional
    public Player savePlayer(Player player) {
        return playerDao.save(player);
    }

    @Override
    public List<TableStructure> findAll() {
        return tableStructureDao.findAll();
    }

}
