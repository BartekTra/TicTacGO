import React from "react";

interface GameInfoProps {
  countdown: number | null;
  currentTurn: string | null;
  opponentId?: string | null;
  winner: string | null;
  errorMessage?: string | null;
}

export const GameInfo: React.FC<GameInfoProps> = ({
  countdown,
  currentTurn,
  opponentId,
  winner,
  errorMessage,
}) => {
  return (
    <div className="flex flex-col items-center space-y-2 mb-4 text-center">
      {errorMessage && (
        <p className="text-yellow-300 font-bold text-sm max-w-[420px]">
          {errorMessage}
        </p>
      )}
      {countdown !== null && (
        <p className="text-red-500 font-bold text-xl">
          Przekierowanie za: {countdown}
        </p>
      )}
      <p className="text-lg">
        Aktualna tura: <span className="font-bold">{currentTurn}</span>
      </p>
      {opponentId && (
        <p className="text-gray-300">Przeciwnik: {opponentId}</p>
      )}
      {!opponentId && (
        <p> Poczekaj na przeciwnika </p>
      )}
      {winner && (
        <p className="text-green-400 font-bold text-2xl mt-2">
          WYGRANY: {winner}
        </p>
      )}
    </div>
  );
};
