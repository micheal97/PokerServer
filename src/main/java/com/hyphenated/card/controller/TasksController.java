package com.hyphenated.card.controller;

import com.google.common.collect.ImmutableMap;
import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.domain.TableStructure;
import com.hyphenated.card.service.TableStructureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TasksController {
    @Autowired
    private SimpMessagingTemplate template;
    @Autowired
    TableStructureService tableStructureService;
    @Autowired
    TableTasksController tableTasksController;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    {
        scheduler.scheduleAtFixedRate(() -> {
            tableStructureService.updateTables(Arrays.stream(BlindLevel.values()).map(Enum::name).toList());
            List<TableStructure> tableStructures = tableStructureService.findAll();
            template.convertAndSend("/tables", tableStructures.stream().map(TableStructure::getTableStructureDTO).toList());
            ImmutableMap<UUID, TableStructure> tableStructureMap = ImmutableMap.copyOf(tableStructures.stream().collect(Collectors.toConcurrentMap(TableStructure::getId, Function.identity())));
            tableTasksController.setTableStructures(tableStructureMap);
        }, 0, 5, TimeUnit.SECONDS);
    }
}
