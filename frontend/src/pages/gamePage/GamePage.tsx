import React from "react";
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
  const {
    gameData,
    sendMove,
    countdown,
    waitingForServerState,
    errorMessage,
    leaveGame,
  } = useTicTacToeSocket(
    gameId,
    user?.email,
    location.state?.initialGameData as GameData | null,
  );

  const mySymbol =
    gameData?.playerX === user.email
      ? "X"
      : gameData?.playerO === user.email
        ? "O"
        : null;

  const isMyTurn =
    mySymbol !== null &&
    gameData?.currentTurn === mySymbol &&
    gameData?.status === "IN_PROGRESS";

  const handleMove = (index: number) => {
    if (!gameData) return;
    if (!gameId) return;
    if (waitingForServerState) return;
    if (!isMyTurn) return;
    if (gameData.status !== "IN_PROGRESS") return;
    if (gameData.board[index] !== null) return;

    sendMove(gameId, index);
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
        <div className="mb-4">
          <button
            onClick={leaveGame}
            className="px-4 py-2 text-sm font-semibold bg-gray-800 hover:bg-gray-700 border border-gray-600 rounded-sm"
            disabled={waitingForServerState}
          >
            Opuść grę
          </button>
        </div>
        <GameInfo
          countdown={countdown}
          currentTurn={gameData.currentTurn}
          opponentId={
            mySymbol === "X" ? gameData.playerO : gameData.playerX
          }
          winner={gameData.winner}
          errorMessage={errorMessage}
        />
        <div className="flex h-full w-screen items-center justify-center gap-2">
          {/* gracz O */}
          <div className="self-start">
            <PlayerTimerWrapper
              isActive={
                gameData.status === "IN_PROGRESS" &&
                gameData.currentTurn === "O" &&
                gameData.playerO !== null
              }
              duration={15}
            >
              <PlayerInfo
                nickname={gameData.playerO ?? "Nie ma gracza"}
                symbol="O"
              />
            </PlayerTimerWrapper>
          </div>

          <GameBoard boardString={gameData.board} onMove={handleMove} />
          {/* gracz X */}
          <div className="self-end">
            <PlayerTimerWrapper
              isActive={
                gameData.status === "IN_PROGRESS" &&
                gameData.currentTurn === "X" &&
                gameData.playerX !== null
              }
              duration={15}
            >
              <PlayerInfo
                nickname={gameData.playerX ?? "Nie ma gracza"}
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
