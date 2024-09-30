package com.hyphenated.card.controller;

import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.service.TableStructureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;

public class TasksController {
    @Autowired
    private SimpMessagingTemplate template;
    @Autowired
    TableStructureService tableStructureService;

    @Scheduled(fixedRate = 5_000L)
    public void updateTables() {
        tableStructureService.updateTables(Arrays.stream(BlindLevel.values()).map(Enum::name).toList());
        template.convertAndSend("/tables", tableStructureService.findAll());
    }
}
