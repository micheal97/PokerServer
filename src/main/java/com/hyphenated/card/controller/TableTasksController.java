package com.hyphenated.card.controller;

import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import com.hyphenated.card.dto.*;
import com.hyphenated.card.enums.Card;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.stream.Stream;

import static com.hyphenated.card.WsUrlConstant.*;

@Controller
public class TableTasksController {

    @Autowired
    private SimpMessagingTemplate template;
    @Setter
    private Map<String, Game> games = Map.of();

    private Stream<String> getPlayerUuidStream(String gameId) {
        return games.get(gameId).getPlayers().stream().map(Player::getId);
    }

    public void playerJoined(String name, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid, PLAYER_JOINED, name));
    }


    public void playerLeft(String name, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid, PLAYER_LEFT, name));
    }

    public void gameStopped(String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid, GAME_STOPPED, true));
    }

    public void playerFolded(PlayerDTO playerDTO, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid, PLAYER_FOLDED, playerDTO));
    }

    public void playerCalled(PlayerBet playerBet, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid, PLAYER_CALLED, playerBet));
    }

    public void playerChecked(PlayerDTO playerDTO, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid, PLAYER_CHECKED, playerDTO));
    }

    public void playerBet(PlayerBet playerBet, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid, PLAYER_BET, playerBet));
    }

    public void playersTurn(String name, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid, PLAYERS_TURN, name));
    }

    public void sendFlop(Cards flop, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid, FLOP, flop.getCards().stream().map(Card::name).toList()));
    }

    public void sendTurn(Card turn, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid, TURN, turn.name()));
    }

    public void sendRiver(Card river, String gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid, RIVER, river.name()));
    }

    public void endGame(PlayerCards playerCards, String gameId, PlayersWonOrderDTO playersWonOrder) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid, AMOUNTS_WON, new EndGameDTO(playerCards, playersWonOrder)));
    }

    public void sendCardsToUser(Cards cards, String userId) {
        template.convertAndSendToUser(userId, CARDS, cards);
    }
}