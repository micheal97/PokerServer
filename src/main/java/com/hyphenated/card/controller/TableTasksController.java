package com.hyphenated.card.controller;

import com.hyphenated.card.controller.dto.PlayerBet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TableTasksController {

    @Autowired
    private SimpMessagingTemplate template;

    public void playerJoined(String name) {
        template.convertAndSend("/playerJoined", name);
    }

    public void playerFolded(String name) {
        template.convertAndSend("/playerFolded", name);
    }

    public void playerCalled(String name) {
        template.convertAndSend("/playerCalled", name);
    }

    public void playerChecked(String name) {
        template.convertAndSend("/playerChecked", name);
    }

    public void playerBet(PlayerBet playerBet) {
        template.convertAndSend("/playerBet", playerBet);
    }

    public void playerSitin(String name) {
        template.convertAndSend("/playerSitin", name);
    }

    public void sendPlayerHandId(UUID playerId, UUID playerHandId) {
        template.convertAndSendToUser(playerId.toString(), "/handId", playerHandId);
    }

    public void playersTurn(String name) {
        template.convertAndSend("/playersTurn", name);
    }
}