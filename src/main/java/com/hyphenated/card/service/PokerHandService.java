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

import com.hyphenated.card.domain.BoardEntity;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.HandEntity;
import com.hyphenated.card.domain.Player;

/**
 * Service to handle operations related to a poker hand.  Tracks the board state and players.
 *
 * @author jacobhyphenated
 */
public interface PokerHandService {

    void handleNextGameStatus(Game game);

    /**
     * Start a new hand on the given game
     *
     * @param game {@link Game} to start the hand with
     * @return {@link HandEntity} for the hand
     */
    HandEntity startNewHand(Game game);

    /**
     * End the hand. Update dependencies on Players, position, dealer, big blind, etc.
     * <br /><br />
     * Be aware, once a hand has ended, it will no longer be tied directly to the game
     * and cannot be retrieved through game.getCurrentHand()
     */
    boolean endHand(Game game);

    /**
     * Get the hand from the persistence context based on the unique id
     *
     * @param id unique id for the hand
     * @return {@link HandEntity}
     */
    HandEntity getHandById(long id);

    /**
     * Save the hand to the persistence context
     *
     * @param hand Detached {@link HandEntity}
     * @return HandEntity with attached persistence context
     */
    HandEntity saveHand(HandEntity hand);

    /**
     * Handle the flop for the hand
     *
     * @param hand {@link HandEntity} that owns the board the flop will be on
     * @return updated hand with {@link BoardEntity} containing the flop
     * @throws IllegalStateException if the hand is not in a state to expect the flop.
     */
    HandEntity flop(HandEntity hand) throws IllegalStateException;

    /**
     * Handle the turn for the hand
     *
     * @param hand {@link HandEntity} that owns the board the turn will be on
     * @return updated hand with {@link BoardEntity} containing the turn
     * @throws IllegalStateException if the hand is not in a state to expect a turn card.
     *                               This may be because there is already a turn card, or there is no flop yet.
     */
    HandEntity turn(HandEntity hand) throws IllegalStateException;

    /**
     * Handle the river card for the hand
     *
     * @param hand {@link HandEntity} that owns the board the river will be on
     * @return updated hand with {@link BoardEntity} containing the river card
     * @throws IllegalStateException if the hand is not in a state to expect a river card.
     */
    HandEntity river(HandEntity hand) throws IllegalStateException;

    /**
     * Gets the player who is the Small Blind for this hand.
     *
     * @param hand {@link HandEntity}
     * @return {@link Player} who is the Small Blind
     */
    Player getPlayerInSB(HandEntity hand);

    /**
     * Gets the player who is the Big Blind for this hand
     *
     * @param hand {@link HandEntity}
     * @return {@link Player} who is the Big Blind
     */
    Player getPlayerInBB(HandEntity hand);

}
