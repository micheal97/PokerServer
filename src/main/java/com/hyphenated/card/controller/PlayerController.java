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

import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.dto.GameDTO;
import com.hyphenated.card.dto.GameDTOs;
import com.hyphenated.card.dto.PlayerDTO;
import com.hyphenated.card.enums.PlayerHandRoundAction;
import com.hyphenated.card.service.GameService;
import com.hyphenated.card.service.PlayerServiceManager;
import com.hyphenated.card.service.ScheduledPlayerActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

import static com.hyphenated.card.Header.*;
import static com.hyphenated.card.UrlConstant.*;


/**
 * Controller class that will handle the front-end API interactions regarding
 * individual players involved with a game.
 *
 * @author jacobhyphenated
 * Copyright (c) 2013
 */
@RestController
public class PlayerController {

    @Autowired
    private PlayerServiceManager playerService;

    @Autowired
    private GameService gameService;

    @Autowired
    private TableTasksController tableTasksController;
    @Autowired
    private ScheduledPlayerActionService scheduledPlayerActionService;

    @GetMapping(GAMES)
    public ResponseEntity<GameDTOs> getGames() {
        return ResponseEntity.ok(new GameDTOs(
                gameService.findAll().stream().map(Game::getGameDTO).toList()));
    }

    @GetMapping(REGISTER)
    public ResponseEntity<Object> registerPlayer(@RequestHeader(NAME) String name, @RequestHeader(PASSWORD) String password) {
        Player player = new Player(name, password);
        return playerService.registerPlayer(player) ? ResponseEntity.ok().body(null) : ResponseEntity.badRequest().body(null);
    }

    @GetMapping(LOGIN)
    public ResponseEntity<PlayerDTO> login(@RequestHeader(NAME) String name, @RequestHeader(PASSWORD) String password) {
        Optional<Player> optionalPlayer = playerService.findPlayerByNameAndPassword(name, password);
        return optionalPlayer.map(player -> ResponseEntity.ok(player.getPlayerDTO()))
                .orElseGet(() -> ResponseEntity.badRequest().body(null));
    }


