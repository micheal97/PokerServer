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
import com.hyphenated.card.domain.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlayerServiceManagerImpl implements PlayerServiceManager {

    @Autowired
    private GameService GameService;

    @Autowired
    private PlayerActionService playerActionService;

    @Autowired
    private PokerHandService handService;

    @Autowired
    private PlayerDao playerDao;


    @Override
    @Transactional
    public Player savePlayer(Player player) {
        return playerDao.save(player);
    }

    @Override

    @Transactional(readOnly = true)
    public boolean playerExistsByName(String name) {
        return playerDao.existsByName(name);
    }

    @Override
    @Transactional
    public boolean registerPlayer(Player player) {
        if (playerDao.existsByName(player.getName())) {
            return false;
        }
        playerDao.save(player);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Player> findPlayerByNameAndPassword(String name, String password) {
        return playerDao.findByNameAndPassword(name, password);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Player> findPlayerById(UUID id) {
        return playerDao.findById(id);
    }

    @Override
    public Optional<Player> buyPayments(String name, MultiValueMap<String, List<String>> payments) {
        return Optional.empty();
    }


}
