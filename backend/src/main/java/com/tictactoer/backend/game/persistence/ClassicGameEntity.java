package com.tictactoer.backend.game.persistence;

import com.tictactoer.backend.game.domain.GameMode;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CLASSIC")
public class ClassicGameEntity extends GameEntity {

    protected ClassicGameEntity() {
        super();
    }

    public ClassicGameEntity(String playerX, String playerO) {
        super(playerX, playerO, GameMode.CLASSIC);
    }
}