    /**
     * Have a new player join a game.
     */
    @GetMapping(JOIN)
    public ResponseEntity<GameDTO> joinGame(@RequestHeader(GAME_ID_STRING) String gameIdString, @RequestHeader(PLAYER_ID_STRING) String playerIdString, @RequestHeader(STARTING_TABLE_CHIPS) String startingTableChips1) {
        int startingTableChips = Integer.parseInt(startingTableChips1);
        UUID gameId = UUID.fromString(gameIdString);
        UUID playerId = UUID.fromString(playerIdString);
        Optional<Game> optionalGame = gameService.findGameById(gameId);
        Optional<Player> optionalPlayer = playerService.findPlayerById(playerId);
        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {
            Game game = optionalGame.get();
            Player player = optionalPlayer.get();
            gameService.addNewPlayerToGame(game, player, startingTableChips);
            player.clearStrikes();
            playerService.savePlayer(player);
            tableTasksController.playerJoined(player.getName(), gameId);
            if (game.getPlayers().size() == 2 && game.getPrivateGameCreator() == null) {
                gameService.startGame(game);
            }
            return ResponseEntity.ok(game.getGameDTO());
        }
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * Fold the hand.
     *
     * @return Map with a single field for success, if the player successfully folded or not.
     * If fold is not a legal action, or it is not this players turn to act, success will be false.
     * Example: {"success":true}
     */
    @GetMapping(FOLD)
    public ResponseEntity<Object> fold(@RequestHeader(GAME_ID_STRING) String gameIdString, @RequestHeader(PLAYER_ID_STRING) String playerIdString) {
        UUID gameId = UUID.fromString(gameIdString);
        UUID playerId = UUID.fromString(playerIdString);
        Optional<Game> optionalGame = gameService.findGameById(gameId);
        Optional<Player> optionalPlayer = playerService.findPlayerById(playerId);
        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {
            Game game = optionalGame.get();
            Player player = optionalPlayer.get();
            scheduledPlayerActionService.handlePlayerRoundAction(PlayerHandRoundAction.FOLD, player, 0, game);
            return ResponseEntity.ok().body(null);
        }
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * Call a bet.
     *
     * @return Map represented as a JSON String with two fields, success and chips.  If calling is
     * not a legal action, or it is not this player's turn, success will be false. The Chips field
     * represents the current amount of chips the player has left after taking this action.
     * Example: {"success":true,"chips":xxx}
     */
    @GetMapping(CALL_ANY)
    public ResponseEntity<Object> callAny(@RequestHeader(GAME_ID_STRING) String gameIdString, @RequestHeader(PLAYER_ID_STRING) String playerIdString) {
        UUID gameId = UUID.fromString(gameIdString);
        UUID playerId = UUID.fromString(playerIdString);
        Optional<Game> optionalGame = gameService.findGameById(gameId);
        Optional<Player> optionalPlayer = playerService.findPlayerById(playerId);
        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {
            scheduledPlayerActionService.handlePlayerRoundAction(PlayerHandRoundAction.CALL_ANY, optionalPlayer.get(), 0, optionalGame.get());
            return ResponseEntity.ok().body(null);
        }
        return ResponseEntity.badRequest().body(null);
    }

    @GetMapping(CALL_CURRENT)
    public ResponseEntity<Object> callCurrent(@RequestHeader(GAME_ID_STRING) String gameIdString, @RequestHeader(PLAYER_ID_STRING) String playerIdString, @RequestHeader(CALL_AMOUNT) String callAmount1) {
        int callAmount = Integer.parseInt(callAmount1);
        UUID gameId = UUID.fromString(gameIdString);
        UUID playerId = UUID.fromString(playerIdString);
        Optional<Game> optionalGame = gameService.findGameById(gameId);
        Optional<Player> optionalPlayer = playerService.findPlayerById(playerId);
        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {
            scheduledPlayerActionService.handlePlayerRoundAction(
                    PlayerHandRoundAction.CALL_CURRENT,
                    optionalPlayer.get(),
                    callAmount,
                    optionalGame.get());
            return ResponseEntity.ok().body(null);
        }
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * Check the action in this hand.
     *
     * @return Map represented as a JSON String with a single field: success.  If checking is not
     * a legal action, or it is not this player's turn, success will be false.
     * Example: {"success":false}
     */
    @GetMapping(CHECK)
    public ResponseEntity<Object> check(@RequestHeader(GAME_ID_STRING) String gameIdString, @RequestHeader(PLAYER_ID_STRING) String playerIdString) {
        UUID gameId = UUID.fromString(gameIdString);
        UUID playerId = UUID.fromString(playerIdString);
        Optional<Game> optionalGame = gameService.findGameById(gameId);
        Optional<Player> optionalPlayer = playerService.findPlayerById(playerId);
        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {
            scheduledPlayerActionService.handlePlayerRoundAction(
                    PlayerHandRoundAction.CHECK,
                    optionalPlayer.get(),
                    0,
                    optionalGame.get());
            return ResponseEntity.ok().body(null);
        }
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * Bet or Raise.
     *
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
    @GetMapping(BET)
    public ResponseEntity<Object> bet(@RequestHeader(GAME_ID_STRING) String gameIdString, @RequestHeader(PLAYER_ID_STRING) String playerIdString, @RequestHeader(BET_AMOUNT) int betAmount) {
        UUID gameId = UUID.fromString(gameIdString);
        UUID playerId = UUID.fromString(playerIdString);
        Optional<Game> optionalGame = gameService.findGameById(gameId);
        Optional<Player> optionalPlayer = playerService.findPlayerById(playerId);
        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {
            scheduledPlayerActionService.handlePlayerRoundAction(
                    PlayerHandRoundAction.BET,
                    optionalPlayer.get(),
                    betAmount,
                    optionalGame.get());
            return ResponseEntity.ok().body(null);
        }
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * Sit back in the game after having sat out
     *
     * @return {"success":true} when the player is sat back in the game
     */

    @GetMapping(LEAVE)
    public ResponseEntity<Object> leave(@RequestHeader(GAME_ID_STRING) String gameIdString, @RequestHeader(PLAYER_ID_STRING) String playerIdString) {
        UUID gameId = UUID.fromString(gameIdString);
        UUID playerId = UUID.fromString(playerIdString);
        Optional<Game> optionalGame = gameService.findGameById(gameId);
        Optional<Player> optionalPlayer = playerService.findPlayerById(playerId);
        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {
            Game game = optionalGame.get();
            Player player = optionalPlayer.get();
            scheduledPlayerActionService.handlePlayerRoundAction(PlayerHandRoundAction.FOLD, player, 0, game);
            gameService.removePlayerFromGame(game, player);
            tableTasksController.playerLeft(player.getName(), game.getId());
            return ResponseEntity.ok().body(null);
        }
        return ResponseEntity.badRequest().body(null);
    }

}
