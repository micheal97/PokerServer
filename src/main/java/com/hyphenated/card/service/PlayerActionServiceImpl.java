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

import com.hyphenated.card.dao.HandDao;
import com.hyphenated.card.dao.PlayerDao;
import com.hyphenated.card.domain.*;
import com.hyphenated.card.util.PlayerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerActionServiceImpl implements PlayerActionService {

    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private HandDao handDao;
    @Autowired
    private PokerHandService pokerHandService;
    @Autowired
    private GameService GameService;


    private boolean playerIsNotCurrentToAct(Player player, HandEntity hand, PlayerHandRoundAction action, int betAmount) {
        player.getPlayerHand().setRoundBetAmount(betAmount);
        return playerIsNotCurrentToAct(player, hand, action);
    }

    private boolean playerIsNotCurrentToAct(Player player, HandEntity hand, PlayerHandRoundAction action) {
        if (player.equals(hand.getCurrentToAct())) {
            return false;
        }
        hand.removePlayer(player);
        player.getPlayerHand().setRoundAction(action);
        hand.addPlayer(player);
        handDao.save(hand);
        playerDao.save(player);
        return true;
    }

    @Override
    @Transactional
    public Player fold(Player player, Game game) {
        HandEntity hand = game.getCurrentHand();
        //fold out of turn
        if (playerIsNotCurrentToAct(player, hand, PlayerHandRoundAction.FOLD)) return null;
        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.removePlayer(player);
        hand.setCurrentToAct(next);
        handDao.save(hand);
        return next;
        //TODO: Überall wie hier
    }


    @Override
    @Transactional
    public Player check(Player player, Game game) {
        HandEntity hand = game.getCurrentHand();
        //check out of turn
        if (playerIsNotCurrentToAct(player, hand, PlayerHandRoundAction.CHECK)) return null;
        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        handDao.save(hand);
        return next;
    }

    @Override
    @Transactional
    public Player bet(Player player, Game game, int betAmount) {
        PlayerHand playerHand = player.getPlayerHand();
        HandEntity hand = game.getCurrentHand();

        //Bet must meet the minimum of twice the previous bet.  Call bet amount and raise exactly that amount or more
        //Alternatively, if there is no previous bet, the first bet must be at least the big blind
        if (betAmount < hand.getLastBetAmount() || betAmount < game.getBlindLevel().getBigBlind()) {
            return null;
        }

        int toCall = hand.getTotalBetAmount() - player.getPlayerHand().getRoundBetAmount();
        int total = betAmount + toCall;
        if (total > player.getChips()) {
            total = player.getChips();
            betAmount = total - toCall;
        }
        int playerHandRoundBetAmount = playerHand.getRoundBetAmount() + betAmount;
        //bet out of turn
        if (playerIsNotCurrentToAct(player, hand, PlayerHandRoundAction.BET, playerHandRoundBetAmount)) return null;

        playerHand.setRoundBetAmount(playerHandRoundBetAmount);
        playerHand.setBetAmount(playerHand.getBetAmount() + total);
        player.setChips(player.getChips() - total);
        hand.setPot(hand.getPot() + total);
        hand.setLastBetAmount(betAmount);
        hand.setTotalBetAmount(hand.getTotalBetAmount() + betAmount);
        hand.setLastBetOrRaise(player);
        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        handDao.save(hand);
        playerDao.save(player);
        return next;
    }

    @Override
    @Transactional
    public Player callAny(Player player, Game game) {
        HandEntity hand = game.getCurrentHand();
        PlayerHand playerHand = player.getPlayerHand();
        //call out of turn
        if (playerIsNotCurrentToAct(player, hand, PlayerHandRoundAction.CALL_ANY)) return null;


        int toCall = hand.getLastBetAmount() - playerHand.getRoundBetAmount();
        toCall = Math.min(toCall, player.getChips());
        if (toCall <= 0) {
            return null;
        }

        playerHand.setRoundBetAmount(playerHand.getRoundBetAmount() + toCall);
        playerHand.setBetAmount(playerHand.getBetAmount() + toCall);
        player.setChips(player.getChips() - toCall);
        hand.setPot(hand.getPot() + toCall);

        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        handDao.save(hand);
        playerDao.save(player);
        return next;
    }

    @Override
    @Transactional
    public Player callCurrent(Player player, Game game, int roundBetAmount) {
        HandEntity hand = game.getCurrentHand();
        PlayerHand playerHand = player.getPlayerHand();

        if (roundBetAmount != playerHand.getBetAmount()) {
            return null;
        }
        int toCall = hand.getTotalBetAmount() - playerHand.getRoundBetAmount();
        toCall = Math.min(toCall, player.getChips());
        if (toCall <= 0) {
            if (playerIsNotCurrentToAct(player, hand, PlayerHandRoundAction.CHECK)) return null;
        }
        playerHand.setRoundBetAmount(playerHand.getRoundBetAmount() + toCall);
        playerHand.setBetAmount(playerHand.getBetAmount() + toCall);

        //call out of turn
        if (playerIsNotCurrentToAct(player, hand, PlayerHandRoundAction.CALL_CURRENT)) return null;

        player.setChips(player.getChips() - toCall);
        hand.setPot(hand.getPot() + toCall);

        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        handDao.save(hand);
        playerDao.save(player);
        return next;
    }
}
