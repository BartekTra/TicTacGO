import React from "react";
import { GameCell } from "./GameCell";
import { type GameData } from "./hooks/useGameWebSocket";

interface GameBoardProps {
  gameData: GameData;
  isMyTurn: boolean;
  onMove: (index: number) => void;
}

export const GameBoard: React.FC<GameBoardProps> = ({
  gameData,
  isMyTurn,
  onMove,
}) => {
  if (!gameData || !gameData.board) return <p className="text-gray-400">Trwa ładowanie planszy...</p>;

  const getExpiringPos = (movesStr: string): number | null => {
    if (gameData.mode !== "INFINITE") return null;
    if (!movesStr) return null;
    const split = movesStr.split(",").filter((s) => s.trim() !== "");
    if (split.length < 3) return null;
    // The oldest move is the first one in the list. Wait, in backend, new moves are appended.
    // So split[0] is the oldest move.
    return parseInt(split[0], 10);
  };

  const expiringX = getExpiringPos(gameData.movesX);
  const expiringO = getExpiringPos(gameData.movesO);

  const boardArray = gameData.board.split("");

  return (
    <div className="grid grid-cols-3 gap-2 border-2 border-gray-700 bg-gray-900 shadow-2xl p-4 rounded-lg w-[350px] h-[350px]">
      {boardArray.map((char, index) => {
        const isExpiring = (char === "X" && index === expiringX) || (char === "O" && index === expiringO);
        
        return (
          <GameCell 
            key={index} 
            index={index} 
            char={char === "-" ? "" : char} 
            isExpiring={isExpiring}
            isMyTurn={isMyTurn}
            onClick={onMove} 
          />
        );
      })}
    </div>
  );
};
