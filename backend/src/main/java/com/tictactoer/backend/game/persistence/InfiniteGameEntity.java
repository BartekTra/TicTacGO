package com.tictactoer.backend.game.persistence;

import com.tictactoer.backend.game.domain.GameMode;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.List;

@Entity
@DiscriminatorValue("INFINITE")
public class InfiniteGameEntity extends GameEntity {

    protected InfiniteGameEntity() {
        super();
    }

    public InfiniteGameEntity(String playerX, String playerO) {
        super(playerX, playerO, GameMode.INFINITE);
    }
}

