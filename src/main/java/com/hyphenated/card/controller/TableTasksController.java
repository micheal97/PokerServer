package com.hyphenated.card.controller;

import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.dto.Cards;
import com.hyphenated.card.dto.PlayerBet;
import com.hyphenated.card.dto.PlayerCards;
import com.hyphenated.card.enums.Card;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.hyphenated.card.WsUrlConstant.*;

@Controller
public class TableTasksController {

    @Autowired
    private SimpMessagingTemplate template;
    @Setter
    private Map<UUID, Game> games = Map.of();

    private Stream<UUID> getPlayerUuidStream(UUID gameId) {
        return games.get(gameId).getPlayers().stream().map(Player::getId);
    }

    public void playerJoined(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), PLAYER_JOINED, name));
    }


    public void playerLeft(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), PLAYER_LEFT, name));
    }

    public void gameStopped(UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), GAME_STOPPED, true));
    }

    public void playerFolded(String name, UUID gameId) {

        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), PLAYER_FOLDED, name));
    }

    public void playerCalled(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), PLAYER_CALLED, name));
    }

    public void playerChecked(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), PLAYER_CHECKED, name));
    }

    public void playerBet(PlayerBet playerBet, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), PLAYER_BET, playerBet));
    }

    public void playersTurn(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), PLAYERS_TURN, name));
    }

    public void sendFlop(Cards flop, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), FLOP, flop.getCards().stream().map(Card::name).toList()));
    }

    public void sendTurn(Card turn, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), TURN, turn.name()));
    }

    public void sendRiver(Card river, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), RIVER, river.name()));
    }

    public void endGame(PlayerCards playerCards, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), AMOUNTS_WON, playerCards));
    }

    public void sendCardsToUser(Cards cards, UUID userId) {
        template.convertAndSendToUser(userId.toString(), CARDS, cards);
    }
}