import React, { useEffect } from "react";
import { useParams } from "react-router-dom";
import { useMutation } from "@apollo/client/react";
import { HANDLE_MOVE } from "../../graphql/mutations/games/handleMove";
import { GameBoard } from "./GameComponents/GameBoard";
import { GameInfo } from "./GameComponents/GameInfo";
import { useGameWebSocket } from "../../hooks/useGameWebSocket";
import { PlayerInfo } from "./GameComponents/PlayerInfo";
import PlayerTimerWrapper from "./GameComponents/PlayerTimerWrapper";
import type { GameData } from "../../types/GameData";
import type { User } from "../../types/User";

const GamePage: React.FC = () => {
  const { id: gameId } = useParams<{ id: string }>();

  const { gameData, gameBoard, currentTurn, winner, countdown } =
    useGameWebSocket(gameId);

  const [handleMoveMutation] = useMutation(HANDLE_MOVE, {
    fetchPolicy: "network-only",
    onError: (err) => console.log(err),
  });

  useEffect(() => {
    console.log(gameData, gameBoard, currentTurn, winner, countdown);
  }, [gameData, gameBoard, currentTurn, winner, countdown]);

  const handleMove = async (cellIndex: number) => {
    try {
      await handleMoveMutation({ variables: { cell: cellIndex } });
    } catch (err) {
      console.error("Błąd przy wysyłaniu ruchu:", err);
    }
  };

  if (!gameBoard || !gameData)
    return (
      <div className="h-screen w-screen flex justify-center items-center text-white bg-mybg">
        <h1>Ładowanie planszy ...</h1>
      </div>
    );

  return (
    <div className="bg-gray-900 h-screen w-screen flex flex-row justify-center items-center text-white">
      <div className="flex flex-col items-center">
        <GameInfo
          countdown={countdown}
          currentTurn={currentTurn}
          opponentId={gameData.player2 ? gameData.player2 : null}
          winner={winner}
        />
        <div className="flex h-full w-screen items-center justify-center gap-2">
          {/* gracz O */}
          <div className="self-start">
            <PlayerTimerWrapper
              isActive={
                currentTurn?.id === gameData.player1?.id &&
                gameData.player2 !== null &&
                gameData.winner === null
              }
              duration={15}
            >
              <PlayerInfo nickname={gameData.player1?.nickname ? gameData.player1.nickname : "Nie ma gracza"} symbol="O" />
            </PlayerTimerWrapper>
          </div>

          <GameBoard boardString={gameBoard} onMove={handleMove} />
          {/* gracz X */}
          <div className="self-end">
            <PlayerTimerWrapper
              isActive={
                currentTurn?.id === gameData.player2?.id &&
                gameData.player1 !== null &&
                gameData.winner === null
              }
              duration={15}
            >
              <PlayerInfo nickname={gameData.player2?.nickname ? gameData.player2.nickname : "Gracz nie ma nazwy?"} symbol="X" />
            </PlayerTimerWrapper>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GamePage;
