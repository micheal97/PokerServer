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
package com.hyphenated.card.controller;

import com.hyphenated.card.controller.dto.GameDTO;
import com.hyphenated.card.controller.dto.PlayerDTO;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.domain.PlayerHand;
import com.hyphenated.card.domain.PlayerHandRoundAction;
import com.hyphenated.card.service.GameService;
import com.hyphenated.card.service.PlayerServiceManager;
import com.hyphenated.card.service.ScheduledPlayerActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.UUID;

/**
 * Controller class that will handle the front-end API interactions regarding
 * individual players involved with a game.
 *
 * @author jacobhyphenated
 * Copyright (c) 2013
 */
@Controller
public class PlayerController {

    @Autowired
    private PlayerServiceManager playerService;

    @Autowired
    private GameService GameService;

    @Autowired
    private TableTasksController tableTasksController;
    @Autowired
    private ScheduledPlayerActionService scheduledPlayerActionService;

    @RequestMapping("/games")
    public @ResponseBody ResponseEntity<List<GameDTO>> getGames() {
        return ResponseEntity.ok(GameService.findAll().stream().map(Game::getGameDTO).toList());
    }

    @RequestMapping("/register")
    public @ResponseBody ResponseEntity.BodyBuilder registerPlayer(@RequestParam String name, @RequestParam String password) {
        Player player = new Player();
        player.setChips(1000);
        player.setPassword(password);
        player.setName(name);
        return playerService.registerPlayer(player) ? ResponseEntity.ok() : ResponseEntity.badRequest();
    }

    @RequestMapping("/login")
    public @ResponseBody ResponseEntity<PlayerDTO> login(@RequestParam String name, @RequestParam String password) {
        return ResponseEntity.ok(playerService.findPlayerByNameAndPassword(name, password).getPlayerDTO());
    }


    /**
     * Have a new player join a game.
     */
    @RequestMapping("/join")
    public @ResponseBody ResponseEntity.BodyBuilder joinGame(@RequestParam UUID gameId, @RequestParam UUID playerId, @RequestParam int startingTableChips) {
        Game game = GameService.getGameById(gameId);
        Player player = playerService.findPlayerById(playerId);
        GameService.addNewPlayerToGame(game, player, startingTableChips);
        tableTasksController.playerJoined(player.getName(), gameId);
        if (game.getPlayers().size() == 2 && game.getPrivateGameCreator() == null) {
            GameService.startGame(game);
        }
        return ResponseEntity.ok();
    }

    /**
     * Fold the hand.
     *
     * @param gameId Unique ID of the game being played
     * @return Map with a single field for success, if the player successfully folded or not.
     * If fold is not a legal action, or it is not this players turn to act, success will be false.
     * Example: {"success":true}
     */
    @RequestMapping("/fold")
    public @ResponseBody ResponseEntity.BodyBuilder fold(@RequestParam UUID gameId, @RequestParam UUID playerId) {
        Game game = GameService.getGameById(gameId);
        playerService.findPlayerById(playerId);
        scheduledPlayerActionService.handlePlayerRoundAction(PlayerHandRoundAction.FOLD, playerId, 0);
        return ResponseEntity.ok();
        //TODO: Überall wie hier
    }

    /**
     * Call a bet.
     *
     * @param gameId Unique ID of the game being played
     * @return Map represented as a JSON String with two fields, success and chips.  If calling is
     * not a legal action, or it is not this player's turn, success will be false. The Chips field
     * represents the current amount of chips the player has left after taking this action.
     * Example: {"success":true,"chips":xxx}
     */
    @RequestMapping("/callany")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody ResponseEntity.BodyBuilder callAny(@RequestParam UUID gameId, @RequestParam UUID playerHandId) {
        Game game = GameService.getGameById(gameId);
        PlayerHand playerHand = game.getCurrentHand().findPlayerHandById(playerHandId).get();
        scheduledPlayerActionService.handlePlayerRoundAction(PlayerHandRoundAction.CALL_ANY, playerHand, 0);
        return ResponseEntity.ok();
    }

    @RequestMapping("/callCurrent")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody ResponseEntity.BodyBuilder callCurrent(@RequestParam UUID gameId, @RequestParam UUID playerHandId, @RequestParam int callAmount) {
        Game game = GameService.getGameById(gameId);
        PlayerHand playerHand = game.getCurrentHand().findPlayerHandById(playerHandId).get();
        scheduledPlayerActionService.handlePlayerRoundAction(PlayerHandRoundAction.CALL_CURRENT, playerHand, callAmount);
        return ResponseEntity.ok();
    }

    /**
     * Check the action in this hand.
     *
     * @param gameId Unique ID of the game being played
     * @return Map represented as a JSON String with a single field: success.  If checking is not
     * a legal action, or it is not this player's turn, success will be false.
     * Example: {"success":false}
     */
    @RequestMapping("/check")
    public @ResponseBody ResponseEntity.BodyBuilder check(@RequestParam UUID gameId, @RequestParam UUID playerHandId) {
        Game game = GameService.getGameById(gameId);
        PlayerHand playerHand = game.getCurrentHand().findPlayerHandById(playerHandId).get();
        scheduledPlayerActionService.handlePlayerRoundAction(PlayerHandRoundAction.CHECK, playerHand, 0);
        return ResponseEntity.ok();
    }

    /**
     * Bet or Raise.
     *
     * @param gameId    unique ID of the game being played
     * @param betAmount amount of the bet.  This is the total amount, so if there was a bet
     *                  before, and this is the raise, this value represents the amount bet for only this raise from the player.
     *                  To put in another way, this betAmount value is the amount is the amount bet <em>in addition to</em>
     *                  the amount it would take to call.
     *                  <br />For example: Player 1 bets 100.  Player 2 raises to 300.  Player 2 calls this method with the
     *                  betAmount parameter of 200.  Player 1 re-raises to 900 total (100 + 200 to call + 600 more).  The betAmount
     *                  parameter is passed as 600.
     * @return Map representing a JSON String with two values: success and chips.  If a bet is not a legal
     * action, or if it is not this player's turn, success will be false. The chips value represents the
     * amount of chips the player has after completing this action.
     * Example: {"success":true,"chips":xxx}
     */
    @RequestMapping("/bet")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody ResponseEntity.BodyBuilder bet(@RequestParam UUID gameId, @RequestParam UUID playerHandId, @RequestParam int betAmount) {
        Game game = GameService.getGameById(gameId);
        PlayerHand playerHand = game.getCurrentHand().findPlayerHandById(playerHandId).get();
        scheduledPlayerActionService.handlePlayerRoundAction(PlayerHandRoundAction.BET, playerHand, betAmount);
        return ResponseEntity.ok();
    }

    /**
     * Sit back in the game after having sat out
     *
     * @param playerId Player being sat back in
     * @return {"success":true} when the player is sat back in the game
     */

    @RequestMapping("/leave")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody ResponseEntity.BodyBuilder leave(@RequestParam UUID playerId) {
        Player player = playerService.findPlayerById(playerId);
        GameService.removePlayerFromGame(player);
        Game game = player.getGame();
        game.getCurrentHand()
                .findPlayerHandByPlayerId(playerId)
                .ifPresent(playerHand ->
                        scheduledPlayerActionService.handlePlayerRoundAction(
                                PlayerHandRoundAction.FOLD, playerHand, 0));
        tableTasksController.playerLeft(player.getName(), game.getId());
        return ResponseEntity.ok();
    }

}
