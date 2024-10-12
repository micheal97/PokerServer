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
    private GameDao gameDao;


    private boolean actionNotCurrentToAct(Player player, Game game, PlayerHandRoundAction action, int betAmount) {
        player.getPlayerHand().setRoundBetAmount(betAmount);
        return actionNotCurrentToAct(player, game, action);
    }

    @Transactional
    private boolean actionNotCurrentToAct(Player player, Game game, PlayerHandRoundAction action) {
        HandEntity hand = game.getHand();
        if (player.equals(hand.getCurrentToAct())) {
            return false;
        }
        hand.removePlayer(player);
        player.getPlayerHand().setRoundAction(action);
        hand.addPlayer(player);
        game.setHand(hand);
        gameDao.save(game);
        playerDao.save(player);
        return true;
    }

    @Override
    @Transactional
    public Player fold(Player player, Game game) {
        HandEntity hand = game.getHand();
        //fold out of turn
        if (actionNotCurrentToAct(player, game, PlayerHandRoundAction.FOLD)) return null;
        hand.removePlayer(player);
        return afterActionSave(player, game, hand);
    }


    @Override
    public Player check(Player player, Game game) {
        HandEntity hand = game.getHand();
        //check out of turn
        if (actionNotCurrentToAct(player, game, PlayerHandRoundAction.CHECK)) return null;
        return afterActionSave(player, game, hand);
    }

    @Override
    public Player bet(Player player, Game game, int betAmount) {
        PlayerHand playerHand = player.getPlayerHand();
        HandEntity hand = game.getHand();
        //Bet must meet the minimum of twice the previous bet.  Call bet amount and raise exactly that amount or more
        //Alternatively, if there is no previous bet, the first bet must be at least the big blind
        if (betAmount < hand.getLastBetAmount() || betAmount < game.getBlindLevel().getBigBlind()) {
            throw new IllegalArgumentException("Bet too less");
        }

        betAmount = Math.min(hand.getBetAmount() - playerHand.getRoundBetAmount(),
                player.getChips());
        int playerHandRoundBetAmount = playerHand.getRoundBetAmount() + betAmount;
        //bet out of turn
        if (actionNotCurrentToAct(player, game, PlayerHandRoundAction.BET, playerHandRoundBetAmount)) return null;

        playerHand.setRoundBetAmount(playerHandRoundBetAmount);
        playerHand.setBetAmount(playerHand.getBetAmount() + betAmount);
        player.removeTableChips(betAmount);
        hand.setPot(hand.getPot() + betAmount);
        hand.setLastBetAmount(betAmount);
        hand.setBetAmount(hand.getBetAmount() + betAmount);
        hand.setLastBetOrRaise(player);
        return afterActionSaveAll(player, game, hand);
    }

    @Override
    public Player callAny(Player player, Game game) {
        HandEntity hand = game.getHand();
        PlayerHand playerHand = player.getPlayerHand();
        //call out of turn
        if (actionNotCurrentToAct(player, game, PlayerHandRoundAction.CALL_ANY)) return null;


        int toCall = Math.min(hand.getLastBetAmount() - playerHand.getRoundBetAmount(),
                player.getChips());
        if (toCall <= 0) {
            return null;
        }

        playerHand.setRoundBetAmount(playerHand.getRoundBetAmount() + toCall);
        playerHand.setBetAmount(playerHand.getBetAmount() + toCall);
        player.removeTableChips(toCall);
        hand.setPot(hand.getPot() + toCall);

        return afterActionSaveAll(player, game, hand);
    }

    @Override
    public Player callCurrent(Player player, Game game, int roundBetAmount) {
        HandEntity hand = game.getHand();
        PlayerHand playerHand = player.getPlayerHand();

        if (roundBetAmount != playerHand.getBetAmount()) {
            return null;
        }
        int toCall = Math.min(hand.getBetAmount() - playerHand.getRoundBetAmount(), player.getChips());
        if (toCall == 0) {
            if (actionNotCurrentToAct(player, game, PlayerHandRoundAction.CHECK)) return null;
        }
        if (toCall < 0) {
            return null;
        }
        playerHand.setRoundBetAmount(playerHand.getRoundBetAmount() + toCall);
        playerHand.setBetAmount(playerHand.getBetAmount() + toCall);

        //call out of turn
        if (actionNotCurrentToAct(player, game, PlayerHandRoundAction.CALL_CURRENT)) return null;

        player.removeTableChips(toCall);
        hand.setPot(hand.getPot() + toCall);

        return afterActionSaveAll(player, game, hand);
    }


    @Transactional
    private Player afterActionSave(Player player, Game game, HandEntity hand) {
        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        game.setHand(hand);
        gameDao.save(game);
        playerDao.save(player);
        return next;
    }

    @Transactional
    private Player afterActionSaveAll(Player player, Game game, HandEntity hand) {
        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        game.setHand(hand);
        gameDao.save(game);
        playerDao.save(player);
        return next;
    }
}
