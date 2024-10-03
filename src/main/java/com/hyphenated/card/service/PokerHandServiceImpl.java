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
import com.hyphenated.card.dao.HandDao;
import com.hyphenated.card.dao.PlayerDao;
import com.hyphenated.card.dao.TableStructureDao;
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
    private HandDao handDao;

    @Autowired
    private TableStructureDao tableStructureDao;

    @Autowired
    private PlayerDao playerDao;

    public HandEntity handleNextGameStatus(TableStructure tableStructure) {
        if (tableStructure.getCurrentHand().getPlayers().size() < 2) {
            tableStructure.setGameStatusEndHand();
        } else {
            tableStructure.setNextGameStatus();
        }
        tableStructureDao.save(tableStructure);
        HandEntity currentHand = tableStructure.getCurrentHand();
        return switch (tableStructure.getGameStatus()) {
            case PREFLOP -> startNewHand(tableStructure);
            case FLOP -> flop(currentHand);
            case TURN -> turn(currentHand);
            case RIVER -> river(currentHand);
            case END_HAND -> {
                if (endHand(currentHand) == null) {
                    tableStructure.setGameStatusNotStarted();
                    tableStructureDao.save(tableStructure);
                    yield null;
                } else {
                    tableStructure.setNextGameStatus();
                    tableStructureDao.save(tableStructure);
                }
                yield startNewHand(tableStructure);
            }
            default -> throw new IllegalStateException("Unexpected value: " + tableStructure.getGameStatus());
        };
    }

    @Override
    @Transactional
    public HandEntity startNewHand(TableStructure tableStructure) {
        HandEntity hand = new HandEntity();
        hand.setTableStructure(tableStructure);
        Deck d = new Deck(true);
        Set<PlayerHand> participatingPlayers = new HashSet<>();
        for (Player p : tableStructure.getPlayers()) {
            if (p.getChips() > 0) {
                PlayerHand ph = new PlayerHand();
                ph.setHandEntity(hand);
                ph.setPlayer(p);
                ph.setCard1(d.dealCard());
                ph.setCard2(d.dealCard());
                participatingPlayers.add(ph);
            }
        }
        hand.setPlayers(participatingPlayers);

        //Sort and get the next player to act (immediately after the big blind)
        Player nextToAct = PlayerUtil.getNextPlayerToAct(hand, this.getPlayerInBB(hand));
        hand.setCurrentToAct(nextToAct);

        //Register the Forced Small and Big Blind bets as part of the hand
        Player smallBlind = getPlayerInSB(hand);
        Player bigBlind = getPlayerInBB(hand);
        int sbBet = 0;
        int bbBet = 0;
        for (PlayerHand ph : hand.getPlayers()) {
            if (ph.getPlayer().equals(smallBlind)) {
                sbBet = Math.min(hand.getBlindLevel().getSmallBlind(), smallBlind.getChips());
                ph.setBetAmount(sbBet);
                ph.setRoundBetAmount(sbBet);
                smallBlind.setChips(smallBlind.getChips() - sbBet);

            } else if (ph.getPlayer().equals(bigBlind)) {
                bbBet = Math.min(hand.getBlindLevel().getBigBlind(), bigBlind.getChips());
                ph.setBetAmount(bbBet);
                ph.setRoundBetAmount(bbBet);
                bigBlind.setChips(bigBlind.getChips() - bbBet);
            }

        }
        hand.setTotalBetAmount(hand.getBlindLevel().getBigBlind());
        hand.setLastBetAmount(hand.getBlindLevel().getBigBlind());
        hand.setPot(sbBet + bbBet);

        BoardEntity b = new BoardEntity();
        hand.setBoard(b);
        hand.setCards(d.exportDeck());
        hand = handDao.save(hand);

        tableStructure.setCurrentHand(hand);
        tableStructureDao.save(tableStructure);
        return hand;
    }

    @Override
    @Transactional
    public Player endHand(HandEntity hand) {
        if (!isActionResolved(hand)) {
            throw new IllegalStateException("There are unresolved betting actions");
        }

        TableStructure tableStructure = hand.getTableStructure();

        hand.setCurrentToAct(null);

        determineWinner(hand);

        //If players were eliminated this hand, set their finished position
        List<PlayerHand> phs = new ArrayList<>(hand.getPlayers());
        //Sort the list of PlayerHands so the one with the smallest chips at risk is first.
        //Use this to determine finish position if multiple players are eliminated on the same hand.
        phs.sort(new PlayerHandBetAmountComparator());
        for (PlayerHand ph : phs) {
            if (ph.getPlayer().getTableChips() <= 0) {
                tableStructure.removePlayer(ph.getPlayer());
                tableStructureDao.save(tableStructure);
            }
        }

        //For all players in the game, remove any who are out of chips (eliminated)
        List<Player> players = new ArrayList<>();
        Player playerInBTN = null;
        for (Player p : tableStructure.getPlayers()) {
            if (p.equals(tableStructure.getPlayerInBTN())) {
                //If the player on the Button has been eliminated, we still need this player
                //in the list so that we calculate next button from its position
                playerInBTN = p;
                players.add(playerInBTN);
            } else if (p.getTableChips() != 0) {
                players.add(p);
            }
        }
        if (tableStructure.getPlayers().size() < 2) {
            return null;
        }

        //Rotate Button.  Use Simplified Moving Button algorithm (for ease of coding)
        //This means we always rotate button.  Blinds will be next two active players.  May skip blinds.
        Player nextButton = PlayerUtil.getNextPlayerInGameOrder(players, playerInBTN);
        tableStructure.setPlayerInBTN(nextButton);
        assert playerInBTN != null;
        if (playerInBTN.getTableChips() == 0) {
            players.remove(playerInBTN);
        }

        tableStructureDao.save(tableStructure);

        //Remove Deck from database. No need to keep that around anymore
        hand.setCards(new ArrayList<>());
        handDao.save(hand);
        return nextButton;
    }

    @Override
    @Transactional(readOnly = true)
    public HandEntity getHandById(long id) {
        return handDao.findById(id);
    }

    @Override
    @Transactional
    public HandEntity saveHand(HandEntity hand) {
        return handDao.save(hand);
    }


    @Override
    @Transactional
    public HandEntity flop(HandEntity hand) throws IllegalStateException {
        if (hand.getBoard().getFlop1() != null) {
            throw new IllegalStateException("Unexpected Flop.");
        }
        //Re-attach to persistent context for this transaction (Lazy Loading stuff)
        if (!isActionResolved(hand)) {
            throw new IllegalStateException("There are unresolved preflop actions");
        }

        Deck d = new Deck(hand.getCards());
        d.shuffleDeck();
        BoardEntity board = hand.getBoard();
        board.setFlop1(d.dealCard());
        board.setFlop2(d.dealCard());
        board.setFlop3(d.dealCard());
        hand.setCards(d.exportDeck());
        resetRoundValues(hand);
        return handDao.save(hand);
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
        Player button = hand.getTableStructure().getPlayerInBTN();
        //Heads up the Button is the Small Blind
        if (hand.getPlayers().size() == 2) {
            return button;
        }
        List<PlayerHand> players = new ArrayList<>(hand.getPlayers());
        return PlayerUtil.getNextPlayerInGameOrderPH(players, button);
    }

    @Override
    public Player getPlayerInBB(HandEntity hand) {
        Player button = hand.getTableStructure().getPlayerInBTN();
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
        Player btn = hand.getTableStructure().getPlayerInBTN();
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
