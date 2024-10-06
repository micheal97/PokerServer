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

import com.hyphenated.card.domain.*;
import com.hyphenated.card.service.GameService;
import com.hyphenated.card.service.PlayerServiceManager;
import com.hyphenated.card.service.PokerHandService;
import com.hyphenated.card.service.ScheduledPlayerActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Controller class that will handle the API interactions with the front-end for the GameController.
 * The game controller is the device that handles the community cards, setting up the game, dealing, etc.
 * This will not be the controller for specific player actions, but for actions that effect the
 * game at a higher level.
 *
 * @author jacobhyphenated
 */
@Controller
public class TableController {
    //TODO:DeleteClass

    @Autowired
    private GameService GameService;
    @Autowired
    private ScheduledPlayerActionService scheduledPlayerActionService;
    @Autowired
    private PokerHandService handService;
    @Autowired
    private TableTasksController tableTasksController;
    @Autowired
    private PlayerServiceManager playerServiceManager;

    /**
     * Get a list of currently available game structures
     * <br /><br />
     * The standard URL Request to the path /structures with no parameters.
     *
     * @return The response is a JSON array of {@link CommonTournamentFormats} objects in JSON Object form.
     * Each object will contain a "name" that is the unique identifier for that format type.
     */
    @RequestMapping("/structures")
    public @ResponseBody List<CommonTournamentFormats> getGameStructures() {
        return Arrays.asList(CommonTournamentFormats.values());
    }

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
    @RequestMapping(value = "/create")
    public @ResponseBody ResponseEntity<UUID> createGame(@RequestParam String gameName,
                                                         @RequestParam int maxPlayers,
                                                         @RequestParam BlindLevel blindLevel,
                                                         @RequestParam UUID playerId) {
        Optional<Player> player = playerServiceManager.findPlayerById(playerId);
        if (player.isPresent()) {
            Game game = new Game();
            game.setName(gameName);
            game.setMaxPlayers(maxPlayers);
            game.setBlindLevel(blindLevel);
            game.setPrivateGameCreator(player.get());
            game = GameService.saveGame(game);
            return ResponseEntity.ok(game.getId());
        }
        return ResponseEntity.badRequest().body(null);
    }

    /**
     * Start the game.  This should be called when the players have joined and everyone is ready to begin.
     *
     * @param gameId unique ID for the game that is to be started
     * @return Map representing a JSON string with a single field for "success" which is either true or false.
     * example: {"success":true}
     */
    @RequestMapping("/startprivategame")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody ResponseEntity.BodyBuilder startGame(@RequestParam UUID gameId, @RequestParam UUID playerId) {
        //TODO:startPrivateGame / startPublicGame
        Optional<Game> optionalGame = GameService.findGameById(gameId);
        if (optionalGame.isPresent()) {
            Game game = optionalGame.get();
            if (game.getGameStatus().equals(GameStatus.NOT_STARTED) && game.getPrivateGameCreator().getId().equals(playerId)) {
                GameService.startGame(game);
                handService.startNewHand(game);
                return ResponseEntity.ok();
            }
        }
        return ResponseEntity.badRequest();
    }

    /**
     * Deal the flop for the hand. This should be called when preflop actions are complete
     * and the players are ready to deal the flop cards
     *
     * @param handId unique ID for the hand where the flop is being dealt
     * @return A map represented as a JSON String for the three cards dealt on the flop.
     * The cards are denoted by the rank and the suit, with <em>2-9,T,J,Q,K,A</em> as the rank and
     * <em>c,s,d,h</em> as the suit.  For example: Ace of clubs is <em>Ac</em> and Nine of Diamonds
     * is <em>9d</em>
     * <br /><br />
     * The json field values are card1, card2, card3.  Example: {"card1":"Xx","card2":"Xx","card3":"Xx"}
     */
    @RequestMapping("/flop")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody Map<String, String> flop(@RequestParam long handId) {
        HandEntity hand = handService.getHandById(handId);
        hand = handService.flop(hand);
        Map<String, String> result = new HashMap<>();
        result.put("card1", hand.getBoard().getFlop1().toString());
        result.put("card2", hand.getBoard().getFlop2().toString());
        result.put("card3", hand.getBoard().getFlop3().toString());
        return result;
    }

    /**
     * Deal the turn  for the hand. This should be called when the flop actions are complete
     * and the players are ready for the turn card to be dealt.
     *
     * @param handId unique ID for the hand to receive the turn card.
     * @return Map represented as a JSON String for the turn card, labeled as card4.
     * Example: {"card4":"Xx"}
     */
    @RequestMapping("/turn")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody Map<String, String> turn(@RequestParam long handId) {
        HandEntity hand = handService.getHandById(handId);
        hand = handService.turn(hand);
        return Collections.singletonMap("card4", hand.getBoard().getTurn().toString());
    }

    /**
     * Deal the river card for the hand. This should be called when the turn action is complete
     * and the players are ready for the river card to be dealt.
     *
     * @param handId Unique ID for the hand to receive the river card
     * @return Map represented as a JSON String for the river card, labeled as card5.
     * Example: {"card5":"Xx"}
     */
    @RequestMapping("/river")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody Map<String, String> river(@RequestParam long handId) {
        HandEntity hand = handService.getHandById(handId);
        hand = handService.river(hand);
        return Collections.singletonMap("card5", hand.getBoard().getRiver().toString());
    }

    /**
     * End the hand. This completes all actions that can be done on the hand.  The winners
     * are determined and the chips given to the appropriate players.  This will detach
     * the hand from the game, and no more actions may be taken on this hand.
     *
     * @param handId Unique ID for the hand to be ended
     * @return Map represented as a JSON String determining if the action was successful.
     * Example: {"success":true}
     */
    @RequestMapping("/endhand")
    @CacheEvict(value = "game", allEntries = true)
    public @ResponseBody Map<String, Boolean> endHand(@RequestParam long handId) {
        HandEntity hand = handService.getHandById(handId);
        handService.endHand(hand);
        return Collections.singletonMap("success", true);
    }


    /**
     * Sometimes it is nice to know that everything is working
     *
     * @return {"success":true}
     */
    @RequestMapping("/ping")
    public @ResponseBody Map<String, Boolean> pingServer() {
        return Collections.singletonMap("success", true);
    }
}
