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
import com.hyphenated.card.dao.GameDao;
import com.hyphenated.card.dao.PlayerDao;
import com.hyphenated.card.domain.*;
import com.hyphenated.card.util.PlayerHandBetAmountComparator;
import com.hyphenated.card.util.PlayerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PokerHandServiceImpl implements PokerHandService {


    @Autowired
    private GameDao gameDao;

    @Autowired
    private PlayerDao playerDao;

    public Optional<HandEntity> handleNextGameStatus(Game game) {
        if (game.getHand().getPlayers().size() < 2) {
            game.setGameStatusEndHand();
        } else {
            game.setNextGameStatus();
        }
        gameDao.save(game);
        HandEntity currentHand = game.getHand();
        return switch (game.getGameStatus()) {
            case PREFLOP -> Optional.of(startNewHand(game));
            case FLOP -> Optional.of(flop(currentHand));
            case TURN -> Optional.of(turn(currentHand));
            case RIVER -> Optional.of(river(currentHand));
            case END_HAND -> {
                if (endHand(currentHand) == null) {
                    game.setGameStatusNotStarted();
                    gameDao.save(game);
                    yield Optional.empty();
                } else {
                    game.setNextGameStatus();
                    gameDao.save(game);
                }
                yield Optional.of(startNewHand(game));
            }
            default -> throw new IllegalStateException("Unexpected value: " + game.getGameStatus());
        };
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
        Player nextToAct = PlayerUtil.getNextPlayerToAct(hand, this.getPlayerInBB(hand));
        hand.setCurrentToAct(nextToAct);

        //Register the Forced Small and Big Blind bets as part of the hand
        Player smallBlind = getPlayerInSB(hand);
        int sbBet = Math.min(game.getBlindLevel().getSmallBlind(), smallBlind.getChips());
        Optional.ofNullable(smallBlind.getPlayerHand()).ifPresent(playerHand -> {
            playerHand.setBetAmount(sbBet);
            playerHand.setRoundBetAmount(sbBet);
        });
        smallBlind.setChips(smallBlind.getChips() - sbBet);
        Player bigBlind = getPlayerInBB(hand);
        int bbBet = Math.min(game.getBlindLevel().getBigBlind(), bigBlind.getChips());
        Optional.ofNullable(bigBlind.getPlayerHand()).ifPresent(playerHand -> {
            playerHand.setBetAmount(bbBet);
            playerHand.setRoundBetAmount(bbBet);
        });
        bigBlind.setChips(bigBlind.getChips() - bbBet);
        hand.setTotalBetAmount(game.getBlindLevel().getBigBlind());
        hand.setLastBetAmount(game.getBlindLevel().getBigBlind());
        hand.setPot(sbBet + bbBet);

        BoardEntity b = new BoardEntity();
        hand.setBoard(b);
        game.setHand(hand);
        playerDao.save(bigBlind);
        playerDao.save(smallBlind);
        gameDao.save(game);
        return hand;
    }

    @Override
    @Transactional
    public Player endHand(Game game) {
        HandEntity hand = game.getHand();
        if (!isActionResolved(hand)) {
            throw new IllegalStateException("There are unresolved betting actions");
        }

        hand.setCurrentToAct(null);

        determineWinner(hand);
        Player playerInBTN = game.findPlayerInBTN().orElse(game.getPlayers().first());
        game.getPlayers().stream().filter(player -> player.getTableChips() <= 0).forEach(game::removePlayer);
        if (game.getPlayers().size() < 2) {
            return null;
        }
        boolean playerToRemove = game.addPlayer(playerInBTN);
        hand.addAllPlayers(game.getPlayers());
        //Rotate Button.  Use Simplified Moving Button algorithm (for ease of coding)
        //This means we always rotate button.  Blinds will be next two active players.  May skip blinds.
        Player nextButton = PlayerUtil.getNextPlayerToAct(game.getHand(), playerInBTN);
        assert nextButton != null;
        nextButton.setPlayerInButton(true);
        assert playerInBTN != null;
        if (playerToRemove) {
            game.removePlayer(playerInBTN);
            hand.removePlayer(playerInBTN);
        }
        gameDao.save(game);
        return nextButton;
    }


    @Override
    @Transactional
    public Game flop(Game game) throws IllegalStateException {
        HandEntity hand = game.getHand();
        if (hand.getBoard().getFlop1() != null) {
            throw new IllegalStateException("Unexpected Flop.");
        }
        //Re-attach to persistent context for this transaction (Lazy Loading stuff)
        if (!isActionResolved(hand)) {
            throw new IllegalStateException("There are unresolved preflop actions");
        }
        BoardEntity board = hand.getBoard();
        Deck deck = hand.getDeck();
        board.setFlop1(deck.dealCard());
        board.setFlop2(deck.dealCard());
        board.setFlop3(deck.dealCard());
        hand.setDeck(deck);
        resetRoundValues(hand);//TODO:Check PlayerUtil.next()
        game.setHand(hand);
        return gameDao.save(game);
    }

    @Override
    @Transactional
    public HandEntity turn(HandEntity hand) throws IllegalStateException {
        if (hand.getBoard().getFlop1() == null || hand.getBoard().getTurn() != null) {
            throw new IllegalStateException("Unexpected Turn.");
        }
        if (!isActionResolved(hand)) {
            throw new IllegalStateException("There are unresolved flop actions");
        }

        Deck d = new Deck(hand.getCards());
        d.shuffleDeck();
        BoardEntity board = hand.getBoard();
        board.setTurn(d.dealCard());
        hand.setCards(d.exportDeck());
        resetRoundValues(hand);
        return handDao.save(hand);
    }

    @Override
    @Transactional
    public HandEntity river(HandEntity hand) throws IllegalStateException {
        if (hand.getBoard().getFlop1() == null || hand.getBoard().getTurn() == null
                || hand.getBoard().getRiver() != null) {
            throw new IllegalStateException("Unexpected River.");
        }
        if (!isActionResolved(hand)) {
            throw new IllegalStateException("There are unresolved turn actions");
        }

        Deck d = new Deck(hand.getCards());
        d.shuffleDeck();
        BoardEntity board = hand.getBoard();
        board.setRiver(d.dealCard());
        hand.setCards(d.exportDeck());
        resetRoundValues(hand);
        return handDao.save(hand);
    }

    @Override
    public Player getPlayerInSB(HandEntity hand) {
        Player button = hand.getGame().getPlayerInBTN();
        //Heads up the Button is the Small Blind
        if (hand.getPlayers().size() == 2) {
            return button;
        }
        List<PlayerHand> players = new ArrayList<>(hand.getPlayers());
        return PlayerUtil.getNextPlayerInGameOrderPH(players, button);
    }

    @Override
    public Player getPlayerInBB(HandEntity hand) {
        Player button = hand.getGame().getPlayerInBTN();
        List<PlayerHand> players = new ArrayList<>(hand.getPlayers());
        Player leftOfButton = PlayerUtil.getNextPlayerInGameOrderPH(players, button);
        //Heads up, the player who is not the Button is the Big blind
        if (hand.getPlayers().size() == 2) {
            return leftOfButton;
        }
        return PlayerUtil.getNextPlayerInGameOrderPH(players, leftOfButton);
    }

    private void resetRoundValues(HandEntity hand) {
        hand.setTotalBetAmount(0);
        hand.setLastBetAmount(0);

        List<Player> playersInHand = new ArrayList<Player>();
        for (PlayerHand ph : hand.getPlayers()) {
            ph.setRoundBetAmount(0);
            playersInHand.add(ph.getPlayer());
        }
        //Next player is to the left of the button.  Given that the button may have been eliminated
        //In a round of betting, we need to put the button back to determine relative position.
        Player btn = hand.getGame().getPlayerInBTN();
        if (!playersInHand.contains(btn)) {
            playersInHand.add(btn);
        }

        Player next = PlayerUtil.getNextPlayerInGameOrder(playersInHand, btn);
        Player firstNext = next;

        //Skip all in players and players that are sitting out
        while (next.getChips() <= 0 || next.isSittingOut()) {
            next = PlayerUtil.getNextPlayerInGameOrder(playersInHand, next);
            if (next.equals(firstNext)) {
                //Exit condition if all players are all in.
                break;
            }
        }
        hand.setCurrentToAct(next);
    }

    private void determineWinner(HandEntity hand) {
        //if only one PH left, everyone else folded
        if (hand.getPlayers().size() == 1) {
            Player winner = hand.getPlayers().iterator().next().getPlayer();
            winner.setChips(winner.getChips() + hand.getPot());
            playerDao.save(winner);
        } else {
            //Refund all in overbet player if applicable before determining winner
            refundOverbet(hand);

            //Iterate through map of players to their amount won.  Persist.
            Map<Player, Integer> winners = PlayerUtil.getAmountWonInHandForAllPlayers(hand);
            if (winners == null) {
                return;
            }
            for (Map.Entry<Player, Integer> entry : winners.entrySet()) {
                Player player = entry.getKey();
                player.setChips(player.getChips() + entry.getValue());
                playerDao.save(player);
            }
        }
    }

    private void refundOverbet(HandEntity hand) {
        List<PlayerHand> phs = new ArrayList<>(hand.getPlayers());
        //Sort from most money contributed, to the least
        phs.sort(new PlayerHandBetAmountComparator());
        Collections.reverse(phs);
        //If there are at least 2 players, and the top player contributed more to the pot than the next player
        if (phs.size() >= 2 && (phs.get(0).getBetAmount() > phs.get(1).getBetAmount())) {
            //Refund that extra amount contributed. Remove from pot, add back to player
            int diff = phs.get(0).getBetAmount() - phs.get(1).getBetAmount();
            phs.get(0).setBetAmount(phs.get(1).getBetAmount());
            phs.get(0).getPlayer().setChips(phs.get(0).getPlayer().getChips() + diff);
            hand.setPot(hand.getPot() - diff);
        }
    }

    //Helper method to see if there are any outstanding actions left in a betting round
    private boolean isActionResolved(HandEntity hand) {
        int roundBetAmount = hand.getTotalBetAmount();
        for (PlayerHand ph : hand.getPlayers()) {
            //All players should have paid the roundBetAmount or should be all in
            if (ph.getRoundBetAmount() != roundBetAmount && ph.getPlayer().getChips() > 0) {
                return false;
            }
        }
        return true;
    }

}
