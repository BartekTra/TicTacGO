import React from "react";
import { useParams, useLocation } from "react-router-dom";
import { useUser } from "../../context/UserContext";
import { useTicTacToeSocket, type GameData } from "./GameComponents/hooks/useGameWebSocket";

const TicTacToeGame: React.FC = () => {
  const { gameId } = useParams<string>();
  if (!gameId) return <p>XD</p>
  const location = useLocation();
  const { user } = useUser();

  const { gameData, sendMove, waitingForServerState, leaveGame } = useTicTacToeSocket(
    gameId,
    user?.email,
    location.state?.initialGameData as GameData | null,
  );

  const mySymbol =
    gameData?.playerX === user?.email
      ? "X"
      : gameData?.playerO === user?.email
        ? "O"
        : null;
  const isMyTurn =
    mySymbol !== null &&
    gameData?.currentTurn === mySymbol &&
    gameData?.status === "IN_PROGRESS";

  const handleCellClick = (index: number) => {
    if (!isMyTurn) return;
    if (!gameData) return;
    if (waitingForServerState) return;
    if (gameData.board[index] !== null) return;
    if (!gameId) return;
    sendMove(gameId, index);
  };

  if (!gameData)
    return (
      <div className="min-h-screen bg-gray-900 flex justify-center">
        <h2 className="text-white mt-20">Ładowanie gry...</h2>
      </div>
    );

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-900 text-white font-sans">
      <h2 className="text-2xl mb-4 font-bold tracking-widest uppercase">
        Pokój: {gameData.status}
      </h2>
      <h3 className="text-xl mb-8 text-blue-400">
        {gameData.status === "WAITING_FOR_OPPONENT"
          ? "Czekaj na przeciwnika..."
          : isMyTurn
            ? "Twój ruch!"
            : "Przeciwnik myśli..."}
      </h3>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(3, 100px)",
          gap: "5px",
        }}
      >
        {gameData.board.map((cell, index) => (
          <div
            key={index}
            onClick={() => handleCellClick(index)}
            style={{
              width: "100px",
              height: "100px",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: "3em",
              cursor: isMyTurn && !cell ? "pointer" : "not-allowed",
            }}
            className="bg-gray-800 border border-gray-600 hover:bg-gray-700 transition"
          >
            {cell}
          </div>
        ))}
      </div>

      <button
        className="mt-12 px-6 py-3 font-semibold bg-gray-800 hover:bg-gray-700 border border-gray-600 rounded-sm"
        onClick={leaveGame}
        disabled={waitingForServerState}
      >
        Opuść grę
      </button>
    </div>
  );
};

export default TicTacToeGame;