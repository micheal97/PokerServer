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
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Component
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
                template.convertAndSendToUser(uuid.toString(), "/playerjoined", name));
    }


    public void playerLeft(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), "/playerleft", name));
    }

    public void gameStopped(UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), "/gamestopped", true));
    }

    public void playerFolded(String name, UUID gameId) {

        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), "/playerfolded", name));
    }

    public void playerCalled(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), "/playercalled", name));
    }

    public void playerChecked(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), "/playerchecked", name));
    }

    public void playerBet(PlayerBet playerBet, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), "/playerbet", playerBet));
    }

    public void playersTurn(String name, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid ->
                template.convertAndSendToUser(uuid.toString(), "/playersturn", name));
    }

    public void sendFlop(Cards flop, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), "/flop", flop.getCards().stream().map(Card::name).toList()));
    }

    public void sendTurn(Card turn, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), "/turn", turn.name()));
    }

    public void sendRiver(Card river, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), "/river", river.name()));
    }

    public void endGame(PlayerCards playerCards, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), "/amountsWon", playerCards));
    }

    public void sendCardsToUser(Cards cards, UUID userId) {
        template.convertAndSendToUser(userId.toString(), "/cards", cards);
    }
}