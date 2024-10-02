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

import java.util.Map;
import java.util.UUID;

@Service
public class PlayerActionServiceImpl implements PlayerActionService {

    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private HandDao handDao;
    @Autowired
    private PokerHandService pokerHandService;
    @Autowired
    private TableStructureService tableStructureService;

    @Override
    @Transactional
    public Player getPlayerById(UUID playerId) {
        return playerDao.findById(playerId);
    }


    @Override
    @Transactional
    public PlayerHand fold(PlayerHand playerHand) {
        HandEntity hand = playerHand.getHandEntity();
        Player player = playerHand.getPlayer();
        //fold out of turn
        if (!player.equals(hand.getCurrentToAct())) {
            hand.getPlayers().remove(playerHand);
            playerHand.setRoundAction(PlayerHandRoundAction.FOLD);
            hand.getPlayers().add(playerHand);
            handDao.save(hand);
            return null;
        }

        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        if (!PlayerUtil.removePlayerFromHand(player, hand)) {
            return null;
        }
        hand.setCurrentToAct(next);
        handDao.save(hand);
        if (next == null) {
            next = pokerHandService.handleNextGameStatus(hand.getTableStructure()).getCurrentToAct();
        }
        return hand.findPlayerHandByPlayerId(next.getId()).get();
        //TODO: Überall wie hier
    }

    @Override
    @Transactional
    public boolean check(PlayerHand playerHand) {
        HandEntity hand = playerHand.getHandEntity();
        Player player = playerHand.getPlayer();
        //Checking is not currently an option
        if (!getPlayerStatus(player).equals(PlayerStatus.ACTION_TO_CHECK)) {
            return false;
        }
        //check out of turn
        if (!player.equals(hand.getCurrentToAct())) {
            hand.getPlayers().remove(playerHand);
            playerHand.setRoundAction(PlayerHandRoundAction.CHECK);
            hand.getPlayers().add(playerHand);
            handDao.save(hand);
            return false;
        }
        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        handDao.save(hand);
        return true;
    }

    @Override
    @Transactional
    public boolean bet(PlayerHand playerHand, int betAmount) {
        HandEntity hand = playerHand.getHandEntity();
        Player player = playerHand.getPlayer();

        //Bet must meet the minimum of twice the previous bet.  Call bet amount and raise exactly that amount or more
        //Alternatively, if there is no previous bet, the first bet must be at least the big blind
        if (betAmount < hand.getLastBetAmount() || betAmount < hand.getBlindLevel().getBigBlind()) {
            return false;
        }

        int toCall = hand.getTotalBetAmount() - playerHand.getRoundBetAmount();
        int total = betAmount + toCall;
        if (total > player.getChips()) {
            total = player.getChips();
            betAmount = total - toCall;
        }
        //bet out of turn
        if (!player.equals(hand.getCurrentToAct())) {
            hand.getPlayers().remove(playerHand);
            playerHand.setRoundAction(PlayerHandRoundAction.BET);
            playerHand.setBetAmount(betAmount);
            hand.getPlayers().add(playerHand);
            handDao.save(hand);
            return false;
        }
        playerHand.setRoundBetAmount(playerHand.getRoundBetAmount() + total);
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
        return true;
    }

    @Override
    @Transactional
    public boolean callAny(PlayerHand playerHand) {
        HandEntity hand = playerHand.getHandEntity();
        Player player = playerHand.getPlayer();
        //call out of turn
        if (!player.equals(hand.getCurrentToAct())) {
            hand.getPlayers().remove(playerHand);
            playerHand.setRoundAction(PlayerHandRoundAction.CALL_ANY);
            hand.getPlayers().add(playerHand);
            handDao.save(hand);
            return false;
        }

        int toCall = hand.getTotalBetAmount() - playerHand.getRoundBetAmount();
        toCall = Math.min(toCall, player.getChips());
        if (toCall <= 0) {
            return false;
        }

        playerHand.setRoundBetAmount(playerHand.getRoundBetAmount() + toCall);
        playerHand.setBetAmount(playerHand.getBetAmount() + toCall);
        player.setChips(player.getChips() - toCall);
        hand.setPot(hand.getPot() + toCall);

        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        handDao.save(hand);
        playerDao.save(player);
        return true;
    }

