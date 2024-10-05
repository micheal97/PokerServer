package com.hyphenated.card.controller;

import com.google.common.collect.ImmutableMap;
import com.hyphenated.card.domain.BlindLevel;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.service.GameService;
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
    GameService GameService;
    @Autowired
    TableTasksController tableTasksController;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    {
        scheduler.scheduleAtFixedRate(() -> {
            GameService.updateTables(Arrays.stream(BlindLevel.values()).map(Enum::name).toList());
            List<Game> games = GameService.findAll();
            template.convertAndSend("/tables", games.stream().map(Game::getGameDTO).toList());
            ImmutableMap<UUID, Game> GameMap = ImmutableMap.copyOf(games.stream().collect(Collectors.toConcurrentMap(Game::getId, Function.identity())));
            tableTasksController.setGames(GameMap);
        }, 0, 5, TimeUnit.SECONDS);
    }
}
