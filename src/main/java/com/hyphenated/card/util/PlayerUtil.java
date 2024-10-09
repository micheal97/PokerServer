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
package com.hyphenated.card.util;

import com.hyphenated.card.domain.HandEntity;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.domain.PlayerHand;
import com.hyphenated.card.eval.HandRank;
import com.hyphenated.card.eval.HandRankEvaluator;
import com.hyphenated.card.eval.TwoPlusTwoHandEvaluator;
import com.hyphenated.card.holder.Board;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class with helper methods for Player interactions
 *
 * @author jacobhyphenated
 */
public class PlayerUtil {

    private static final HandRankEvaluator evaluator = TwoPlusTwoHandEvaluator.getInstance();

    /**
     * Get the next player to act
     *
     * @param players     List of players involved
     * @param startPlayer Start position.  The player to the left of this player should be the next to act
     * @return {@link Player} who is next to act
     */
    private static Player getNextPlayerInGameOrder(SortedSet<Player> players, @Nullable Player startPlayer) {
        //Sorted list by game order
        if (startPlayer == null) {
            return players.first();
        }
        AtomicInteger nextGamePosition = new AtomicInteger(startPlayer.getGamePosition() + 1);
        if (players.size() <= nextGamePosition.get()) {
            nextGamePosition.set(0);
        }
        return players.stream().filter(player -> player.getGamePosition() == nextGamePosition.get()).findAny().get();
    }

    /**
     * Get the next player in the hand to act.  This will skip over all-in players and sitting out players
     *
     * @param hand        Current Hand. This object may be modified by this method.
     * @param startPlayer Use player as the starting point for the search. Find next player in game over
     *                    compared to this player.
     * @return Player who will be next to act
     */
    @Nullable
    public static Player getNextPlayerToAct(HandEntity hand, Player startPlayer) {
        SortedSet<Player> players = hand.getPlayers();
        Player next = startPlayer;

        while (true) {
            next = PlayerUtil.getNextPlayerInGameOrder(players, next);
            //If the player is not sitting out and still has chips, then this player is next to act
            if (next.getChips() > 0) {
                continue;
            }
            //Escape condition
            if (next.equals(startPlayer)) {
                return null;
            }
            if (next.equals(hand.getLastBetOrRaise()) || hand.getBetAmount() == 0) {
                return next;
            }

        }
    }


    /**
     * Determine the winner(s) of a hand.  Limit the players considered to be the winner to the list
     * passed into the parameter.  This allows for a separation of concerns when dealing with split
     * pots and multiple side pots.
     *
     * @param hand {@link HandEntity} that has been concluded with a winner to be determined
     * @return List of {@link Player}s who have won the hand.  If there is a tie, all players that have
     * tied are returned in the list.
     */
    public static List<Player> getWinnersOfHand(HandEntity hand) {
        List<Player> winners = List.of();
        Board board = new Board(hand.getBoard().getFlop1(), hand.getBoard().getFlop2(),
                hand.getBoard().getFlop3(), hand.getBoard().getTurn(), hand.getBoard().getRiver());
        HandRank highestRank = null;
        SortedSet<Player> players = hand.getPlayers();
        for (Player player : players) {
            HandRank rank = evaluator.evaluate(board, player.getPlayerHand().getHand());
            //First player to be checked
            if (highestRank == null) {
                highestRank = rank;
                winners = List.of(player);
                continue;
            }

            //Compare This player to current best player
            int comp = rank.compareTo(highestRank);
            if (comp > 0) {
                //New best
                highestRank = rank;
                winners = List.of(player);
            } else if (comp == 0) {
                //Tie
                ArrayList<Player> winners1 = new ArrayList<>(winners);
                winners1.add(player);
                winners = winners1.stream().toList();
            }
        }

        return winners;
    }

