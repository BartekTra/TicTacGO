import React from "react";
import { type User } from "../../../types/User";

interface GameInfoProps {
  countdown: number | null;
  currentTurn: User | null;
  opponentId?: User | null;
  winner: User | null;
}

export const GameInfo: React.FC<GameInfoProps> = ({
  countdown,
  currentTurn,
  opponentId,
  winner,
}) => {
  return (
    <div className="flex flex-col items-center space-y-2 mb-4 text-center">
      {countdown !== null && (
        <p className="text-red-500 font-bold text-xl">
          Przekierowanie za: {countdown}
        </p>
      )}
      <p className="text-lg">
        Aktualna tura: <span className="font-bold">{currentTurn?.nickname}</span>
      </p>
      {opponentId && (
        <p className="text-gray-300">Przeciwnik: {opponentId.email}</p>
      )}
      {!opponentId && (
        <p> Poczekaj na przeciwnika </p>
      )}
      {winner && (
        <p className="text-green-400 font-bold text-2xl mt-2">
          WYGRANY: {winner.email}
        </p>
      )}
    </div>
  );
};
