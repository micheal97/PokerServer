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

import com.hyphenated.card.Deck;
import com.hyphenated.card.controller.TableTasksController;
import com.hyphenated.card.dao.GameDao;
import com.hyphenated.card.dao.PlayerDao;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.HandEntity;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.domain.PlayerHand;
import com.hyphenated.card.util.PlayerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PokerHandServiceImpl implements PokerHandService {

    @Autowired
    private GameDao gameDao;
    @Autowired
    private PlayerDao playerDao;
    @Autowired
    private PlayerActionService playerActionService;
    @Autowired
    private TableTasksController tableTasksController;

    public void handleNextGameStatus(Game game) {
        if (game.getHand().getPlayers().size() < 2) {
            game.setGameStatusEndHand();
        } else {
            game.setNextGameStatus();
        }
        gameDao.save(game);
        resetRoundValues(game);
        switch (game.getGameStatus()) {
            case PREFLOP -> startNewHand(game);
            case FLOP -> flop(game);
            case TURN -> turn(game);
            case RIVER -> river(game);
            case END_HAND -> {
                if (endHand(game)) {
                    game.setGameStatusNotStarted();
                    gameDao.save(game);
                } else {
                    game.setNextGameStatus();
                    gameDao.save(game);
                    startNewHand(game);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + game.getGameStatus());
        }
        ;
    }

    @Override
    @Transactional
    public HandEntity startNewHand(Game game) {
        HandEntity hand = new HandEntity(game);
        game.setHand(hand);
        Deck deck = hand.getDeck();
        hand.getPlayers().forEach(player -> player
                .setPlayerHand(new PlayerHand(deck.dealCard(), deck.dealCard())));

        //Sort and get the next player to act (immediately after the big blind)
        Player nextToAct = PlayerUtil.getNextPlayerToAct(hand, getPlayerInBB(hand));
        hand.setCurrentToAct(nextToAct);

        //Register the Forced Small and Big Blind bets as part of the hand
        Player smallBlind = getPlayerInSB(hand);
        int sbBet = Math.min(game.getBlindLevel().getSmallBlind(), smallBlind.getChips());
        Optional.ofNullable(smallBlind.getPlayerHand()).ifPresent(playerHand -> {
            playerHand.setBetAmount(sbBet);
            playerHand.setRoundBetAmount(sbBet);
        });
        smallBlind.removeTableChips(sbBet);
        Player bigBlind = getPlayerInBB(hand);
        int bbBet = Math.min(game.getBlindLevel().getBigBlind(), bigBlind.getChips());
        Optional.ofNullable(bigBlind.getPlayerHand()).ifPresent(playerHand -> {
            playerHand.setBetAmount(bbBet);
            playerHand.setRoundBetAmount(bbBet);
        });
        bigBlind.removeTableChips(bbBet);
        hand.setBetAmount(game.getBlindLevel().getBigBlind());
        hand.setLastBetAmount(game.getBlindLevel().getBigBlind());
        hand.setPot(sbBet + bbBet);

        HandEntity newHand = new HandEntity(game);
        game.setHand(hand);
        tableTasksController.sendCardsToUser()
        playerDao.save(bigBlind);
        playerDao.save(smallBlind);
        gameDao.save(game);
        return newHand;
    }

    @Override
    @Transactional
    public boolean endHand(Game game) {
        HandEntity hand = game.getHand();
        handleWinners(hand);
        Player playerInBTN = hand.findPlayerInBTN().orElse(game.getPlayers().first());
        game.getPlayers().stream().filter(player -> player.getTableChips() <= 0).forEach(game::removePlayer);
        if (game.getPlayers().size() < 2) {
            return true;
        }
        boolean playerToRemove = game.addPlayer(playerInBTN);
        hand.addAllPlayers(game.getPlayers());
        //Rotate Button.  Use Simplified Moving Button algorithm (for ease of coding)
        //This means we always rotate button.  Blinds will be next two active players.  May skip blinds.
        Optional<Player> btn = hand.findPlayerInBTN();
        btn.ifPresent(player ->
                player.setPlayerInButton(false));
        Optional<Player> nextButton = Optional.ofNullable(PlayerUtil.getNextPlayerToAct(hand, btn.orElse(null)));
        if (nextButton.isPresent()) {
            nextButton.get().setPlayerInButton(true);
        } else {
            hand.getPlayers().first().setPlayerInButton(true);
        }
        hand.setCurrentToAct(null);
        if (playerToRemove) {
            game.removePlayer(playerInBTN);
            hand.removePlayer(playerInBTN);
        }
        game.setHand(hand);

        tableTasksController.sendPlayersForUpdatingAmountsWon(hand.getPlayers())
        gameDao.save(game);
        return false;
    }


    @Override
    @Transactional
    public void flop(Game game) throws IllegalStateException {
        tableTasksController.sendFlop(game.getHand().getBoard().getFlop(), game.getId());
    }

    @Override
    @Transactional
    public void turn(Game game) throws IllegalStateException {
        tableTasksController.sendTurn(game.getHand().getBoard().getTurn(), game.getId());
    }

    @Override
    @Transactional
    public void river(Game game) throws IllegalStateException {
        tableTasksController.sendTurn(game.getHand().getBoard().getRiver(), game.getId());

    }

    @Override
    public Player getPlayerInSB(HandEntity hand) {
        Player button = hand.findPlayerInBTN().orElseThrow(() -> new IllegalStateException("No player in btn"));
        //Heads up the Button is the Small Blind
        if (hand.getPlayers().size() == 2) {
            return button;
        }
        return PlayerUtil.getNextPlayerToAct(hand, button);
    }

    @Override
    public Player getPlayerInBB(HandEntity hand) {
        Optional<Player> button = hand.findPlayerInBTN();
        Player leftOfButton = PlayerUtil.getNextPlayerToAct(hand, button.orElse(null));
        //Heads up, the player who is not the Button is the Big blind
        if (hand.getPlayers().size() == 2) {
            return leftOfButton;
        }
        return PlayerUtil.getNextPlayerToAct(hand, leftOfButton);
    }

    private void resetRoundValues(Game game) {
        HandEntity hand = game.getHand();
        hand.setBetAmount(0);
        hand.setLastBetAmount(0);
        hand.getPlayers().stream().filter(player -> player.getPlayerHand() != null).forEach(player -> {
            PlayerHand playerHand = player.getPlayerHand();
            playerHand.setRoundBetAmount(0);
            playerHand.setRoundAction(null);
            player.setPlayerHand(playerHand);
            playerDao.save(player);
        });
        //Next player is to the left of the button.  Given that the button may have been eliminated
        //In a round of betting, we need to put the button back to determine relative position.
        Player btn = hand.findPlayerInBTN().orElseThrow(() -> new IllegalStateException("No Player in Button"));
        hand.setCurrentToAct(btn);
        game.setHand(hand);
        gameDao.save(game);
    }

    private void handleWinners(HandEntity hand) {
        Map<Integer, List<Player>> playerBetAmountMap = hand.getPlayers().stream()
                .filter(player -> player.getPlayerHand() != null)
                .collect(Collectors.toConcurrentMap(player -> player.getPlayerHand().getBetAmount()
                        , List::of, (s, a) -> {
                            List<Player> players = new ArrayList<>();
                            players.addAll(s);
                            players.addAll(a);
                            return List.copyOf(players);
                        }));
        TreeMap<Integer, List<Player>> sortedMap = new TreeMap<>(playerBetAmountMap);
        sortedMap.put(0, Collections.emptyList());
        calculateWinners(sortedMap);
    }

    private void calculateWinners(TreeMap<Integer, List<Player>> sortedMap) {
        Optional.ofNullable(sortedMap.lastEntry()).ifPresent(lastEntry -> {
            Optional<Map.Entry<Integer, List<Player>>> optionalLowerEntry = Optional.ofNullable(sortedMap.lowerEntry(lastEntry.getKey()));
            if (optionalLowerEntry.isPresent()) {
                int diff = lastEntry.getKey() - optionalLowerEntry.get().getKey();
                List<Player> winners = lastEntry.getValue();
                PlayerUtil.getAmountWonInHandForAllPlayers(winners, diff).forEach((player, betAmount) -> {
                    player.addTableChips(betAmount);
                    playerDao.save(player);
                });
            }
        });
    }


}
