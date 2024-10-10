package com.hyphenated.card.controller;

import com.google.common.collect.ImmutableMap;
import com.hyphenated.card.Card;
import com.hyphenated.card.controller.dto.PlayerBet;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.domain.Player;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class TableTasksController {

    @Autowired
    private SimpMessagingTemplate template;
    @Setter
    private ImmutableMap<UUID, Game> Games = ImmutableMap.of();

    private Stream<UUID> getPlayerUuidStream(UUID gameId) {
        return Games.get(gameId).getPlayers().stream().map(Player::getId);
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
        template.convertAndSend("/playerfolded", name);
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

    public void sendFlop(List<Card> flop, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), "/flop", flop.stream().map(Card::name).toList()));
    }

    public void sendTurn(Card turn, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), "/turn", turn.name()));
    }

    public void sendRiver(Card river, UUID gameId) {
        getPlayerUuidStream(gameId).forEach(uuid -> template.convertAndSendToUser(uuid.toString(), "/river", river.name()));
    }
}