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
import com.hyphenated.card.enums.BlindLevel;
import com.hyphenated.card.enums.GameStatus;
import com.hyphenated.card.enums.Payment;
import com.hyphenated.card.service.GameService;
import com.hyphenated.card.service.PlayerServiceManager;
import com.hyphenated.card.service.PokerHandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.hyphenated.card.Header.*;
import static com.hyphenated.card.UrlConstant.CREATE;
import static com.hyphenated.card.UrlConstant.START_PRIVATE_GAME;

/**
 * Controller class that will handle the API interactions with the front-end for the GameController.
 * The game controller is the device that handles the community cards, setting up the game, dealing, etc.
 * This will not be the controller for specific player actions, but for actions that effect the
 * game at a higher level.
 *
 * @author jacobhyphenated
 */
@RestController
public class TableController {

    @Autowired
    private GameService gameService;
    @Autowired
    private PokerHandService handService;
    @Autowired
    private PlayerServiceManager playerServiceManager;


    /**
     * Create a new game based on the parameters from the URL Request
     * <br /><br />
     * The standard URL Request to the path /create with two parameters, like:
     * pokerserverurl.com/create?gameName=MyPokerGame&gameStructure=TWO_HR_SEVENPPL
     * <br /><br />
     * Use the Spring to leverage the Enum type conversions. Return JSON response
     * with one value, gameId.
     *
     * @param gameName   Name to identify this game
     * @param blindLevel Type of the game that will be played
     * @return {"gameId":xxxx}.  The Java Method returns the Map<String,Long> which is converted
     * by Spring to the JSON object.
     */
    @GetMapping(CREATE)
    public ResponseEntity<String> createGame(@RequestParam(GAME_NAME) String gameName,
                                             @RequestParam(PASSWORD) String password,
                                             @RequestParam(MAX_PLAYERS) int maxPlayers,
                                             @RequestParam(BLIND_LEVEL) BlindLevel blindLevel,
                                             @RequestParam(PLAYER_ID_STRING) String playerId) {
        //TODO:evaluate if player is allowed and setTableCoins not linked to playerAccount

        Optional<Player> optionalPlayer = playerServiceManager.findPlayerById(playerId);
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            if (player.getPayments().getPayments().contains(Payment.GAME_CREATOR)) {
                Game game = new Game(blindLevel, maxPlayers, gameName, true, password);
                game = gameService.saveGame(game);
                player.setPrivateGameCreator(true);
                return ResponseEntity.ok(game.getId());
            }
        }
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * Start the game.  This should be called when the players have joined and everyone is ready to begin.
     *
     * @return Map representing a JSON string with a single field for "success" which is either true or false.
     * example: {"success":true}
     */
    @GetMapping(START_PRIVATE_GAME)
    public @ResponseBody ResponseEntity<Object> startGame(@RequestParam(GAME_ID_STRING) String gameId, @RequestParam(PLAYER_ID_STRING) String playerId) {

        Optional<Game> optionalGame = gameService.findGameById(gameId);
        Optional<Player> optionalPlayer = playerServiceManager.findPlayerById(playerId);
        if (optionalGame.isPresent() && optionalPlayer.isPresent()) {
            Game game = optionalGame.get();
            Player player = optionalPlayer.get();
            if (game.getGameStatus().equals(GameStatus.NOT_STARTED) && player.equals(game.getPrivateGameCreator())) {
                gameService.startGame(game);
                handService.startNewHand(game);
                return ResponseEntity.ok().body(null);
            }
        }
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * Sometimes it is nice to know that everything is working
     *
     * @return {"success":true}
     */
    @GetMapping("/ping")
    public @ResponseBody Map<String, Boolean> pingServer() {
        return Collections.singletonMap("success", true);
    }
}
