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
        template.convertAndSend("/playerjoined", name);
    }

    public void playerLeft(String name) {
        template.convertAndSend("/playerleft", name);
    }

    public void playerFolded(String name) {
        template.convertAndSend("/playerfolded", name);
    }

    public void playerCalled(String name) {
        template.convertAndSend("/playercalled", name);
    }

    public void playerChecked(String name) {
        template.convertAndSend("/playerchecked", name);
    }

    public void playerBet(PlayerBet playerBet) {
        template.convertAndSend("/playerbet", playerBet);
    }

    public void playerSitin(String name) {
        template.convertAndSend("/playersitin", name);
    }

    public void sendPlayerHandId(UUID playerId, UUID playerHandId) {
        template.convertAndSendToUser(playerId.toString(), "/handid", playerHandId);
    }

    public void playersTurn(String name) {
        template.convertAndSend("/playersturn", name);
    }
}