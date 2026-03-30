import React, { useEffect } from "react";
import { useLocation, useParams } from "react-router-dom";
import { GameBoard } from "./GameComponents/GameBoard";
import { GameInfo } from "./GameComponents/GameInfo";
import {
  useTicTacToeSocket,
  type GameData,
} from "./GameComponents/hooks/useGameWebSocket";

import { PlayerInfo } from "./GameComponents/PlayerInfo";
import PlayerTimerWrapper from "./GameComponents/PlayerTimerWrapper";
import { useUser } from "../../context/UserContext";

const GamePage: React.FC = () => {
  const { gameId } = useParams<string>();
  const { user } = useUser();
  if (!user || !gameId) return <p> loading </p>;
  const location = useLocation();
  const { gameData, sendMove, countdown } = useTicTacToeSocket(
    gameId,
    user?.email,
    location.state?.initialGameData as GameData | null,
  );

  const mySymbol = gameData?.playerX === user?.email ? "X" : "O";
  const isMyTurn =
    gameData?.currentTurn === mySymbol && gameData?.status === "IN_PROGRESS";

  const handleMove = (index: number) => {
    sendMove(gameId, index);
    console.log(gameData?.board);
  };

  if (!gameData)
    return (
      <div className="min-h-screen bg-gray-900 flex justify-center">
        <h2 className="text-white mt-20">Ładowanie gry...</h2>
      </div>
    );

  return (
    <div className="bg-gray-900 h-screen w-screen flex flex-row justify-center items-center text-white">
      <div className="flex flex-col items-center">
        <GameInfo
          countdown={countdown}
          currentTurn={gameData.currentTurn}
          opponentId={
            gameData.playerO === user?.email
              ? gameData.playerO
              : gameData.playerX
          }
          winner={gameData.winner}
        />
        <div className="flex h-full w-screen items-center justify-center gap-2">
          {/* gracz O */}
          <div className="self-start">
            <PlayerTimerWrapper
              isActive={
                gameData.currentTurn === gameData.playerO &&
                gameData.playerX !== null
              }
              duration={15}
            >
              <PlayerInfo
                nickname={gameData.playerX ? gameData.playerX : "Nie ma gracza"}
                symbol="O"
              />
            </PlayerTimerWrapper>
          </div>

          <GameBoard boardString={gameData.board} onMove={handleMove} />
          {/* gracz X */}
          <div className="self-end">
            <PlayerTimerWrapper
              isActive={
                gameData.currentTurn === gameData.playerX &&
                gameData.playerO !== null
              }
              duration={15}
            >
              <PlayerInfo
                nickname={gameData.playerO ? gameData.playerO : "Nie ma gracza"}
                symbol="X"
              />
            </PlayerTimerWrapper>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GamePage;
