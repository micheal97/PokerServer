package com.hyphenated.card.controller;

import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.service.TableStructureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TasksController {
    @Autowired
    private SimpMessagingTemplate template;
    @Autowired
    TableStructureService tableStructureService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    {
        scheduler.scheduleAtFixedRate(() -> {
            tableStructureService.updateTables(Arrays.stream(BlindLevel.values()).map(Enum::name).toList());
            template.convertAndSend("/tables", tableStructureService.findAll());
        }, 0, 5, TimeUnit.SECONDS);
    }
}
