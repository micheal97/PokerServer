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
import com.hyphenated.card.domain.*;
import com.hyphenated.card.view.PlayerStatusObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PlayerServiceManagerImpl implements PlayerServiceManager {

    @Autowired
    private TableStructureService tableStructureService;

    @Autowired
    private PlayerActionService playerActionService;

    @Autowired
    private PokerHandService handService;

    @Autowired
    private PlayerDao playerDao;

    @Override
    @Cacheable("game")
    public PlayerStatusObject buildPlayerStatus(long tableStructureId, UUID playerId) {
        TableStructure tableStructure = tableStructureService.getTableStructureById(tableStructureId);
        Player player = playerActionService.getPlayerById(playerId);
        PlayerStatusObject results = new PlayerStatusObject();
        //Get the player status.
        //In the special case of preflop, player is not current to act, see if the player is SB or BB
        PlayerStatus playerStatus = playerActionService.getPlayerStatus(player);
        if (playerStatus == PlayerStatus.WAITING && tableStructure.getGameStatus() == GameStatus.PREFLOP) {
            if (player.equals(handService.getPlayerInSB(tableStructure.getCurrentHand()))) {
                playerStatus = PlayerStatus.POST_SB;
            } else if (player.equals(handService.getPlayerInBB(tableStructure.getCurrentHand()))) {
                playerStatus = PlayerStatus.POST_BB;
            }
        }
        results.setStatus(playerStatus);

        results.setChips(player.getChips());
        results.setSmallBlind(tableStructure.getBlindLevel().getSmallBlind());
        results.setBigBlind(tableStructure.getBlindLevel().getBigBlind());
        if (tableStructure.getCurrentHand() != null) {
            HandEntity hand = tableStructure.getCurrentHand();
            PlayerHand playerHand = null;
            for (PlayerHand ph : hand.getPlayers()) {
                if (ph.getPlayer().equals(player)) {
                    playerHand = ph;
                    break;
                }
            }
            if (playerHand != null) {
                results.setCard1(playerHand.getHand().getCard(0));
                results.setCard2(playerHand.getHand().getCard(1));
                results.setAmountBetRound(playerHand.getRoundBetAmount());

                int toCall = hand.getTotalBetAmount() - playerHand.getRoundBetAmount();
                toCall = Math.min(toCall, player.getChips());
                if (toCall > 0) {
                    results.setAmountToCall(toCall);
                }
            }
        }
        return results;
    }

    @Override
    @Transactional
    public Player savePlayer(Player player) {
        return playerDao.save(player);
    }

    @Override
    @Transactional
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
    @Transactional
    public Player findPlayerByNameAndPassword(String name, String password) {
        return playerDao.findByNameAndPassword(name, password);
    }

    @Override
    @Transactional
    public Player findPlayerById(UUID id) {
        return playerDao.findById(id);
    }


}
