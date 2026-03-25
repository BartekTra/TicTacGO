import React from "react";

interface GameCellProps {
  char: string;
  index: number;
  onClick: (index: number) => void;
}

export const GameCell: React.FC<GameCellProps> = ({ char, index, onClick }) => {
  const displayChar = char === "O" || char === "X" ? char : "";
  
  return (
    <button
      onClick={() => onClick(index)}
      className="w-24 h-24 bg-gray-800 rounded text-4xl hover:bg-gray-700 transition-colors"
    >
      {displayChar}
    </button>
  );
};