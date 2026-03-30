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

    @Override
    public void executeMove(String playerSymbol, int position) {
        List<Integer> playerMoves = playerSymbol.equals("X") ? movesX : movesO;

        boolean isOverwritingOldest =
                playerMoves.size() == 3 && playerMoves.get(0).equals(position);

        if (boardCells[position] != null && !isOverwritingOldest) {
            throw new IllegalStateException("To pole jest już zajęte!");
        }

        if (playerMoves.size() == 3) {
            int oldestPosition = playerMoves.remove(0);
            boardCells[oldestPosition] = null;
        }

        boardCells[position] = playerSymbol;
        playerMoves.add(position);

        // Mutujemy listy/array, więc wymuszamy zmianę referencji dla Dirty Checking.
        if (playerSymbol.equals("X")) {
            movesX = playerMoves;
        } else {
            movesO = playerMoves;
        }

        notifyBoardChanged();
        notifyMovesChanged();
    }
}

