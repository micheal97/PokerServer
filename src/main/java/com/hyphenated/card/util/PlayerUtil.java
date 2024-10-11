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
import com.hyphenated.card.eval.HandRankEvaluator;
import com.hyphenated.card.eval.TwoPlusTwoHandEvaluator;
import com.hyphenated.card.holder.Board;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    public static Player getNextPlayerToAct(HandEntity hand, @Nullable Player startPlayer) {
        SortedSet<Player> players = hand.getPlayers();
        Player next = startPlayer;

        while (true) {
            next = PlayerUtil.getNextPlayerInGameOrder(players, next);
            //Escape condition
            if (next.equals(startPlayer) || next.equals(hand.getLastBetOrRaise())) {
                return null;
            }
            if (next.getChips() > 0) {
                return next;
            }
        }
    }


    /**
     * Determine the winner(s) of a hand.  Limit the players considered to be the winner to the list
     * passed into the parameter.  This allows for a separation of concerns when dealing with split
     * pots and multiple side pots.
     *
     * @return List of {@link Player}s who have won the hand.  If there is a tie, all players that have
     * tied are returned in the list.
     */
    public static List<Player> getWinnersOfHand(Board board, List<Player> players) {
        return new TreeMap<>(players.stream()
                .collect(Collectors.toConcurrentMap((player -> evaluator.evaluate(board, ((Player) (player)).getPlayerHand().getHand())),
                        List::of, (s, a) -> {
                            List<Player> players1 = new ArrayList<>();
                            players1.addAll(s);
                            players1.addAll(a);
                            return List.copyOf(players1);
                        })))
                .lastEntry().getValue();
    }
}