    @Override
    @Transactional
    public boolean callCurrent(PlayerHand playerHand, int roundBetAmount) {
        HandEntity hand = playerHand.getHandEntity();
        Player player = playerHand.getPlayer();

        if (roundBetAmount != playerHand.getBetAmount()) {
            return false;
        }
        int toCall = hand.getTotalBetAmount() - playerHand.getRoundBetAmount();
        toCall = Math.min(toCall, player.getChips());
        if (toCall <= 0) {
            return false;
        }
        playerHand.setRoundBetAmount(playerHand.getRoundBetAmount() + toCall);
        playerHand.setBetAmount(playerHand.getBetAmount() + toCall);

        //call out of turn
        if (!player.equals(hand.getCurrentToAct())) {
            hand.getPlayers().remove(playerHand);
            playerHand.setRoundAction(PlayerHandRoundAction.CALL_CURRENT);
            hand.getPlayers().add(playerHand);
            handDao.save(hand);
            return null;
        }
        player.setChips(player.getChips() - toCall);
        hand.setPot(hand.getPot() + toCall);

        Player next = PlayerUtil.getNextPlayerToAct(hand, player);
        hand.setCurrentToAct(next);
        handDao.save(hand);
        playerDao.save(player);
        return true;
    }

    @Override
    @Transactional
    public void sitIn(Player player, TableStructure tableStructure, int startingChips) {
        tableStructureService.addNewPlayerToTableStructure(tableStructure, player, startingChips);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerStatus getPlayerStatus(Player player) {
        player = playerDao.save(player);
        if (player == null) {
            return PlayerStatus.ELIMINATED;
        }

        if (player.isSittingOut()) {
            return PlayerStatus.SIT_OUT_GAME;
        }

        TableStructure tableStructure = player.getTableStructure();
        if (!tableStructure.getGameStatus().equals(GameStatus.NOT_STARTED)) {
            return PlayerStatus.NOT_STARTED;
        }

        HandEntity hand = tableStructure.getCurrentHand();
        if (hand == null) {
            return PlayerStatus.SEATING;
        }
        PlayerHand playerHand = null;
        for (PlayerHand ph : hand.getPlayers()) {
            if (ph.getPlayer().equals(player)) {
                playerHand = ph;
                break;
            }
        }

        if (!hand.getPlayers().contains(playerHand)) {
            if (player.getChips() <= 0) {
                return PlayerStatus.ELIMINATED;
            }
            return PlayerStatus.SIT_OUT;
        }

        if (hand.getCurrentToAct() == null) {
            //Only one player, everyone else folded, player is the winner
            if (hand.getPlayers().size() == 1) {
                return PlayerStatus.WON_HAND;
            }
            //Get the list of players who won at least some amount of chips at showdown
            Map<Player, Integer> winners = PlayerUtil.getAmountWonInHandForAllPlayers(hand);
            if (winners != null && winners.containsKey(player)) {
                //Player is contained in this collection, so the player was a winner in the hand
                return PlayerStatus.WON_HAND;
            } else {
                //Hand is over but player lost at showdown.
                return PlayerStatus.LOST_HAND;
            }
        }

        if (player.getChips() <= 0) {
            return PlayerStatus.ALL_IN;
        }

        if (!player.equals(hand.getCurrentToAct())) {
            //Small and Big Blind to be determined later?
            //Let controller handle that status
            return PlayerStatus.WAITING;
        }

        if (hand.getTotalBetAmount() > playerHand.getRoundBetAmount()) {
            return PlayerStatus.ACTION_TO_CALL;
        } else if (playerHand.getRoundBetAmount() > 0) {
            //We have placed a bet but now our action is check?  This means the round of betting is over
            //TODO still problem when every player checks or BB.  Need additional info to solve this
            return PlayerStatus.ACTION_TO_CHECK;
        } else {
            return PlayerStatus.ACTION_TO_CHECK;
        }

    }
}
