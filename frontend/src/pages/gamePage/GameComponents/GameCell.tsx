import React from "react";

interface GameCellProps {
  char: string;
  index: number;
  isExpiring: boolean;
  isMyTurn: boolean;
  onClick: (index: number) => void;
}

export const GameCell: React.FC<GameCellProps> = ({ char, index, isExpiring, isMyTurn, onClick }) => {
  const displayChar = char === "O" || char === "X" ? char : "";
  const baseBg = "bg-gray-800";
  const hoverBg = isMyTurn && !displayChar ? "hover:bg-gray-700 cursor-pointer" : "cursor-not-allowed";
  
  // Mikro-animacja dla znikających elementów w trybie Infinite
  const expiringClass = isExpiring ? "opacity-30 animate-pulse border-red-500 border-2" : "border-gray-600 border";
  
  return (
    <button
      onClick={() => onClick(index)}
      className={`w-24 h-24 ${baseBg} rounded text-4xl font-bold transition-all duration-300 ${hoverBg} ${expiringClass}`}
      disabled={!isMyTurn || displayChar !== ""}
    >
      <span className={char === "X" ? "text-blue-400" : "text-green-400"}>
        {displayChar}
      </span>
    </button>
  );
};