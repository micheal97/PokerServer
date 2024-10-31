package com.hyphenated.card.controller;

import com.hyphenated.card.UrlConstant;
import com.hyphenated.card.domain.Game;
import com.hyphenated.card.dto.GameDTOs;
import com.hyphenated.card.enums.BlindLevel;
import com.hyphenated.card.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class TasksController {
    //TODO:TEST if works
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
            template.convertAndSend(UrlConstant.GAMES, new GameDTOs(games.stream().map(Game::getGameDTO).toList()));
            Map<String, Game> GameMap = Map.copyOf(games.stream().collect(Collectors.toConcurrentMap(Game::getId, Function.identity())));
            tableTasksController.setGames(GameMap);
        }, 0, 5, TimeUnit.SECONDS);
    }
}