    /**
     * For a showdown, there are multiple possible outcomes.  There is one winner; there are multiple winners;
     * there are multiple winners with different amounts won because of side pots.
     * <br /><br />
     * This method will take a {@link HandEntity} and determine what players win what amount.  This will
     * take into account split pots and side pots.
     *
     * @param hand Hand that has gotten to the river and is at showdown
     * @return A {@link Map} of {@link Player} objects to the Integer value of chips that they are awarded as a result
     * of the hand.  The map will contain only winning players (of some kind) during this hand.  Null is returned
     * if the {@link HandEntity} is in the incorrect state (not at the river) or the proper showdown conditions have
     * not been met.
     */
    public static Map<Player, Integer> getAmountWonInHandForAllPlayers(HandEntity hand) {
        if (hand.getBoard().getRiver() == null) {
            return null;
        }

        Map<Player, Integer> winnersMap = new HashMap<>();

        //Make deep copy of players.  We will manipulate this set in the following methods
        Set<Player> currentPlayers = new HashSet<>(hand.getPlayers());

        resolveSidePot(winnersMap, hand, 0, currentPlayers);
        resolveDeadMoney(winnersMap, hand);

        return winnersMap;
    }

    private static void applyWinningAndChips(Map<Player, Integer> winnersMap, HandEntity hand,
                                             int minPlayerBetAmount) {
        int numOfPlayersInvolved = hand.getPlayers().size();
        List<Player> winners = PlayerUtil.getWinnersOfHand(hand);
        int potSplit = (minPlayerBetAmount * numOfPlayersInvolved) / winners.size();
        //Odd chips go to first player in game order
        int remaining = (minPlayerBetAmount * numOfPlayersInvolved) % winners.size();
        for (Player player : winners) {
            Integer bigIValue = winnersMap.get(player);
            int i = (bigIValue == null) ? 0 : bigIValue;
            winnersMap.put(player, potSplit + remaining + i);
            remaining = 0;
        }
    }

    /*
     * Recursive helper method.  Goes through each potential pot in order, starting from the
     * main pot and up through each possible side pot.
     *
     * winnersMap is passed by value and updated by each recursive pass through.
     * allInBetRunningTotal keeps track of the bet amount (per player) that of each pot additively
     * playersInvolved is used to determine exit conditions; tracks players involved in each pot/side pot
     */
    private static void resolveSidePot(Map<Player, Integer> winnersMap, HandEntity hand,
                                       int allInBetRunningTotal, Set<Player> playersInvolved) {
        //Determine all in player if applicable
        Player allInPlayer = null;
        Player potentialAllInPlayer = null;
        int minimumBetAmountPerPlayer = 0;
        for (Player player : playersInvolved) {
            PlayerHand ph = player.getPlayerHand();
            if (minimumBetAmountPerPlayer == 0) {
                minimumBetAmountPerPlayer = ph.getBetAmount();
                potentialAllInPlayer = player;
            } else if (minimumBetAmountPerPlayer > ph.getBetAmount()) {
                minimumBetAmountPerPlayer = ph.getBetAmount();
                allInPlayer = player;
                potentialAllInPlayer = allInPlayer;
            } else if (minimumBetAmountPerPlayer < ph.getBetAmount()) {
                allInPlayer = potentialAllInPlayer;
            }
        }
        //minimum bet per player is just for this pot (of many possible side pots).
        //discount any chips or bets that may have been awarded in previous pots
        minimumBetAmountPerPlayer = minimumBetAmountPerPlayer - allInBetRunningTotal;
        allInBetRunningTotal += minimumBetAmountPerPlayer;

        //exit condition.  No player is all in, or we are down to only 2 remaining players to evaluate
        if (allInPlayer == null || playersInvolved.size() == 2) {
            applyWinningAndChips(winnersMap, hand, minimumBetAmountPerPlayer);
            return;
        }

        //Handle this side pot. Remove the all in player, then re-evaluate for the remaining players.
        applyWinningAndChips(winnersMap, hand, minimumBetAmountPerPlayer);
        playersInvolved.remove(allInPlayer);
        resolveSidePot(winnersMap, hand, allInBetRunningTotal, playersInvolved);
    }

    private static void resolveDeadMoney(Map<Player, Integer> winnersMap, HandEntity hand) {
        int payout = 0;
        for (Player player : winnersMap.keySet()) {
            payout += winnersMap.get(player);
        }
        int deadMoney = hand.getPot() - payout;
        for (Player player : winnersMap.keySet()) {
            winnersMap.put(player, winnersMap.get(player) + deadMoney / winnersMap.size());
        }
        int remainder = deadMoney % winnersMap.size();
        Player oddChipWinner = winnersMap.keySet().iterator().next();
        winnersMap.put(oddChipWinner, winnersMap.get(oddChipWinner) + remainder);
    }
}
