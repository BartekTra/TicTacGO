package com.tictactoer.backend.game.controller.config;

import com.tictactoer.backend.game.domain.Game;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class GameConfig {

    @Bean
    public Map<String, Game> activeGames() {
        return new ConcurrentHashMap<>();
    }
}