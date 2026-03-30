import React from "react";
import { GameCell } from "./GameCell";

interface GameBoardProps {
  boardString: (string | null)[];
  onMove: (index: number) => void;
}

export const GameBoard: React.FC<GameBoardProps> = ({
  boardString,
  onMove,
}) => {

  if (!boardString) return <p> XD </p>;

  return (
    <div className="grid grid-cols-3 gap-2 border border-gray-600 p-2 rounded-sm  w-[320px] h-[320px]">
      {boardString?.map((char, index) => (
        <GameCell key={index} index={index} char={char} onClick={onMove} />
      ))}
    </div>
  );
};
