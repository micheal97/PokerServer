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

import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.domain.PlayerStatus;

/**
 * Service class to handle actions the player involved in the hand can take.
 *
 * @author jacobhyphenated
 */
public interface PlayerActionService {


    /**
     * The player folds out of the hand
     *
     * @return true if the player folds, false if the fold is not permitted
     * (the player is out of position, the player is not in the hand).
     */
    Player fold(Player player);

    /**
     * The Player checks back their action
     *
     * @return true if the player checks. False if a check is not permitted
     * (The player is out of turn, there is a bet and a check is not allowed).
     */
    Player check(Player player);

    /**
     * The Player places a bet
     *
     * @param betAmount amount of the bet.  This is the total amount, so if there was a bet
     *                  before, and this is the raise, this value represents the amount bet for only this raise from the player.
     *                  <br />For example: Player 1 bets 100.  Player 2 raises to 300.  Player 2 calls this method with the
     *                  betAmount parameter of 200.  Player 1 re-raises to 900 total (100 + 300 + 500 more).  The betAmount
     *                  parameter is passed as 500.
     * @return true if the bet is placed.  False if the bet is not permitted
     * (The bet is too small, the player is out of turn).
     */
    Player bet(Player player, int betAmount);

    /**
     * Call the bet
     *
     * @return true if the bet is successfully called. False if a call is not permitted
     * (there is not bet, it is not the players turn, etc.)
     */
    Player callAny(Player player);

    Player callCurrent(Player player, int roundBetAmount);

    /**
     * Get the status in the game or hand of the player
     *
     * @param player {@link Player} to check the status of
     * @return the {@link PlayerStatus}
     */
    PlayerStatus getPlayerStatus(Player player);

    /**
     * If a player is sitting out of the game, sit the player back in.
     *
     * @param player {@link Player} who is back in the game
     */
    void sitIn(Player player, Game game, int startingChips);
}
